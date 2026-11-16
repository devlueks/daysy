package io.github.devlueks.daysy.ui

import android.content.res.Resources
import io.github.devlueks.daysy.R
import io.github.devlueks.daysy.data.CalendarUnit
import io.github.devlueks.daysy.data.Milestone
import io.github.devlueks.daysy.data.UnitValue
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/*
 * Translated texts that depend on numbers, and dates in the current language.
 * Everything takes [Resources] so it works the same in the app, the widget and notifications.
 * Dates use Locale.getDefault() at call time, so a language switch applies right away.
 */

private fun Long.asQuantity(): Int = coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

/** The word after a number on cards and widgets: "Tage" / "days", "Std." / "hrs". */
fun Resources.unitLabel(u: UnitValue): String = getQuantityString(u.unit.labelRes, u.value.asQuantity())

private fun largestUnit(parts: List<UnitValue>): UnitValue? =
    parts.firstOrNull { it.value > 0 && it.unit != CalendarUnit.SECONDS }

/** "vor 3 Tagen" / "3 days ago", based on the largest non-zero unit; "gerade eben" / "just now" below a minute. */
fun Resources.agoText(parts: List<UnitValue>): String =
    largestUnit(parts)?.let { getQuantityString(it.unit.agoRes, it.value.asQuantity(), it.value.asQuantity()) }
        ?: getString(R.string.just_now)

/** "in 3 Tagen" / "in 3 days"; "gleich" / "any moment" below a minute. */
fun Resources.inText(parts: List<UnitValue>): String =
    largestUnit(parts)?.let { getQuantityString(it.unit.inRes, it.value.asQuantity(), it.value.asQuantity()) }
        ?: getString(R.string.any_moment)

/** "100 Tage" / "100 days". */
fun Resources.milestoneLabel(m: Milestone): String = getQuantityString(m.unit.labelRes, m.amount, m.amount)

/** "Freitag, 20. November 2026" / "Friday, November 20, 2026". */
fun fullDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault()))

/** "18:30" / "6:30 PM". */
fun shortTime(time: LocalTime): String =
    time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()))

/**
 * A short date in the order and style of the current language, e.g. "Do., 19. Nov." / "Thu, Nov 19".
 * The year is only added when it isn't the current one.
 */
fun shortDate(date: LocalDate, weekday: Boolean = true): String {
    val withYear = date.year != LocalDate.now().year
    val skeleton = (if (weekday) "EEE" else "") + "MMMd" + (if (withYear) "y" else "")
    val locale = Locale.getDefault()
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
    return date.format(DateTimeFormatter.ofPattern(pattern, locale))
}

/** A reminder time in the largest whole unit: "1 Woche", "2 Tage", "3 Stunden", "30 Minuten". */
fun Resources.reminderLabel(minutes: Long): String {
    val (res, n) = when {
        minutes % (7 * 24 * 60) == 0L -> R.plurals.reminder_weeks to minutes / (7 * 24 * 60)
        minutes % (24 * 60) == 0L -> R.plurals.reminder_days to minutes / (24 * 60)
        minutes % 60 == 0L -> R.plurals.reminder_hours to minutes / 60
        else -> R.plurals.reminder_minutes to minutes
    }
    val count = n.toInt()
    return getQuantityString(res, count, count)
}
