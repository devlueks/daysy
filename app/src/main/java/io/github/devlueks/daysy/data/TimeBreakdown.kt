package io.github.devlueks.daysy.data

import androidx.annotation.PluralsRes
import io.github.devlueks.daysy.R
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** A time unit with its (translated) labels: after a number, "… ago" and "in …". */
enum class CalendarUnit(@PluralsRes val labelRes: Int, @PluralsRes val agoRes: Int, @PluralsRes val inRes: Int) {
    YEARS(R.plurals.unit_years, R.plurals.ago_years, R.plurals.in_years),
    MONTHS(R.plurals.unit_months, R.plurals.ago_months, R.plurals.in_months),
    WEEKS(R.plurals.unit_weeks, R.plurals.ago_weeks, R.plurals.in_weeks),
    DAYS(R.plurals.unit_days, R.plurals.ago_days, R.plurals.in_days),
    HOURS(R.plurals.unit_hours, R.plurals.ago_hours, R.plurals.in_hours),
    MINUTES(R.plurals.unit_minutes, R.plurals.ago_minutes, R.plurals.in_minutes),
    SECONDS(R.plurals.unit_seconds, R.plurals.ago_minutes, R.plurals.in_minutes),
}

data class UnitValue(val unit: CalendarUnit, val value: Long)

/**
 * Splits the span between [a] and [b] (order doesn't matter) into calendar-correct
 * years, months, weeks, days, hours, minutes and seconds. Years, months and days follow
 * the calendar (so "1 Monat" from Jan 31 lands on Feb 28); the time part uses real
 * elapsed seconds, so daylight-saving switches are handled correctly.
 */
fun breakdown(a: ZonedDateTime, b: ZonedDateTime): List<UnitValue> {
    val (from, to) = if (b.isBefore(a)) b to a else a to b
    var cursor = from
    val years = ChronoUnit.YEARS.between(cursor, to)
    cursor = cursor.plusYears(years)
    val months = ChronoUnit.MONTHS.between(cursor, to)
    cursor = cursor.plusMonths(months)
    val totalDays = ChronoUnit.DAYS.between(cursor, to)
    cursor = cursor.plusDays(totalDays)
    val seconds = ChronoUnit.SECONDS.between(cursor, to).coerceAtLeast(0)

    return listOf(
        UnitValue(CalendarUnit.YEARS, years),
        UnitValue(CalendarUnit.MONTHS, months),
        UnitValue(CalendarUnit.WEEKS, totalDays / 7),
        UnitValue(CalendarUnit.DAYS, totalDays % 7),
        UnitValue(CalendarUnit.HOURS, seconds / 3600),
        UnitValue(CalendarUnit.MINUTES, (seconds % 3600) / 60),
        UnitValue(CalendarUnit.SECONDS, seconds % 60),
    )
}

/**
 * The [count] largest non-zero units, e.g. "1 Monat · 3 Tage · 9 Std." – a "0 Wochen" in
 * between is skipped and the next smaller unit takes its place. Seconds only appear for spans
 * under a day, so "5 Tage 23 Std. 0 Min." doesn't turn into a ticking "5 Tage 23 Std. 32 Sek.".
 * When everything is zero, the smallest unit is shown ("0 Sek.").
 */
fun topUnits(parts: List<UnitValue>, count: Int = 3): List<UnitValue> {
    val nonZero = parts.filter { it.value != 0L }
    val underADay = nonZero.firstOrNull()?.let { it.unit >= CalendarUnit.HOURS } ?: true
    return nonZero
        .filter { underADay || it.unit != CalendarUnit.SECONDS }
        .take(count)
        .ifEmpty { listOf(parts.last()) }
}
