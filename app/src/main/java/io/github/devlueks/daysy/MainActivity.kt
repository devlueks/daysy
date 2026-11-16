package io.github.devlueks.daysy

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.devlueks.daysy.data.ThemeMode
import io.github.devlueks.daysy.ui.CountdownListScreen
import io.github.devlueks.daysy.ui.CountdownViewModel
import io.github.devlueks.daysy.ui.SettingsScreen
import io.github.devlueks.daysy.ui.theme.CountDownTheme

class MainActivity : ComponentActivity() {

    /** Bumped when a notification is tapped while the app is open: go back to the list. */
    private val showListRequest = mutableIntStateOf(0)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        showListRequest.intValue++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: CountdownViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val dark = when (settings.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Keep status/navigation bar icons readable when the app theme differs from the system.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose {}
            }

            CountDownTheme(darkTheme = dark) {
                var settingsOpen by rememberSaveable { mutableStateOf(false) }
                // Keeps the list's tab, filter and scroll position while the settings are open.
                val stateHolder = rememberSaveableStateHolder()
                val showList by showListRequest
                LaunchedEffect(showList) { if (showList > 0) settingsOpen = false }

                AnimatedContent(
                    targetState = settingsOpen,
                    transitionSpec = {
                        val dir = if (targetState) 1 else -1
                        (slideInHorizontally { it / 4 * dir } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 4 * dir } + fadeOut())
                    },
                    label = "screen",
                ) { open ->
                    if (open) {
                        SettingsScreen(
                            settings = settings,
                            onUpdate = viewModel::updateSettings,
                            onBack = { settingsOpen = false },
                            onPermissionsChanged = viewModel::resyncNotifications,
                        )
                    } else {
                        stateHolder.SaveableStateProvider("list") {
                            CountdownListScreen(
                                viewModel = viewModel,
                                onOpenSettings = { settingsOpen = true },
                            )
                        }
                    }
                }
            }
        }
    }
}
