package io.github.devlueks.daysy.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.devlueks.daysy.data.Countdown
import io.github.devlueks.daysy.data.CountdownRepository
import io.github.devlueks.daysy.data.Settings
import io.github.devlueks.daysy.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import io.github.devlueks.daysy.notify.CountdownNotifications
import io.github.devlueks.daysy.widget.CountdownWidget
import kotlinx.coroutines.launch

class CountdownViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CountdownRepository(application)
    private val settingsRepository = SettingsRepository(application)
    val countdowns: StateFlow<List<Countdown>> = repository.countdowns
    val isLoaded: StateFlow<Boolean> = repository.isLoaded
    val loadProblem: StateFlow<Boolean> = repository.loadProblem
    val settings: StateFlow<Settings> = settingsRepository.settings

    /**
     * Entries deleted since the last "Rückgängig" offer ended, so the list can bring all of them
     * back – not just the most recent one when several are deleted in a row.
     */
    private val _recentlyDeleted = MutableStateFlow<List<Countdown>>(emptyList())
    val recentlyDeleted: StateFlow<List<Countdown>> = _recentlyDeleted.asStateFlow()

    init {
        viewModelScope.launch {
            repository.load()
            // Keep the scheduled notifications in step with every change to counters or settings.
            combine(repository.countdowns, settingsRepository.settings) { list, s -> list to s.notificationsEnabled }
                .collect { (list, enabled) ->
                    CountdownNotifications.sync(application, list, enabled)
                    // Home-screen widgets show the same data.
                    runCatching { CountdownWidget.refreshAll(application) }
                }
        }
    }

    /** Re-sync after a permission change (e.g. exact alarms were just allowed). */
    fun resyncNotifications() {
        CountdownNotifications.sync(getApplication(), countdowns.value, settings.value.notificationsEnabled)
    }

    fun save(countdown: Countdown) {
        viewModelScope.launch { repository.upsert(countdown) }
    }

    fun delete(countdown: Countdown) {
        viewModelScope.launch { repository.delete(countdown.id) }
        _recentlyDeleted.update { list -> list.filterNot { it.id == countdown.id } + countdown }
    }

    fun undoDelete() {
        val restore = _recentlyDeleted.getAndUpdate { emptyList() }
        viewModelScope.launch { restore.forEach { repository.upsert(it) } }
    }

    fun clearRecentlyDeleted() {
        _recentlyDeleted.value = emptyList()
    }

    fun updateSettings(transform: (Settings) -> Settings) = settingsRepository.update(transform)
}
