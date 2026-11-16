package io.github.devlueks.daysy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material.icons.rounded.Close
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.SheetValue
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.ui.res.painterResource
import io.github.devlueks.daysy.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.text.font.FontWeight
import io.github.devlueks.daysy.ui.theme.ExpressiveFont
import io.github.devlueks.daysy.data.Countdown
import io.github.devlueks.daysy.data.Reminder
import io.github.devlueks.daysy.notify.rememberNotificationPermissionRequest
import androidx.compose.material3.ToggleButtonDefaults
import java.time.LocalDateTime
import io.github.devlueks.daysy.data.Kind
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

private val presetEmojis = listOf(
    "🎉", "🎂", "🎄", "✈️", "🏖️", "💍", "🎓", "🏆", "❤️", "🎁",
    "🚀", "🎸", "⚽", "📅", "🏠", "👶", "🍾", "🌴", "🎮", "📚",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CountdownEditorSheet(
    initial: Countdown?,
    initialKind: Kind,
    notificationsEnabled: Boolean,
    defaultReminder: Reminder,
    onDismiss: () -> Unit,
    onSave: (Countdown) -> Unit,
    onDelete: (Countdown) -> Unit,
) {
    fun defaultDate(k: Kind) = if (k == Kind.COUNTDOWN) LocalDate.now().plusDays(7) else LocalDate.now()

    var kind by rememberSaveable { mutableStateOf(initial?.kind ?: initialKind) }
    var autoCountUp by rememberSaveable { mutableStateOf(initial?.autoCountUp ?: false) }
    var notify by rememberSaveable { mutableStateOf(initial?.notify ?: true) }
    var reminder by rememberSaveable { mutableStateOf(initial?.reminder ?: defaultReminder) }
    val requestNotificationPermission = rememberNotificationPermissionRequest()
    var pinned by rememberSaveable { mutableStateOf(initial?.pinned ?: false) }
    var shape by rememberSaveable {
        mutableStateOf(initial?.let { BadgeShapes.indexOf(it.badgeShape()) } ?: randomBadgeShape())
    }
    var dateTouched by rememberSaveable { mutableStateOf(initial != null) }
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var emoji by rememberSaveable { mutableStateOf(initial?.emoji) }
    var color by rememberSaveable { mutableStateOf(initial?.color) }
    var milestones by rememberSaveable { mutableStateOf(initial?.milestones ?: false) }
    var date by rememberSaveable { mutableStateOf(initial?.date ?: defaultDate(kind)) }
    var useTime by rememberSaveable { mutableStateOf(initial?.time != null) }
    var time by rememberSaveable { mutableStateOf(initial?.time ?: LocalTime.of(12, 0)) }
    // An emoji that isn't one of the presets is shown (and editable) in the "own emoji" field.
    var customEmoji by rememberSaveable { mutableStateOf(initial?.emoji?.takeIf { it !in presetEmojis } ?: "") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    fun enableTime() {
        // A new count-up most likely starts "now" – a fixed 12:00 could lie in the future.
        if (kind == Kind.COUNTUP && initial?.time == null) {
            time = LocalTime.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES)
        }
        useTime = true
        showTimePicker = true
    }

    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var showEmojiPicker by rememberSaveable { mutableStateOf(false) }
    // Only an existing "Zeit seit" entry can be restarted.
    val canReset = kind == Kind.COUNTUP && initial?.kind == Kind.COUNTUP

    fun current() = Countdown(
        id = initial?.id ?: 0L,
        name = name.trim(),
        date = date,
        time = if (useTime) time else null,
        emoji = emoji,
        kind = kind,
        autoCountUp = kind == Kind.COUNTDOWN && autoCountUp,
        pinned = pinned,
        shape = shape,
        notify = notify,
        reminder = reminder,
        color = color,
        milestones = kind == Kind.COUNTUP && milestones,
    )

    val startsInFuture = kind == Kind.COUNTUP &&
        LocalDateTime.of(date, if (useTime) time else LocalTime.MIDNIGHT).isAfter(LocalDateTime.now())

    // The ✓ at the top saves; closing the sheet any other way (swipe down, back, tap outside) discards.
    // If something can't be saved, ✓ keeps the sheet open and shows the reason at the top.
    val problem = when {
        name.isBlank() -> stringResource(R.string.error_no_title)
        startsInFuture -> stringResource(R.string.error_start_in_future)
        else -> null
    }
    var triedToSave by rememberSaveable { mutableStateOf(false) }

    // Deprecated in material3 1.5, but the replacement cannot skip the half-open state yet.
    @Suppress("DEPRECATION")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        val scroll = rememberScrollState()
        // Header stays pinned at the top while the form below scrolls, so ✓ is always reachable.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        initial != null -> stringResource(R.string.editor_edit)
                        else -> stringResource(R.string.editor_new)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = ExpressiveFont,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                // "Zeit seit" only: restart at 0 (asks first).
                if (canReset) {
                    FilledTonalIconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.reset))
                    }
                }
                FilledTonalIconToggleButton(checked = pinned, onCheckedChange = { pinned = it }) {
                    Icon(
                        painterResource(R.drawable.ic_pin),
                        contentDescription = if (pinned) stringResource(R.string.unpin) else stringResource(R.string.pin),
                    )
                }
                if (initial != null) {
                    FilledTonalIconButton(onClick = { onDelete(initial) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                    }
                }
                // Always visible "done": closes and saves.
                FilledIconButton(
                    onClick = {
                        if (problem == null) scope.launch { sheetState.hide() }.invokeOnCompletion { onSave(current()) }
                        else triedToSave = true
                    },
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = if (initial == null) stringResource(R.string.add) else stringResource(R.string.done))
                }
            }

            AnimatedVisibility(visible = triedToSave && problem != null) {
                Text(
                    text = problem.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        // Thin line under the header once content has scrolled beneath it.
        val dividerAlpha by animateFloatAsState(if (scroll.canScrollBackward) 1f else 0f, label = "headerDivider")
        HorizontalDivider(
            modifier = Modifier.graphicsLayer { alpha = dividerAlpha },
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Spacer(Modifier.height(4.dp))

            // Type: connected button group (M3 Expressive)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                Kind.entries.forEachIndexed { index, k ->
                    ToggleButton(
                        checked = kind == k,
                        onCheckedChange = {
                            kind = k
                            if (!dateTouched) date = defaultDate(k)
                            // A count-up can't start in the future.
                            if (k == Kind.COUNTUP && date.isAfter(LocalDate.now())) date = LocalDate.now()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { role = Role.RadioButton },
                        shapes = if (index == 0) ButtonGroupDefaults.connectedLeadingButtonShapes()
                        else ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    ) {
                        Icon(
                            painterResource(if (k == Kind.COUNTDOWN) R.drawable.ic_countdown else R.drawable.ic_countup),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text(if (k == Kind.COUNTDOWN) stringResource(R.string.kind_countdown) else stringResource(R.string.tab_since))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Preview + title side by side – roughly how the card will look.
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Tap the badge to roll a new random shape.
                AnimatedContent(
                    targetState = shape,
                    transitionSpec = {
                        (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut())
                    },
                    label = "badgeShape",
                    modifier = Modifier.clickable(
                        onClickLabel = stringResource(R.string.other_shape),
                        interactionSource = null,
                        indication = null,
                    ) {
                        var next: Int
                        do { next = randomBadgeShape() } while (next == shape)
                        shape = next
                    },
                ) { s ->
                    // Preview in the chosen color, so picking one shows right away how it looks.
                    val own = CardColor.of(color)?.palette(isAppDark())
                    EmojiBadge(
                        emoji = emoji,
                        fallback = name,
                        color = own?.container ?: MaterialTheme.colorScheme.primaryContainer,
                        contentColor = own?.content ?: MaterialTheme.colorScheme.onPrimaryContainer,
                        size = 72,
                        shape = BadgeShapes[s],
                    )
                }
                Spacer(Modifier.width(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.title_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                )
            }

            Spacer(Modifier.height(24.dp))
            Section(stringResource(R.string.section_emoji)) {
                val noEmojiLabel = stringResource(R.string.no_emoji)
                // Start scrolled so the chosen emoji is visible (like the color row).
                val emojiRowState = rememberLazyListState(
                    initialFirstVisibleItemIndex = (presetEmojis.indexOf(emoji) + 2 - 2).coerceAtLeast(0)
                )
                LazyRow(
                    state = emojiRowState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    // stringResource(R.string.no_emoji) – the card then shows the title's first letter instead.
                    item(key = "none") {
                        val selected = emoji == null
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(if (selected) MaterialShapes.Cookie6Sided.toShape() else MaterialShapes.Circle.toShape())
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .selectable(selected = selected, role = Role.RadioButton) { emoji = null; customEmoji = "" }
                                .semantics { contentDescription = noEmojiLabel },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    // Then: type any emoji. Tapping a selected emoji again also removes it.
                    item(key = "custom") {
                        CustomEmojiButton(
                            value = customEmoji,
                            selected = emoji != null && emoji == customEmoji,
                            onClick = { showEmojiPicker = true },
                        )
                    }
                    items(presetEmojis) { e ->
                        val selected = e == emoji
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(
                                    if (selected) MaterialShapes.Cookie6Sided.toShape()
                                    else MaterialShapes.Circle.toShape()
                                )
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable { emoji = if (selected) null else e; customEmoji = "" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(e, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            Section(stringResource(R.string.section_color)) {
                // Start scrolled so the chosen color is visible (it may sit at the far end).
                val colorRowState = rememberLazyListState(
                    initialFirstVisibleItemIndex = (CardColor.entries.indexOfFirst { it.key == color } + 1 - 2).coerceAtLeast(0)
                )
                LazyRow(
                    state = colorRowState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    item(key = "system") {
                        ColorSwatch(
                            fill = MaterialTheme.colorScheme.primary,
                            selected = color == null,
                            label = stringResource(R.string.color_system),
                            system = true,
                            onClick = { color = null },
                        )
                    }
                    items(CardColor.entries, key = { it.key }) { option ->
                        ColorSwatch(
                            fill = option.swatch,
                            selected = color == option.key,
                            label = stringResource(option.labelRes),
                            onClick = { color = if (color == option.key) null else option.key },
                        )
                    }
                }
            }

            Section(if (kind == Kind.COUNTDOWN) stringResource(R.string.section_end) else stringResource(R.string.section_start)) {
                val rows = if (kind == Kind.COUNTDOWN) 3 else 2
                EditorRow(
                    title = stringResource(R.string.date),
                    subtitle = fullDate(date),
                    icon = rememberVectorPainter(Icons.Rounded.DateRange),
                    shape = groupShape(0, rows),
                    onClick = { showDatePicker = true },
                )
                EditorRow(
                    title = stringResource(R.string.time),
                    subtitle = if (useTime) shortTime(time)
                    else stringResource(R.string.all_day),
                    icon = painterResource(R.drawable.ic_schedule),
                    shape = groupShape(1, rows),
                    onClick = { if (useTime) showTimePicker = true else enableTime() },
                ) {
                    Switch(checked = useTime, onCheckedChange = { if (it) enableTime() else useTime = false })
                }
                if (kind == Kind.COUNTDOWN) {
                    EditorRow(
                        title = stringResource(R.string.keep_counting),
                        subtitle = if (autoCountUp) stringResource(R.string.keep_counting_on) else stringResource(R.string.keep_counting_off),
                        icon = painterResource(R.drawable.ic_countup),
                        shape = groupShape(2, rows),
                        onClick = { autoCountUp = !autoCountUp },
                    ) {
                        Switch(checked = autoCountUp, onCheckedChange = { autoCountUp = it })
                    }
                }
            }

            if (kind == Kind.COUNTUP) {
                Section(stringResource(R.string.section_milestones)) {
                    EditorRow(
                        title = stringResource(R.string.show_milestones),
                        subtitle = stringResource(R.string.show_milestones_hint),
                        icon = painterResource(R.drawable.ic_flag),
                        shape = groupShape(0, if (milestones) 2 else 1),
                        onClick = { milestones = !milestones },
                    ) {
                        Switch(checked = milestones, onCheckedChange = { milestones = it })
                    }
                    AnimatedVisibility(visible = milestones) {
                    EditorRow(
                        title = stringResource(R.string.notify),
                        subtitle = when {
                            !notificationsEnabled -> stringResource(R.string.disabled_in_settings)
                            notify -> stringResource(R.string.notify_milestone_hint)
                            else -> stringResource(R.string.off)
                        },
                        icon = painterResource(R.drawable.ic_notifications_on),
                        shape = groupShape(1, 2),
                        onClick = {
                            if (notificationsEnabled) {
                                notify = !notify
                                if (notify) requestNotificationPermission()
                            }
                        },
                    ) {
                        Switch(
                            checked = notify && notificationsEnabled,
                            enabled = notificationsEnabled,
                            onCheckedChange = {
                                notify = it
                                if (it) requestNotificationPermission()
                            },
                        )
                    }
                    }
                }
            }

            // End notification + early warning (countdowns only)
            if (kind == Kind.COUNTDOWN) {
                val showReminder = notify && notificationsEnabled
                Section(stringResource(R.string.section_notification)) {
                    EditorRow(
                        title = stringResource(R.string.notify_on_end),
                        subtitle = when {
                            !notificationsEnabled -> stringResource(R.string.disabled_in_settings)
                            notify -> stringResource(R.string.on)
                            else -> stringResource(R.string.off)
                        },
                        icon = painterResource(R.drawable.ic_notifications_on),
                        shape = groupShape(0, if (showReminder) 2 else 1),
                        onClick = {
                            if (notificationsEnabled) {
                                notify = !notify
                                if (notify) requestNotificationPermission()
                            }
                        },
                    ) {
                        Switch(
                            checked = notify && notificationsEnabled,
                            enabled = notificationsEnabled,
                            onCheckedChange = {
                                notify = it
                                if (it) requestNotificationPermission()
                            },
                        )
                    }
                    AnimatedVisibility(visible = showReminder) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(groupShape(1, 2))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(16.dp)
                        ) {
                            Text(
                                stringResource(R.string.extra_reminder),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            ) {
                                Reminder.entries.forEachIndexed { index, r ->
                                    ToggleButton(
                                        checked = reminder == r,
                                        onCheckedChange = { reminder = r },
                                        modifier = Modifier
                                            .weight(1f)
                                            .semantics { role = Role.RadioButton },
                                        shapes = when (index) {
                                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            Reminder.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        },
                                        colors = ToggleButtonDefaults.colors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        ),
                                        contentPadding = PaddingValues(horizontal = 4.dp),
                                    ) { Text(stringResource(r.labelRes), maxLines = 1, softWrap = false) }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = startsInFuture) {
                Text(
                    text = stringResource(R.string.error_time_in_future),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showEmojiPicker) {
        EmojiInputDialog(
            onPick = { picked ->
                emoji = picked
                customEmoji = if (picked in presetEmojis) "" else picked
                showEmojiPicker = false
            },
            onDismiss = { showEmojiPicker = false },
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
            title = { Text(stringResource(R.string.reset_title)) },
            text = { Text("„${name.trim()}“ startet jetzt wieder bei 0. Das bisherige Startdatum geht dabei verloren.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    val now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
                    onSave(current().copy(date = now.toLocalDate(), time = now.toLocalTime()))
                }) { Text(stringResource(R.string.reset)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showDatePicker) {
        val today = LocalDate.now()
        val todayUtcMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = if (kind == Kind.COUNTUP) object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtcMillis
                override fun isSelectableYear(year: Int) = year <= today.year
            } else DatePickerDefaults.AllDates,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        dateTouched = true
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } },
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.pick_time)) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    time = LocalTime.of(state.hour, state.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** A row in one of the editor's grouped sections – same look as the settings screen. */
@Composable
private fun EditorRow(
    title: String,
    subtitle: String,
    icon: Painter,
    shape: Shape,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    ListItem(
        // One line each so all rows in a group have the same height.
        supportingContent = { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier
            .clip(shape)
            .clickable(onClick = onClick),
    ) { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

/**
 * First item of the emoji row: opens the keyboard-based emoji input. Shows "+" until an own emoji
 * was picked, then that emoji (selected = cookie shape, like a chosen preset).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CustomEmojiButton(value: String, selected: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val ownEmojiLabel = stringResource(R.string.choose_own_emoji)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(if (selected) MaterialShapes.Cookie6Sided.toShape() else MaterialShapes.Circle.toShape())
            .background(if (selected) c.primary else c.surfaceContainerHighest)
            .clickable(onClickLabel = stringResource(R.string.choose_emoji), onClick = onClick)
            .semantics { contentDescription = ownEmojiLabel },
        contentAlignment = Alignment.Center,
    ) {
        if (value.isEmpty()) Icon(Icons.Rounded.Add, contentDescription = null, tint = c.onSurfaceVariant)
        else Text(value, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    }
}

/** True if [s] starts with a pictographic/symbol code point (i.e. looks like an emoji, not a letter). */
private fun looksLikeEmoji(s: String): Boolean {
    if (s.isEmpty()) return false
    val cp = s.codePointAt(0)
    return cp >= 0x1F000 || cp in 0x2190..0x2BFF || Character.getType(cp) == Character.OTHER_SYMBOL.toInt()
}

/**
 * Own emoji via the system keyboard: the field gets focus right away so the keyboard opens –
 * its emoji panel has search and recents. Picking an emoji there takes it immediately;
 * anything else (e.g. a letter) can be taken with "Ãbernehmen".
 */
@Composable
private fun EmojiInputDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.own_emoji)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.own_emoji_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { input ->
                        val g = lastGrapheme(input)
                        if (looksLikeEmoji(g)) onPick(g) else value = g
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .width(120.dp)
                        .focusRequester(focus),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(value) }, enabled = value.isNotBlank()) { Text(stringResource(R.string.apply)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * One color choice in the editor – looks like the emoji choices: a circle that turns into a
 * cookie shape with a check mark when selected. The system swatch shows the system's own roles.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColorSwatch(
    fill: Color,
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    system: Boolean = false,
) {
    val c = MaterialTheme.colorScheme
    val background = if (system) {
        Modifier.background(Brush.sweepGradient(listOf(c.primary, c.tertiary, c.secondary, c.primary)))
    } else {
        Modifier.background(fill)
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(if (selected) MaterialShapes.Cookie6Sided.toShape() else MaterialShapes.Circle.toShape())
            .then(background)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = if (system) c.onPrimary else if (fill.luminance() > 0.5f) Color.Black else Color.White,
            )
        }
    }
}
