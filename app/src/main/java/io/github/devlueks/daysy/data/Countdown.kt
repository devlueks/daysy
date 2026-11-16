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

/** Optional early warning before a countdown ends. Order/names are persisted. */
enum class Reminder(@StringRes val labelRes: Int, val before: Duration?) {
    NONE(R.string.reminder_none, null),
    HOUR(R.string.reminder_hour, Duration.ofHours(1)),
    DAY(R.string.reminder_day, Duration.ofDays(1)),
    WEEK(R.string.reminder_week, Duration.ofDays(7)),
}

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
    /** Countdown only: additional notification some time before the end. */
    val reminder: Reminder = Reminder.NONE,
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
