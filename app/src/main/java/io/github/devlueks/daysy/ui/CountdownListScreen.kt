package io.github.devlueks.daysy.ui

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.devlueks.daysy.R
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import io.github.devlueks.daysy.data.Countdown
import io.github.devlueks.daysy.data.Kind
import io.github.devlueks.daysy.data.Settings
import io.github.devlueks.daysy.data.UnitValue
import io.github.devlueks.daysy.data.breakdown
import io.github.devlueks.daysy.data.topUnits
import io.github.devlueks.daysy.data.nextMilestone
import io.github.devlueks.daysy.data.recentMilestone
import io.github.devlueks.daysy.data.milestoneProgress
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.ui.semantics.contentDescription
import io.github.devlueks.daysy.ui.theme.ExpressiveFont
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

private const val NEW_ENTRY = -1L

/** Space the floating tab bar (+ button) takes at the bottom, incl. its margin. */
private val FLOATING_BAR_SPACE = 96.dp

/** Height of the floating tab pill and the + next to it. */
private val FLOATING_BAR_HEIGHT = 64.dp

private enum class CountdownFilter { Running, Expired }

private data class Entry(val countdown: Countdown, val state: DisplayState)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CountdownListScreen(
    viewModel: CountdownViewModel,
    onOpenSettings: () -> Unit,
) {
    val countdowns by viewModel.countdowns.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val recentlyDeleted by viewModel.recentlyDeleted.collectAsStateWithLifecycle()
    val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()
    val loadProblem by viewModel.loadProblem.collectAsStateWithLifecycle()
    val now by rememberNow()

    val entries = countdowns.map { Entry(it, it.state(now)) }
    val countdownTab = entries
        .filter { it.state == DisplayState.Running || it.state == DisplayState.Expired }
        .sortedWith(
            compareByDescending<Entry> { it.countdown.pinned }
                .thenBy { it.state == DisplayState.Expired }
                .thenBy { if (it.state == DisplayState.Expired) -it.countdown.targetMillis() else it.countdown.targetMillis() }
        )
    val countUpTab = entries
        .filter { it.state == DisplayState.CountingUp || it.state == DisplayState.NotStarted }
        .sortedWith(
            compareByDescending<Entry> { it.countdown.pinned }
                .thenByDescending { it.countdown.targetMillis() }
        )
    val runningCount = countdownTab.count { it.state == DisplayState.Running }
    val expiredCount = countdownTab.size - runningCount

    var filter by rememberSaveable { mutableStateOf(CountdownFilter.Running) }
    // Which editor is open: null = none, NEW_ENTRY = new one, otherwise the id being edited.
    // Saveable, so an open editor survives rotation and dark-mode switches.
    var editorTarget by rememberSaveable { mutableStateOf<Long?>(null) }
    // Swipe left/right between "Countdowns" and "Zeit seit" (the bottom bar follows along).
    val pagerState = rememberPagerState { 2 }
    val tab = pagerState.targetPage
    val listStates = listOf(rememberLazyListState(), rememberLazyListState())
    val snackbarHostState = remember { SnackbarHostState() }
    val res = LocalResources.current
    val scope = rememberCoroutineScope()

    // Stored data was (partly) unreadable – a copy was kept, tell the user once.
    LaunchedEffect(loadProblem) {
        if (loadProblem) {
            snackbarHostState.showSnackbar(
                IconSnackbarVisuals(
                    message = res.getString(R.string.load_problem),
                    icon = Icons.Rounded.Warning,
                    withDismissAction = true,
                    duration = SnackbarDuration.Long,
                )
            )
        }
    }

    // Offer "Rückgängig" after deleting. Several deletions in a row are collected, so
    // undo brings all of them back instead of only the last one.
    LaunchedEffect(recentlyDeleted) {
        val deleted = recentlyDeleted.ifEmpty { return@LaunchedEffect }
        snackbarHostState.currentSnackbarData?.dismiss()
        val result = snackbarHostState.showSnackbar(
            IconSnackbarVisuals(
                message = if (deleted.size == 1) res.getString(R.string.deleted_one, deleted.single().name)
                else res.getQuantityString(R.plurals.deleted_many, deleted.size, deleted.size),
                icon = Icons.Rounded.Delete,
                actionLabel = res.getString(R.string.undo),
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete() else viewModel.clearRecentlyDeleted()
    }

    val onEdit: (Countdown) -> Unit = { editorTarget = it.id }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ScreenHeader(
                title = stringResource(if (tab == 0) R.string.tab_countdowns else R.string.tab_since),
                trailing = {
                    // Round, like the other icon buttons in the app.
                    FilledTonalIconButton(
                        onClick = onOpenSettings,
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.padding(end = 8.dp), // right edge in line with the cards
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
    ) { padding ->
      Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            beyondViewportPageCount = 1,
            key = { it },
        ) { page ->
            // Room for the floating tab bar + button, so the last card can scroll above them.
            val bottom = padding.calculateBottomPadding() + FLOATING_BAR_SPACE
            if (page == 0) {
                if (!isLoaded) {
                    // Nothing yet – avoids flashing the "no entries" hint during start-up.
                } else if (countdownTab.isEmpty()) {
                    EmptyState(
                        emoji = "⏳",
                        shape = MaterialShapes.Sunny,
                        title = stringResource(R.string.empty_countdowns_title),
                        text = stringResource(R.string.empty_countdowns_text),
                        bottomPadding = padding.calculateBottomPadding() + FLOATING_BAR_SPACE,
                    )
                } else {
                    val visible = countdownTab.filter {
                        when (filter) {
                            CountdownFilter.Running -> it.state == DisplayState.Running
                            CountdownFilter.Expired -> it.state == DisplayState.Expired
                        }
                    }
                    EntryList(visible, now, settings, bottom, listStates[0], onEdit) {
                        item(key = "filters") {
                            FilterRow(filter, { filter = it }, runningCount, expiredCount)
                        }
                        if (visible.isEmpty()) item(key = "nothing") {
                            Text(
                                text = stringResource(if (filter == CountdownFilter.Expired) R.string.none_expired else R.string.none_active),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp),
                            )
                        }
                    }
                }
            } else {
                if (!isLoaded) {
                    // see above
                } else if (countUpTab.isEmpty()) {
                    EmptyState(
                        emoji = "🌱",
                        shape = MaterialShapes.Clover4Leaf,
                        title = stringResource(R.string.empty_since_title),
                        text = stringResource(R.string.empty_since_text),
                        bottomPadding = padding.calculateBottomPadding() + FLOATING_BAR_SPACE,
                    )
                } else {
                    EntryList(countUpTab, now, settings, bottom, listStates[1], onEdit)
                }
            }
        }

        // Floating bottom: snackbar, then the tab pill with the + next to it. The lists scroll behind.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = padding.calculateBottomPadding() + 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SnackbarHost(snackbarHostState) { ExpressiveSnackbar(it) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FloatingTabs(
                    selected = tab,
                    onSelect = { index ->
                        scope.launch {
                            if (tab == index) listStates[index].animateScrollToItem(0)
                            else pagerState.animateScrollToPage(index)
                        }
                    },
                )
                AddButton(onClick = { editorTarget = NEW_ENTRY })
            }
        }
      }
    }

    val target = editorTarget
    val editing = target?.takeIf { it != NEW_ENTRY }?.let { id -> countdowns.firstOrNull { it.id == id } }
    // Edit: wait until the entry is available (e.g. right after the app was restored).
    if (target != null && (target == NEW_ENTRY || editing != null)) {
        CountdownEditorSheet(
            initial = editing,
            initialKind = if (tab == 0) Kind.COUNTDOWN else Kind.COUNTUP,
            notificationsEnabled = settings.notificationsEnabled,
            defaultReminder = settings.defaultReminder,
            onDismiss = { editorTarget = null },
            onSave = {
                viewModel.save(it)
                editorTarget = null
            },
            onDelete = { deleted ->
                editorTarget = null
                viewModel.delete(deleted)
            },
        )
    }
}

