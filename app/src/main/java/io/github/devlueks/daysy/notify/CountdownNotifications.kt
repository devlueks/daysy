package io.github.devlueks.daysy.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.devlueks.daysy.MainActivity
import io.github.devlueks.daysy.R
import io.github.devlueks.daysy.data.Countdown
import io.github.devlueks.daysy.data.Kind
import io.github.devlueks.daysy.data.nextMilestone
import io.github.devlueks.daysy.ui.milestoneLabel
import java.time.ZonedDateTime

/**
 * Schedules one alarm per countdown end (and one per early warning) with AlarmManager.
 * [sync] is idempotent: it cancels alarms that are no longer wanted and (re)schedules the rest.
 */
object CountdownNotifications {

    private const val CHANNEL_ID = "countdown_events"
    private const val PREFS = "alarms"
    private const val KEY_CODES = "codes"
    private const val KEY_FIRED = "fired"
    private const val ACTION_PRIMARY = "io.github.devlueks.daysy.ALARM"
    private const val ACTION_BACKUP = "io.github.devlueks.daysy.ALARM_BACKUP"
    private const val EXTRA_AT = "at"
    private const val INEXACT_WINDOW_MS = 10 * 60 * 1000L

    const val EXTRA_ID = "countdown_id"
    private const val EXTRA_NAME = "name"
    private const val EXTRA_EMOJI = "emoji"
    private const val EXTRA_REMINDER = "reminder_label"
    private const val EXTRA_MILESTONE = "milestone_label"
    private const val CHANNEL_MILESTONES = "milestones"


    fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun sync(context: Context, countdowns: List<Countdown>, enabled: Boolean) {
        // Create the channels early, so they show up in the system settings before the first alert.
        ensureChannels(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getStringSet(KEY_CODES, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
        val now = System.currentTimeMillis()

        val wanted = mutableMapOf<Int, Pair<Long, Intent>>()
        if (enabled) {
            for (c in countdowns) {
                if (!c.notify) continue
                if (c.kind == Kind.COUNTUP) {
                    if (!c.milestones) continue
                    // Only the next milestone; when it fires, the one after it gets scheduled.
                    nextMilestone(c.target(), ZonedDateTime.now())?.let { m ->
                        wanted[requestCode(c.id, reminder = false)] =
                            m.at.toInstant().toEpochMilli() to alarmIntent(context, c, null).putExtra(EXTRA_MILESTONE, context.resources.milestoneLabel(m))
                    }
                    continue
                }
                val end = c.targetMillis()
                if (end > now) wanted[requestCode(c.id, reminder = false)] = end to alarmIntent(context, c, null)
                c.reminder.before?.let { before ->
                    val at = end - before.toMillis()
                    if (at > now) wanted[requestCode(c.id, reminder = true)] = at to alarmIntent(context, c, context.getString(c.reminder.labelRes))
                }
            }
        }

        (previous - wanted.keys).forEach { code ->
            // null = alarms from app versions before the primary/backup split
            listOf(null, ACTION_PRIMARY, ACTION_BACKUP).forEach { action -> cancel(context, alarmManager, code, action) }
        }

        val exact = canScheduleExact(context)
        wanted.forEach { (code, value) ->
            val (time, intent) = value
            intent.putExtra(EXTRA_AT, time)
            val primary = pendingFor(context, code, Intent(intent).setAction(ACTION_PRIMARY))
            if (exact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, primary)
                cancel(context, alarmManager, code, ACTION_BACKUP)
            } else {
                // Without "Wecker & Erinnerungen" Android may move an inexact alarm by up to an hour.
                // So: one alarm with a 10-minute window (on time while the phone is in use) …
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, time, INEXACT_WINDOW_MS, primary)
                // … plus the doze-proof one as a backup. Whichever fires first shows the notification.
                val backup = pendingFor(context, code, Intent(intent).setAction(ACTION_BACKUP))
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, backup)
            }
            cancel(context, alarmManager, code, null)
        }
        prefs.edit().putStringSet(KEY_CODES, wanted.keys.map { it.toString() }.toSet()).apply()
    }

    /** Called by [AlarmReceiver] when an alarm fires. */
    fun show(context: Context, intent: Intent) {
        if (!canPostNotifications(context)) return
        val id = intent.getLongExtra(EXTRA_ID, -1)
        if (!markFired(context, requestCode(id, intent.getStringExtra(EXTRA_REMINDER) != null), intent.getLongExtra(EXTRA_AT, 0))) {
            return // the other alarm of the pair already showed it
        }
        ensureChannels(context)
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        val emoji = intent.getStringExtra(EXTRA_EMOJI)
        val reminder = intent.getStringExtra(EXTRA_REMINDER)
        val milestone = intent.getStringExtra(EXTRA_MILESTONE)

        val open = PendingIntent.getActivity(
            context,
            requestCode(id, reminder != null),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, if (milestone != null) CHANNEL_MILESTONES else CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(listOfNotNull(emoji, name).joinToString(" "))
            .setContentText(
                when {
                    milestone != null -> "Meilenstein erreicht: $milestone 🎉"
                    reminder != null -> context.getString(R.string.notification_reminder, reminder)
                    else -> context.getString(R.string.notification_now)
                }
            )
            .setCategory(if (reminder != null) NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission") // checked above
        NotificationManagerCompat.from(context).notify(requestCode(id, reminder != null), notification)
    }

    /**
     * Two channels, so milestones can be muted in the system settings without losing the
     * countdown alerts (and the other way round). Creating an existing channel only updates its texts.
     */
    private fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.channel_countdowns), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = context.getString(R.string.channel_countdowns_description)
                },
                NotificationChannel(CHANNEL_MILESTONES, context.getString(R.string.channel_milestones), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Wenn ein „Zeit seit“-Eintrag einen Meilenstein erreicht"
                },
            )
        )
    }

    private fun alarmIntent(context: Context, c: Countdown, reminderLabel: String?) =
        Intent(context, AlarmReceiver::class.java)
            .putExtra(EXTRA_ID, c.id)
            .putExtra(EXTRA_NAME, c.name)
            .putExtra(EXTRA_EMOJI, c.emoji)
            .putExtra(EXTRA_REMINDER, reminderLabel)

    private fun requestCode(id: Long, reminder: Boolean): Int = (id * 2 + if (reminder) 1 else 0).toInt()

    private fun pendingFor(context: Context, code: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun cancel(context: Context, alarmManager: AlarmManager, code: Int, action: String?) {
        PendingIntent.getBroadcast(
            context, code, Intent(context, AlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    /**
     * Remembers which (alarm, time) pairs already produced a notification.
     * Returns false if this one was shown before. Entries older than a week are dropped.
     */
    @Synchronized
    private fun markFired(context: Context, code: Int, at: Long): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "$code@$at"
        val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val fired = prefs.getStringSet(KEY_FIRED, emptySet()).orEmpty()
            .filter { (it.substringAfter('@').toLongOrNull() ?: 0L) > weekAgo }
            .toMutableSet()
        if (!fired.add(key)) return false
        prefs.edit().putStringSet(KEY_FIRED, fired).commit()
        return true
    }
}
