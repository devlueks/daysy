package io.github.devlueks.daysy.ui

import io.github.devlueks.daysy.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.devlueks.daysy.data.Settings
import io.github.devlueks.daysy.data.Reminder
import io.github.devlueks.daysy.notify.CountdownNotifications
import io.github.devlueks.daysy.notify.openExactAlarmSettings
import io.github.devlueks.daysy.notify.openNotificationSettings
import io.github.devlueks.daysy.notify.rememberNotificationPermissionRequest
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.devlueks.daysy.data.ThemeMode
import io.github.devlueks.daysy.ui.theme.ExpressiveFont
import io.github.devlueks.daysy.ui.theme.ExpressiveHeadline

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onUpdate: ((Settings) -> Settings) -> Unit,
    onBack: () -> Unit,
    onPermissionsChanged: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val requestNotificationPermission = rememberNotificationPermissionRequest()
    var canPost by remember { mutableStateOf(CountdownNotifications.canPostNotifications(context)) }
    var canExact by remember { mutableStateOf(CountdownNotifications.canScheduleExact(context)) }
    // Coming back from a system settings page (or the permission dialog): refresh and reschedule.
    LifecycleResumeEffect(Unit) {
        canPost = CountdownNotifications.canPostNotifications(context)
        canExact = CountdownNotifications.canScheduleExact(context)
        onPermissionsChanged()
        onPauseOrDispose { }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ScreenHeader(
                title = stringResource(R.string.settings),
                leading = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 32.dp,
            ),
            modifier = Modifier.navigationBarsPadding(),
        ) {
            item {
                // The emoji option has no effect in compact mode (no emoji there), so it is hidden then.
                val count = if (settings.compact) 3 else 4
                var index = 0
                Section(stringResource(R.string.section_appearance)) {
                    SwitchRow(
                        title = stringResource(R.string.compact_mode),
                        subtitle = if (settings.compact) stringResource(R.string.compact_on)
                        else stringResource(R.string.compact_off),
                        checked = settings.compact,
                        onChange = { v -> onUpdate { it.copy(compact = v) } },
                        shape = groupShape(index++, count),
                    )
                    SwitchRow(
                        title = stringResource(R.string.show_date),
                        subtitle = when {
                            !settings.showDate -> stringResource(R.string.show_date_off)
                            settings.compact -> stringResource(R.string.show_date_compact)
                            else -> stringResource(R.string.show_date_on)
                        },
                        checked = settings.showDate,
                        onChange = { v -> onUpdate { it.copy(showDate = v) } },
                        shape = groupShape(index++, count),
                    )
                    if (!settings.compact) {
                        SwitchRow(
                            title = stringResource(R.string.big_emoji),
                            subtitle = if (settings.bigEmoji) stringResource(R.string.big_emoji_on)
                            else stringResource(R.string.big_emoji_off),
                            checked = settings.bigEmoji,
                            onChange = { v -> onUpdate { it.copy(bigEmoji = v) } },
                            shape = groupShape(index++, count),
                        )
                    }
                    ChoiceRow(
                        title = stringResource(R.string.unit_count),
                        subtitle = stringResource(R.string.unit_count_hint),
                        options = listOf(1 to "1", 2 to "2", 3 to "3"),
                        selected = settings.unitCount,
                        onSelect = { v -> onUpdate { it.copy(unitCount = v) } },
                        shape = groupShape(index, count),
                    )
                }
            }
            item {
                // Rows depend on what the system currently allows; re-checked whenever we come back.
                val showBlocked = settings.notificationsEnabled && !canPost
                val showExact = settings.notificationsEnabled && canPost && !canExact
                val count = 2 + (if (showBlocked) 1 else 0) + (if (showExact) 1 else 0)
                var index = 0
                Section(stringResource(R.string.section_notifications)) {
                    SwitchRow(
                        title = stringResource(R.string.section_notifications),
                        subtitle = if (settings.notificationsEnabled) stringResource(R.string.notifications_on)
                        else stringResource(R.string.notifications_off),
                        checked = settings.notificationsEnabled,
                        onChange = { v ->
                            onUpdate { it.copy(notificationsEnabled = v) }
                            if (v) requestNotificationPermission()
                        },
                        shape = groupShape(index++, count),
                    )
                    if (showBlocked) {
                        ActionRow(
                            title = stringResource(R.string.notifications_blocked),
                            subtitle = stringResource(R.string.notifications_blocked_hint),
                            onClick = { openNotificationSettings(context) },
                            shape = groupShape(index++, count),
                            warning = true,
                        )
                    }
                    if (showExact) {
                        ActionRow(
                            title = stringResource(R.string.exact_alarms),
                            subtitle = stringResource(R.string.exact_alarms_hint),
                            onClick = { openExactAlarmSettings(context) },
                            shape = groupShape(index++, count),
                        )
                    }
                    ChoiceRow(
                        title = stringResource(R.string.default_reminder),
                        subtitle = stringResource(R.string.default_reminder_hint),
                        options = Reminder.entries.map { it to stringResource(it.labelRes) },
                        selected = settings.defaultReminder,
                        onSelect = { v -> onUpdate { it.copy(defaultReminder = v) } },
                        shape = groupShape(index, count),
                    )
                }
            }
            item {
                Section(stringResource(R.string.section_design)) {
                    ChoiceRow(
                        title = stringResource(R.string.theme),
                        subtitle = stringResource(R.string.theme_hint),
                        options = listOf(
                            ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                            ThemeMode.LIGHT to stringResource(R.string.theme_light),
                            ThemeMode.DARK to stringResource(R.string.theme_dark),
                        ),
                        selected = settings.theme,
                        onSelect = { v -> onUpdate { it.copy(theme = v) } },
                        shape = groupShape(0, 1),
                    )
                }
            }
            item {
                Text(
                    stringResource(R.string.settings_tip),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                )
            }
        }
    }
}

/** Expressive grouped list: big outer corners, small inner corners, 2dp gaps. */
internal fun groupShape(index: Int, count: Int): Shape {
    val big = 24.dp
    val small = 6.dp
    return RoundedCornerShape(
        topStart = if (index == 0) big else small,
        topEnd = if (index == 0) big else small,
        bottomStart = if (index == count - 1) big else small,
        bottomEnd = if (index == count - 1) big else small,
    )
}

@Composable
internal fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(bottom = 24.dp)) {
        Text(
            title,
            fontFamily = ExpressiveFont,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    shape: Shape,
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.padding(start = 16.dp))
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun <T> ChoiceRow(
    title: String,
    subtitle: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    shape: Shape,
) {
    Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                options.forEachIndexed { index, (value, label) ->
                    ToggleButton(
                        checked = selected == value,
                        onCheckedChange = { onSelect(value) },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { role = Role.RadioButton },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        // The row itself is surfaceContainer, so unselected buttons need a higher tone.
                        colors = ToggleButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                    ) { Text(label, maxLines = 1, softWrap = false) }
                }
            }
        }
    }
}

/** A row that leads somewhere (system settings), with a chevron on the right. */
@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    shape: Shape,
    warning: Boolean = false,
) {
    val c = MaterialTheme.colorScheme
    Surface(
        shape = shape,
        color = if (warning) c.errorContainer else c.surfaceContainer,
        contentColor = if (warning) c.onErrorContainer else c.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (warning) c.onErrorContainer else c.onSurfaceVariant,
                )
            }
            Spacer(Modifier.padding(start = 16.dp))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}