@Composable
private fun EntryList(
    entries: List<Entry>,
    now: Long,
    settings: Settings,
    bottomPadding: Dp,
    state: LazyListState,
    onEdit: (Countdown) -> Unit,
    header: LazyListScope.() -> Unit = {},
) {
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        header()
        items(entries, key = { it.countdown.id }) { entry ->
            EntryCard(
                countdown = entry.countdown,
                state = entry.state,
                now = now,
                settings = settings,
                onClick = { onEdit(entry.countdown) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

/** Single-select filter as an M3 Expressive connected button group. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FilterRow(
    selected: CountdownFilter,
    onSelect: (CountdownFilter) -> Unit,
    runningCount: Int,
    expiredCount: Int,
) {
    val options = listOf(
        Triple(CountdownFilter.Running, stringResource(R.string.filter_active), runningCount),
        Triple(CountdownFilter.Expired, stringResource(R.string.filter_expired), expiredCount),
    )
    // Spans the full card width, split exactly in half.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, (value, label, count) ->
            val checked = selected == value
            ToggleButton(
                checked = checked,
                onCheckedChange = { onSelect(value) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shapes = if (index == 0) ButtonGroupDefaults.connectedLeadingButtonShapes()
                else ButtonGroupDefaults.connectedTrailingButtonShapes(),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Text(label, maxLines = 1, softWrap = false)
                Spacer(Modifier.width(6.dp))
                CountBadge(count, checked)
            }
        }
    }
}

/** Number of entries in a small circle after the filter label; inverts on the selected button. */
@Composable
private fun CountBadge(count: Int, selected: Boolean) {
    val c = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
            .clip(CircleShape)
            .background(if (selected) c.onPrimary else c.surfaceContainerHighest)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) c.primary else c.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun EntryCard(
    countdown: Countdown,
    state: DisplayState,
    now: Long,
    settings: Settings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.colorScheme
    val palette = cardPalette(state, countdown.color)
    val parts = breakdown(Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()), countdown.target())

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = palette.container,
        contentColor = palette.content,
    ) {
        Column(Modifier.padding(if (settings.compact) 18.dp else 20.dp)) {
            if (settings.compact) {
                CompactHeader(countdown, state, settings.showDate, palette)
            } else Row(verticalAlignment = Alignment.CenterVertically) {
                CounterBadge(
                    countdown = countdown,
                    palette = palette,
                    size = 56,
                    bigEmoji = settings.bigEmoji,
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = countdown.name,
                        fontFamily = ExpressiveFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (settings.showDate) 22.sp else 26.sp,
                        lineHeight = if (settings.showDate) 26.sp else 30.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (settings.showDate) {
                        Text(text = countdown.dateLine(), style = MaterialTheme.typography.bodyMedium)
                    }
                    if (state == DisplayState.CountingUp && countdown.kind == Kind.COUNTDOWN) {
                        Spacer(Modifier.height(6.dp))
                        Tag(stringResource(R.string.tag_since_end), palette.badge, palette.onBadge)
                    }
                }
                if (countdown.pinned) {
                    Icon(
                        painterResource(R.drawable.ic_pin),
                        contentDescription = stringResource(R.string.pinned),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(20.dp)
                            .align(Alignment.Top),
                    )
                }
            }
            Spacer(Modifier.height(if (settings.compact) 6.dp else 12.dp))
            if (state == DisplayState.Expired) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tag(stringResource(R.string.tag_expired), c.errorContainer, c.onErrorContainer)
                    Spacer(Modifier.width(12.dp))
                    Text(LocalResources.current.agoText(parts), fontFamily = ExpressiveFont, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                }
            } else {
                ExpressiveUnits(topUnits(parts, settings.unitCount))
            }
            if (countdown.kind == Kind.COUNTUP && countdown.milestones && state == DisplayState.CountingUp) {
                MilestoneProgress(countdown, now, palette)
            }
        }
    }
}

/**
 * Compact mode: no emoji, title and a short date share one line (baseline-aligned),
 * e.g. "GTA VI   Do., 19. Nov." – a long title is shortened, the date stays readable.
 */
@Composable
private fun CompactHeader(countdown: Countdown, state: DisplayState, showDate: Boolean, palette: CardPalette) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f)) {
            Text(
                text = countdown.name,
                fontFamily = ExpressiveFont,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .alignByBaseline(),
            )
            if (showDate) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = countdown.compactDateLine(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        if (state == DisplayState.CountingUp && countdown.kind == Kind.COUNTDOWN) {
            Spacer(Modifier.width(8.dp))
            Tag(stringResource(R.string.tag_since_end), palette.badge, palette.onBadge)
        }
        if (countdown.pinned) {
            Icon(
                painterResource(R.drawable.ic_pin),
                contentDescription = stringResource(R.string.pinned),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(18.dp),
            )
        }
    }
}

