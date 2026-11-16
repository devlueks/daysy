package io.github.devlueks.daysy.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

/**
 * Stores countdowns as a small JSON file.
 *
 * Safety rules:
 *  - Nothing is ever written before the file has been read, so a quick tap during app start
 *    can't overwrite existing data with an almost empty list.
 *  - If the file (or a single entry) can't be read, the original file is kept as a
 *    "countdowns-unreadable-<time>.json" copy before anything new is written, and
 *    [loadProblem] tells the UI about it.
 *  - Writes go to a temporary file first and then replace the real one.
 */
class CountdownRepository(private val directory: File) {

    constructor(context: Context) : this(context.filesDir)

    private val file = File(directory, FILE_NAME)
    private val mutex = Mutex()
    private var loaded = false

    private val _countdowns = MutableStateFlow<List<Countdown>>(emptyList())
    val countdowns: StateFlow<List<Countdown>> = _countdowns.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    /** False until the stored entries have been read – the UI shows no "empty" hint before that. */
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val _loadProblem = MutableStateFlow(false)
    /** True if stored data could not be read completely (a backup copy was kept). */
    val loadProblem: StateFlow<Boolean> = _loadProblem.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock { loadLocked() }
    }

    suspend fun upsert(countdown: Countdown) = mutate { list ->
        val id = if (countdown.id == 0L) (list.maxOfOrNull { it.id } ?: 0L) + 1 else countdown.id
        val item = countdown.copy(id = id)
        if (list.any { it.id == id }) list.map { if (it.id == id) item else it } else list + item
    }

    suspend fun delete(id: Long) = mutate { list -> list.filterNot { it.id == id } }

    private fun loadLocked() {
        if (loaded) return
        val result = if (file.exists()) {
            runCatching { decode(file.readText()) }.getOrElse { DecodeResult(emptyList(), hadErrors = true) }
        } else {
            DecodeResult(emptyList(), hadErrors = false)
        }
        if (result.hadErrors) {
            // Keep the original untouched – the next save would otherwise overwrite it. Only once per
            // content: the widget and the alarm receivers load the file too and must not pile up copies.
            runCatching {
                val content = file.readBytes()
                val alreadyKept = directory.listFiles().orEmpty()
                    .any { it.name.startsWith("countdowns-unreadable-") && it.readBytes().contentEquals(content) }
                if (!alreadyKept) file.copyTo(File(directory, "countdowns-unreadable-${System.currentTimeMillis()}.json"))
            }
            _loadProblem.value = true
        }
        _countdowns.value = result.items
        loaded = true
        _isLoaded.value = true
    }

    private suspend fun mutate(block: (List<Countdown>) -> List<Countdown>) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                loadLocked() // never write before the existing data is known
                val updated = block(_countdowns.value)
                write(encode(updated))
                _countdowns.value = updated
            }
        }

    private fun write(json: String) {
        val tmp = File(directory, "$FILE_NAME.tmp")
        tmp.writeText(json)
        if (!tmp.renameTo(file)) {
            // Some file systems refuse to replace an existing file via rename.
            file.writeText(json)
            tmp.delete()
        }
    }

    private class DecodeResult(val items: List<Countdown>, val hadErrors: Boolean)

    private fun decode(json: String): DecodeResult {
        val array = JSONArray(json)
        var hadErrors = false
        // A single broken entry must not cost all the others.
        val items = (0 until array.length()).mapNotNull { i ->
            runCatching { decodeItem(array.getJSONObject(i)) }
                .onFailure { hadErrors = true }
                .getOrNull()
        }
        return DecodeResult(items, hadErrors)
    }

    private fun decodeItem(o: JSONObject) = Countdown(
        id = o.getLong("id"),
        name = o.getString("name"),
        date = LocalDate.parse(o.getString("date")),
        time = o.optString("time").takeIf { it.isNotEmpty() }?.let(LocalTime::parse),
        emoji = o.optString("emoji").takeIf { it.isNotEmpty() },
        kind = Kind.entries.firstOrNull { it.name == o.optString("kind") } ?: Kind.COUNTDOWN,
        autoCountUp = o.optBoolean("autoCountUp", false),
        pinned = o.optBoolean("pinned", false),
        shape = o.optInt("shape", -1),
        notify = o.optBoolean("notify", true),
        reminder = Reminder.entries.firstOrNull { it.name == o.optString("reminder") } ?: Reminder.NONE,
        color = o.optString("color").takeIf { it.isNotEmpty() },
        milestones = o.optBoolean("milestones", false),
    )

    private fun encode(list: List<Countdown>): String = JSONArray().apply {
        list.forEach { c ->
            put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("date", c.date.toString())
                c.time?.let { put("time", it.toString()) }
                c.emoji?.let { put("emoji", it) }
                put("kind", c.kind.name)
                put("autoCountUp", c.autoCountUp)
                put("pinned", c.pinned)
                put("shape", c.shape)
                put("notify", c.notify)
                put("reminder", c.reminder.name)
                c.color?.let { put("color", it) }
                put("milestones", c.milestones)
            })
        }
    }.toString()

    private companion object {
        const val FILE_NAME = "countdowns.json"
    }
}
