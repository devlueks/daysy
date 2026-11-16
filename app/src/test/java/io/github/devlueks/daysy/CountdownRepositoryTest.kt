package io.github.devlueks.daysy

import io.github.devlueks.daysy.data.Countdown
import io.github.devlueks.daysy.data.CountdownRepository
import io.github.devlueks.daysy.data.Kind
import io.github.devlueks.daysy.data.Reminder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

class CountdownRepositoryTest {

    @get:Rule
    val dir = TemporaryFolder()

    private fun file() = File(dir.root, "countdowns.json")
    private fun backups() = dir.root.listFiles().orEmpty().filter { it.name.startsWith("countdowns-unreadable-") }

    @Test
    fun savesAndLoadsEverything() = runBlocking {
        val original = Countdown(
            id = 0, name = "Silvester 🍾", date = LocalDate.of(2027, 1, 1), time = LocalTime.of(18, 30),
            emoji = "🍾", kind = Kind.COUNTDOWN, autoCountUp = true, pinned = true, shape = 4,
            notify = false, reminder = Reminder.DAY, color = "lila", milestones = true,
        )
        CountdownRepository(dir.root).apply { load(); upsert(original) }

        val reloaded = CountdownRepository(dir.root).apply { load() }.countdowns.value
        assertEquals(listOf(original.copy(id = 1)), reloaded)
    }

    @Test
    fun readsOldFilesWithoutNewerFields() = runBlocking {
        file().writeText("""[{"id":7,"name":"Alt","date":"2026-12-24"}]""")
        val c = CountdownRepository(dir.root).apply { load() }.countdowns.value.single()
        assertEquals("Alt", c.name)
        assertEquals(Kind.COUNTDOWN, c.kind)
        assertTrue(c.notify)
        assertEquals(Reminder.NONE, c.reminder)
        assertEquals(-1, c.shape)
        assertEquals(null, c.color)
        assertFalse(c.milestones)
    }

    @Test
    fun brokenFileIsKeptAndNotOverwrittenSilently() = runBlocking {
        file().writeText("this is not json")
        val repo = CountdownRepository(dir.root).apply { load() }
        assertTrue(repo.loadProblem.value)
        assertEquals(1, backups().size)
        assertEquals("this is not json", backups().single().readText())

        // Saving afterwards works and the backup stays.
        repo.upsert(Countdown(id = 0, name = "Neu", date = LocalDate.of(2027, 1, 1)))
        assertEquals("this is not json", backups().single().readText())
        assertEquals(1, CountdownRepository(dir.root).apply { load() }.countdowns.value.size)
    }

    @Test
    fun repeatedLoadsKeepOnlyOneCopyOfABrokenFile() = runBlocking {
        file().writeText("this is not json")
        // e.g. the widget refreshing several times while the file is still broken
        repeat(3) { CountdownRepository(dir.root).load() }
        assertEquals(1, backups().size)
    }

    @Test
    fun oneBrokenEntryDoesNotCostTheOthers() = runBlocking {
        file().writeText("""[{"id":1,"name":"Gut","date":"2026-12-24"},{"id":2,"name":"Kaputt","date":"kein Datum"}]""")
        val repo = CountdownRepository(dir.root).apply { load() }
        assertEquals(listOf("Gut"), repo.countdowns.value.map { it.name })
        assertTrue(repo.loadProblem.value)
        assertEquals(1, backups().size)
    }

    @Test
    fun saveBeforeLoadKeepsExistingEntries() = runBlocking {
        file().writeText("""[{"id":1,"name":"Bestehend","date":"2026-12-24"}]""")
        // upsert without calling load() first – e.g. a very fast tap during app start
        val repo = CountdownRepository(dir.root)
        repo.upsert(Countdown(id = 0, name = "Neu", date = LocalDate.of(2027, 1, 1)))
        assertEquals(listOf("Bestehend", "Neu"), repo.countdowns.value.map { it.name })
        assertFalse(repo.loadProblem.value)
    }

    @Test
    fun missingFileIsNotAProblem() = runBlocking {
        val repo = CountdownRepository(dir.root).apply { load() }
        assertTrue(repo.countdowns.value.isEmpty())
        assertFalse(repo.loadProblem.value)
        assertTrue(backups().isEmpty())
    }
}
