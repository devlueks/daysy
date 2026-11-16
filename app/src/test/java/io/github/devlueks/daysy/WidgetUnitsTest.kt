package io.github.devlueks.daysy

import io.github.devlueks.daysy.data.CalendarUnit.DAYS
import io.github.devlueks.daysy.data.CalendarUnit.HOURS
import io.github.devlueks.daysy.data.CalendarUnit.MONTHS
import io.github.devlueks.daysy.data.breakdown
import io.github.devlueks.daysy.widget.widgetUnits
import io.github.devlueks.daysy.widget.fitUnits
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class WidgetUnitsTest {
    private fun at(s: String) = LocalDateTime.parse(s).atZone(ZoneId.of("Europe/Berlin"))

    @Test
    fun noMinutesOrSecondsOnWidgets() {
        // 1 Monat, 0 Wochen, 2 Tage, 9 Std. → "1 Monat · 2 Tage"
        val long = breakdown(at("2026-11-13T14:30:00"), at("2026-12-16T00:00:00"))
        assertEquals(listOf(MONTHS, DAYS), widgetUnits(long).map { it.unit })
        // Last week: days and hours
        val week = breakdown(at("2026-11-13T02:00:00"), at("2026-11-19T00:00:00"))
        assertEquals(listOf(DAYS, HOURS), widgetUnits(week).map { it.unit })
        // Last day: just the hours
        val short = breakdown(at("2026-11-13T08:00:00"), at("2026-11-13T17:45:00"))
        assertEquals(listOf(HOURS), widgetUnits(short).map { it.unit })
        // Under an hour: nothing (the widget says "Gleich so weit")
        assertEquals(emptyList<Any>(), widgetUnits(breakdown(at("2026-11-13T08:00:00"), at("2026-11-13T08:40:00"))))
    }

    @Test
    fun narrowWidgetsDropTheSmallerUnit() {
        val label = { u: io.github.devlueks.daysy.data.UnitValue -> if (u.unit == MONTHS) "Monat" else "Tage" }
        val units = widgetUnits(breakdown(at("2026-11-13T14:30:00"), at("2026-12-16T00:00:00"))) // 1 Monat · 2 Tage
        assertEquals(2, fitUnits(units, availableDp = 300f, labelOf = label).size)
        assertEquals(listOf(MONTHS), fitUnits(units, availableDp = 120f, labelOf = label).map { it.unit })
        assertEquals(1, fitUnits(units, availableDp = 10f, labelOf = label).size) // never empty
    }
}
