package io.github.devlueks.daysy.data

import androidx.annotation.StringRes
import io.github.devlueks.daysy.R
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

enum class Kind { COUNTDOWN, COUNTUP }

/** Default reminder for new countdowns (settings). Order/names are persisted. */
enum class Reminder(@StringRes val labelRes: Int, val before: Duration?) {
    NONE(R.string.reminder_none, null),
    HOUR(R.string.reminder_hour, Duration.ofHours(1)),
    DAY(R.string.reminder_day, Duration.ofDays(1)),
    WEEK(R.string.reminder_week, Duration.ofDays(7)),
}

/** Quick choices for reminders in the editor, in minutes before the end: 15 min, 1 h, 1 day, 1 week. */
val REMINDER_PRESETS: List<Long> = listOf(15L, 60L, 24 * 60L, 7 * 24 * 60L)

/** At most this many reminders per countdown (keeps alarms and the editor manageable). */
const val MAX_REMINDERS = 5

/** Cleans a reminder list: positive, unique, at most [MAX_REMINDERS], longest first. */
fun normalizeReminders(minutes: List<Long>): List<Long> =
    minutes.filter { it > 0 }.distinct().sortedDescending().take(MAX_REMINDERS)


data class Countdown(
    val id: Long,
    val name: String,
    /** Target date for a countdown, start date for a count-up. */
    val date: LocalDate,
    /** Optional exact time. If null, the event is at the start of [date]. */
    val time: LocalTime? = null,
    val emoji: String? = null,
    val kind: Kind = Kind.COUNTDOWN,
    /** Countdown only: once expired, keep counting up (shown in the count-up tab). */
    val autoCountUp: Boolean = false,
    /** Pinned entries are shown at the top of their list. */
    val pinned: Boolean = false,
    /** Randomly chosen background shape for the emoji (index into the UI's shape list, -1 = derive from id). */
    val shape: Int = -1,
    /** Countdown only: post a notification when it ends. */
    val notify: Boolean = true,
    /** Countdown only: extra notifications this many minutes before the end (e.g. 60 = 1 hour). */
    val reminders: List<Long> = emptyList(),
    /** Own card color (key of a preset, e.g. "blau"); null = system colors. */
    val color: String? = null,
    /** "Zeit seit" only: show milestones (1 Tag, 1 Woche, …) – off until the user switches it on. */
    val milestones: Boolean = false,
) {
    fun target(zone: ZoneId = ZoneId.systemDefault()): ZonedDateTime =
        LocalDateTime.of(date, time ?: LocalTime.MIDNIGHT).atZone(zone)

    fun targetMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
        target(zone).toInstant().toEpochMilli()
}
