package io.github.devlueks.daysy

import io.github.devlueks.daysy.data.CalendarUnit.DAYS
import io.github.devlueks.daysy.data.CalendarUnit.HOURS
import io.github.devlueks.daysy.data.CalendarUnit.MINUTES
import io.github.devlueks.daysy.data.CalendarUnit.MONTHS
import io.github.devlueks.daysy.data.CalendarUnit.SECONDS
import io.github.devlueks.daysy.data.CalendarUnit.WEEKS
import io.github.devlueks.daysy.data.CalendarUnit.YEARS
import io.github.devlueks.daysy.data.UnitValue
import io.github.devlueks.daysy.data.breakdown
import io.github.devlueks.daysy.data.topUnits
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TimeBreakdownTest {
    private val utc = ZoneId.of("UTC")
    private fun at(s: String) = LocalDateTime.parse(s).atZone(utc)
    private fun values(p: List<UnitValue>) = p.map { it.value }

    @Test
    fun fullBreakdown() {
        val p = breakdown(at("2026-01-31T00:00:00"), at("2027-03-15T10:20:30"))
        // +1y -> 2027-01-31, +1m -> 2027-02-28, +15d (2w 1d) -> 2027-03-15
        assertEquals(listOf(1L, 1, 2, 1, 10, 20, 30), values(p))
    }

    @Test
    fun orderDoesNotMatter() {
        val a = at("2026-11-12T11:00:00")
        val b = at("2026-12-24T18:00:00")
        assertEquals(breakdown(a, b), breakdown(b, a))
    }

    @Test
    fun topThreeStartsAtLargestNonZeroUnit() {
        val p = breakdown(at("2026-11-12T11:00:00"), at("2026-12-24T18:30:00"))
        // 1 Monat (12.12.), 12 Tage = 1 Woche 5 Tage, 7:30
        assertEquals(listOf(MONTHS, WEEKS, DAYS), topUnits(p).map { it.unit })
        assertEquals(listOf(1L, 1, 5), topUnits(p).map { it.value })
    }

    @Test
    fun zeroUnitsAreSkippedAndFilledWithSmallerOnes() {
        // 1 Jahr, 0 Monate, 0 Wochen, 3 Tage, 0 Std., 0 Min. → "1 Jahr 3 Tage"
        val p = breakdown(at("2026-01-01T00:00:00"), at("2027-01-04T00:00:00"))
        assertEquals(listOf(YEARS, DAYS), topUnits(p).map { it.unit })
        // 1 Monat, 0 Wochen, 2 Tage, 9 Std., 30 Min. → "1 Monat 2 Tage 9 Std."
        val q = breakdown(at("2026-11-13T14:30:00"), at("2026-12-16T00:00:00"))
        assertEquals(listOf(MONTHS, DAYS, HOURS), topUnits(q).map { it.unit })
        assertEquals(listOf(1L, 2, 9), topUnits(q).map { it.value })
        // 5 Tage, 23 Std., 0 Min., 32 Sek. → seconds don't fill in for long spans
        val r = breakdown(at("2026-11-13T00:00:00"), at("2026-11-18T23:00:32"))
        assertEquals(listOf(DAYS, HOURS), topUnits(r).map { it.unit })
    }

    @Test
    fun shortSpansShowTimeUnits() {
        val p = breakdown(at("2026-01-01T00:00:00"), at("2026-01-01T00:00:42"))
        assertEquals(listOf(SECONDS), topUnits(p).map { it.unit })
        val zero = topUnits(breakdown(at("2026-01-01T00:00:00"), at("2026-01-01T00:00:00")))
        assertEquals(listOf(SECONDS), zero.map { it.unit })
        assertEquals(listOf(0L), zero.map { it.value })
    }


    @Test
    fun fewerUnits() {
        val p = breakdown(at("2026-11-12T11:00:00"), at("2026-12-24T18:30:00"))
        assertEquals(listOf(MONTHS), topUnits(p, 1).map { it.unit })
        assertEquals(listOf(SECONDS), topUnits(breakdown(at("2026-01-01T00:00:00"), at("2026-01-01T00:00:05")), 1).map { it.unit })
    }

    @Test
    fun daylightSavingUsesRealHours() {
        val berlin = ZoneId.of("Europe/Berlin")
        // Clocks jump forward on 2026-03-29 at 02:00: midnight to noon is only 11 real hours.
        val p = breakdown(
            LocalDateTime.parse("2026-03-29T00:00:00").atZone(berlin),
            LocalDateTime.parse("2026-03-29T12:00:00").atZone(berlin),
        )
        assertEquals(listOf(0L, 0, 0, 0, 11, 0, 0), values(p))
    }
}
