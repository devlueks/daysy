package io.github.devlueks.daysy.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    /** Show date and time below the title on each card. */
    val showDate: Boolean = true,
    /** Show the emoji large and on its own instead of inside a background shape. */
    val bigEmoji: Boolean = false,
    /** Compact cards: no emoji, title and date on one line. */
    val compact: Boolean = false,
    /** How many time units a card shows (1–3). */
    val unitCount: Int = 3,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Master switch for all countdown notifications. */
    val notificationsEnabled: Boolean = true,
    /** Early warning preselected for new countdowns. */
    val defaultReminder: Reminder = Reminder.NONE,
)

/** Small settings store on top of SharedPreferences. */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    fun update(transform: (Settings) -> Settings) {
        val new = transform(_settings.value).let { it.copy(unitCount = it.unitCount.coerceIn(1, 3)) }
        _settings.value = new
        prefs.edit()
            .putBoolean("showDate", new.showDate)
            .putBoolean("bigEmoji", new.bigEmoji)
            .putBoolean("compact", new.compact)
            .putInt("unitCount", new.unitCount)
            .putString("theme", new.theme.name)
            .putBoolean("notificationsEnabled", new.notificationsEnabled)
            .putString("defaultReminder", new.defaultReminder.name)
            // Settings that no longer exist.
            .remove("autoCountUpAll")
            .remove("useWeeks")
            .apply()
    }

    private fun read() = Settings(
        showDate = prefs.getBoolean("showDate", true),
        bigEmoji = prefs.getBoolean("bigEmoji", false),
        compact = prefs.getBoolean("compact", false),
        unitCount = prefs.getInt("unitCount", 3).coerceIn(1, 3),
        theme = ThemeMode.entries.firstOrNull { it.name == prefs.getString("theme", null) } ?: ThemeMode.SYSTEM,
        notificationsEnabled = prefs.getBoolean("notificationsEnabled", true),
        defaultReminder = Reminder.entries.firstOrNull { it.name == prefs.getString("defaultReminder", null) }
            ?: Reminder.NONE,
    )
}
