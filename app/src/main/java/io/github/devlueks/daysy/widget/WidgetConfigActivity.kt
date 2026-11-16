package io.github.devlueks.daysy.widget

import io.github.devlueks.daysy.R
import androidx.compose.ui.res.stringResource
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import io.github.devlueks.daysy.data.Countdown
import io.github.devlueks.daysy.data.CountdownRepository
import io.github.devlueks.daysy.data.SettingsRepository
import io.github.devlueks.daysy.data.ThemeMode
import io.github.devlueks.daysy.ui.CounterBadge
import io.github.devlueks.daysy.ui.ScreenHeader
import io.github.devlueks.daysy.ui.cardPalette
import io.github.devlueks.daysy.ui.dateLine
import io.github.devlueks.daysy.ui.state
import io.github.devlueks.daysy.ui.theme.CountDownTheme
import io.github.devlueks.daysy.ui.theme.ExpressiveFont
import kotlinx.coroutines.launch

/** Opened when a widget is placed (or reconfigured): pick the counter it shows. */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Backing out without a choice must not leave an empty widget behind.
        setResult(RESULT_CANCELED, resultIntent())
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        enableEdgeToEdge()
        val theme = SettingsRepository(this).settings.value.theme

        setContent {
            val repository = remember { CountdownRepository(applicationContext) }
            LaunchedEffect(Unit) { repository.load() }
            val countdowns by repository.countdowns.collectAsState()
            val isLoaded by repository.isLoaded.collectAsState()
            val dark = when (theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            CountDownTheme(darkTheme = dark) {
                Scaffold(
                    topBar = {
                        ScreenHeader(
                            title = stringResource(R.string.widget_config_title),
                            trailing = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cancel))
                                }
                            },
                        )
                    },
                ) { padding ->
                    if (isLoaded && countdowns.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.widget_config_empty),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp,
                                top = padding.calculateTopPadding() + 4.dp,
                                bottom = padding.calculateBottomPadding() + 16.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            item {
                                Text(
                                    stringResource(R.string.widget_config_question),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                                )
                            }
                            items(countdowns.sortedByDescending { it.pinned }, key = { it.id }) { c ->
                                Choice(c) { choose(c) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun choose(countdown: Countdown) {
        WidgetPrefs.set(this, appWidgetId, countdown.id)
        lifecycleScope.launch {
            runCatching {
                val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
                CountdownWidget.refresh(this@WidgetConfigActivity, glanceId)
            }
            setResult(RESULT_OK, resultIntent())
            finish()
        }
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}

@androidx.compose.runtime.Composable
private fun Choice(countdown: Countdown, onClick: () -> Unit) {
    val palette = cardPalette(countdown.state(System.currentTimeMillis()), countdown.color)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = palette.container,
        contentColor = palette.content,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CounterBadge(countdown = countdown, palette = palette, size = 44, bigEmoji = false)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    countdown.name,
                    fontFamily = ExpressiveFont,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(countdown.dateLine(), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            }
        }
    }
}