/** Short date for one line: "Do., 19. Nov." (this year), "19. Nov. 2027" otherwise, plus the time if set. */
private fun Countdown.compactDateLine(): String =
    shortDate(date, weekday = date.year == java.time.LocalDate.now().year) +
        (time?.let { " · " + shortTime(it) } ?: "")

/**
 * "Zeit seit" with milestones switched on: the next milestone with a wavy progress bar
 * from the previous one, plus a small celebration tag for a day after reaching one.
 *
 *   🏁 1 Woche                 in 6 Tagen
 *   ～～～～～～～─────────────────
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MilestoneProgress(countdown: Countdown, now: Long, palette: CardPalette) {
    val nowZoned = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
    val start = countdown.target()
    val next = nextMilestone(start, nowZoned) ?: return
    val recent = recentMilestone(start, nowZoned)
    val progress = milestoneProgress(start, nowZoned)
    val res = LocalResources.current
    val remaining = res.inText(breakdown(nowZoned, next.at))
    val nextLabel = res.milestoneLabel(next)

    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_flag), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = nextLabel,
            fontFamily = ExpressiveFont,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        Spacer(Modifier.weight(1f))
        Text(remaining, style = MaterialTheme.typography.labelLarge)
    }
    Spacer(Modifier.height(8.dp))
    LinearWavyProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = res.getString(R.string.milestone_next_description, nextLabel, remaining) },
        color = palette.badge,
        trackColor = palette.content.copy(alpha = 0.18f),
    )
    if (recent != null) {
        Spacer(Modifier.height(12.dp))
        Tag(stringResource(R.string.milestone_reached, res.milestoneLabel(recent)), palette.badge, palette.onBadge)
    }
}

/**
 * The time units as compact pairs ("5 Tage  11 Std  58 Min"): number and label share a
 * baseline, the pairs sit close together and wrap to a new line on narrow screens.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpressiveUnits(units: List<UnitValue>) {
    val res = LocalResources.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        itemVerticalAlignment = Alignment.Bottom,
    ) {
        units.forEach { part ->
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold)) {
                        append(part.value.toString())
                    }
                    append(' ')
                    withStyle(SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)) {
                        append(res.unitLabel(part))
                    }
                },
                fontFamily = ExpressiveFont,
                style = TextStyle(fontFeatureSettings = "tnum"), // equal-width digits, no jiggle
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun EmptyState(
    emoji: String,
    shape: RoundedPolygon,
    title: String,
    text: String,
    bottomPadding: Dp,
) {
    // Centered in the visible area between the header and the navigation bar.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmojiBadge(
            emoji = emoji,
            fallback = "",
            color = MaterialTheme.colorScheme.tertiaryContainer,
            size = 140,
            shape = shape,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = ExpressiveFont,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}


/**
 * Floating navigation in the style of an M3 Expressive floating toolbar: a slim elevated pill.
 * The active tab shows icon + label in a filled capsule, the other one only its icon –
 * switching tabs smoothly grows/shrinks the capsules.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FloatingTabs(selected: Int, onSelect: (Int) -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = c.surfaceContainerHighest,
        shadowElevation = 8.dp,
        modifier = Modifier
            .height(FLOATING_BAR_HEIGHT)
            .width(280.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                stringResource(R.string.tab_countdowns) to R.drawable.ic_countdown,
                stringResource(R.string.tab_since) to R.drawable.ic_countup,
            ).forEachIndexed { index, (label, icon) ->
                val isSelected = selected == index
                // Fixed pill width: the active capsule takes more room, so the + next to it never moves.
                val weight by animateFloatAsState(if (isSelected) 2.2f else 1f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "tabWeight")
                val background by animateColorAsState(
                    // Fade from the same color at alpha 0 – animating from Color.Transparent (= transparent
                    // black) would pass through a dark tone and flash black.
                    if (isSelected) c.primaryContainer else c.primaryContainer.copy(alpha = 0f), label = "tabBackground",
                )
                val content by animateColorAsState(
                    if (isSelected) c.onPrimaryContainer else c.onSurfaceVariant, label = "tabContent",
                )
                Row(
                    modifier = Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(background)
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(index) })
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painterResource(icon),
                        contentDescription = if (isSelected) null else label,
                        tint = content,
                    )
                    if (isSelected) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            label,
                            color = content,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                        )
                    }
                }
            }
        }
    }
}

/** The + next to the floating tabs: same height, strong primary color. */
@Composable
private fun AddButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.size(FLOATING_BAR_HEIGHT),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add), modifier = Modifier.size(28.dp))
    }
}
