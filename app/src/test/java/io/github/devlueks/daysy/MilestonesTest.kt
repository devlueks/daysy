package io.github.devlueks.daysy

import io.github.devlueks.daysy.data.milestones
import io.github.devlueks.daysy.data.nextMilestone
import io.github.devlueks.daysy.data.milestoneProgress
import io.github.devlueks.daysy.data.recentMilestone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class MilestonesTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private fun at(s: String) = LocalDateTime.parse(s).atZone(zone)

    @Test
    fun orderIsChronological() {
        val labels = milestones(at("2026-01-01T00:00:00"), years = 4).map { it.key }
        assertEquals(
            listOf("1d", "1w", "1m", "100d", "6m", "1y", "500d", "2y", "1000d", "3y", "4y"),
            labels,
        )
    }

    @Test
    fun nextAndRecent() {
        val start = at("2026-11-13T08:00:00")
        assertEquals("1d", nextMilestone(start, at("2026-11-13T09:00:00"))?.key)
        assertEquals("1w", nextMilestone(start, at("2026-11-14T08:00:00"))?.key) // exactly reached → next one
        assertEquals("1d", recentMilestone(start, at("2026-11-14T10:00:00"))?.key)
        assertNull(recentMilestone(start, at("2026-11-16T10:00:00"))) // older than a day
        assertNull(nextMilestone(start, at("2026-11-12T10:00:00"))) // not started yet
        assertEquals(at("2026-12-13T08:00:00"), nextMilestone(start, at("2026-11-25T00:00:00"))?.at) // calendar month
    }

    @Test
    fun progressBetweenMilestones() {
        val start = at("2026-11-13T00:00:00")
        assertEquals(0.5f, milestoneProgress(start, at("2026-11-13T12:00:00")), 0.001f) // halfway to 1 Tag
        assertEquals(0.5f, milestoneProgress(start, at("2026-11-17T00:00:00")), 0.001f) // 1 Tag → 1 Woche: 3 of 6 days
    }
}
