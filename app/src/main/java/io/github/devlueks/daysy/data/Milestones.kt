package io.github.devlueks.daysy.data

import androidx.annotation.PluralsRes
import io.github.devlueks.daysy.R
import java.time.ZonedDateTime

enum class MilestoneUnit(@PluralsRes val labelRes: Int, val code: Char) {
    DAYS(R.plurals.milestone_days, 'd'),
    WEEKS(R.plurals.milestone_weeks, 'w'),
    MONTHS(R.plurals.milestone_months, 'm'),
    YEARS(R.plurals.milestone_years, 'y'),
}

/** A round anniversary of a "Zeit seit" entry, e.g. "1 Woche" or "100 Tage" (text via resources). */
data class Milestone(val amount: Int, val unit: MilestoneUnit, val at: ZonedDateTime) {
    /** Language-independent id, e.g. "100d" or "2y". */
    val key: String get() = "$amount${unit.code}"
}

/**
 * All milestones after [start], in order: a few early ones (1 Tag, 1 Woche, 1 Monat, …),
 * the classic round day counts (100, 500, 1000 Tage) and then every full year.
 * Months and years follow the calendar.
 */
fun milestones(start: ZonedDateTime, years: Int = 100): List<Milestone> {
    val list = mutableListOf(
        Milestone(1, MilestoneUnit.DAYS, start.plusDays(1)),
        Milestone(1, MilestoneUnit.WEEKS, start.plusWeeks(1)),
        Milestone(1, MilestoneUnit.MONTHS, start.plusMonths(1)),
        Milestone(100, MilestoneUnit.DAYS, start.plusDays(100)),
        Milestone(6, MilestoneUnit.MONTHS, start.plusMonths(6)),
        Milestone(1, MilestoneUnit.YEARS, start.plusYears(1)),
        Milestone(500, MilestoneUnit.DAYS, start.plusDays(500)),
        Milestone(1000, MilestoneUnit.DAYS, start.plusDays(1000)),
    )
    for (y in 2..years) list += Milestone(y, MilestoneUnit.YEARS, start.plusYears(y.toLong()))
    return list.sortedBy { it.at }
}

/** The next milestone still to come, or null if [now] is before the start. */
fun nextMilestone(start: ZonedDateTime, now: ZonedDateTime): Milestone? =
    if (now.isBefore(start)) null else milestones(start).firstOrNull { it.at.isAfter(now) }

/** How far along the way from the last milestone (or the start) to the next one, 0..1. */
fun milestoneProgress(start: ZonedDateTime, now: ZonedDateTime): Float {
    val next = nextMilestone(start, now) ?: return 0f
    val from = milestones(start).lastOrNull { !it.at.isAfter(now) }?.at ?: start
    val total = java.time.Duration.between(from, next.at).toMillis().toFloat()
    val done = java.time.Duration.between(from, now).toMillis().toFloat()
    return if (total <= 0f) 0f else (done / total).coerceIn(0f, 1f)
}

/** The milestone reached most recently, if it was within [withinHours] hours. */
fun recentMilestone(start: ZonedDateTime, now: ZonedDateTime, withinHours: Long = 24): Milestone? =
    milestones(start).lastOrNull { !it.at.isAfter(now) }?.takeIf { it.at.isAfter(now.minusHours(withinHours)) }
