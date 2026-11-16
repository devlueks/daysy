package io.github.devlueks.daysy.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.devlueks.daysy.data.CountdownRepository
import io.github.devlueks.daysy.data.SettingsRepository
import io.github.devlueks.daysy.widget.CountdownWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires at a countdown's end, an early warning or a milestone and posts the notification. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        CountdownNotifications.show(context, intent)
        // Something just ended or was reached: refresh the widgets right away (instead of up to
        // 30 minutes later) and, after a milestone, schedule the next one.
        resyncInBackground(context)
    }
}

/** System broadcasts the reschedule receiver reacts to – anything else is ignored. */
private val RESCHEDULE_ACTIONS = setOf(
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_MY_PACKAGE_REPLACED,
    Intent.ACTION_TIME_CHANGED,
    Intent.ACTION_TIMEZONE_CHANGED,
    android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
)

/**
 * Alarms don't survive a reboot and are tied to wall-clock time, so they are rebuilt
 * after boot, app updates, time/time-zone changes and when exact alarms get allowed.
 */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in RESCHEDULE_ACTIONS) resyncInBackground(context)
    }
}

/** Reads the saved data and brings alarms and widgets up to date, outside the app. */
private fun BroadcastReceiver.resyncInBackground(context: Context) {
    val pending = goAsync()
    val appContext = context.applicationContext
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val repository = CountdownRepository(appContext)
            repository.load()
            val settings = SettingsRepository(appContext).settings.value
            CountdownNotifications.sync(appContext, repository.countdowns.value, settings.notificationsEnabled)
            CountdownWidget.refreshAll(appContext)
        } finally {
            pending.finish()
        }
    }
}
