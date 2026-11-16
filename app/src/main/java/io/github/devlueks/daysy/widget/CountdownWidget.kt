package io.github.devlueks.daysy.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.devlueks.daysy.MainActivity
import io.github.devlueks.daysy.data.CalendarUnit
import io.github.devlueks.daysy.data.Countdown
import io.github.devlueks.daysy.data.CountdownRepository
import io.github.devlueks.daysy.data.Kind
import io.github.devlueks.daysy.data.UnitValue
import io.github.devlueks.daysy.data.breakdown
import io.github.devlueks.daysy.R
import io.github.devlueks.daysy.ui.CardColor
import io.github.devlueks.daysy.ui.DisplayState

import io.github.devlueks.daysy.ui.state
import java.time.Instant
import java.time.ZoneId
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.glance.ColorFilter
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Box
import androidx.glance.layout.size
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import io.github.devlueks.daysy.data.SettingsRepository
import io.github.devlueks.daysy.ui.agoText
import io.github.devlueks.daysy.ui.shortDate
import io.github.devlueks.daysy.ui.shortTime
import io.github.devlueks.daysy.ui.unitLabel
import io.github.devlueks.daysy.ui.badgeShape
import io.github.devlueks.daysy.ui.firstGrapheme
import kotlin.math.roundToInt

/**
 * Home-screen widget showing one counter. Widgets can't tick every second, so it shows
 * days (or hours on the last day) and is refreshed every 30 minutes and whenever data changes.
 */
class CountdownWidget : GlanceAppWidget() {

    // The real size is needed to decide how many time units fit.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            // Glance keeps a widget's session alive for a while and an update() only recomposes it.
            // So the data is loaded inside the composition, keyed on a counter that every refresh bumps.
            val tick = currentState(REFRESH) ?: 0L
            val data by produceState<WidgetData?>(initialValue = null, tick) {
                val countdownId = WidgetPrefs.get(context, appWidgetId)
                val repository = CountdownRepository(context.applicationContext).apply { load() }
                val settings = SettingsRepository(context).settings.value
                value = WidgetData(
                    countdown = repository.countdowns.value.firstOrNull { it.id == countdownId },
                    now = System.currentTimeMillis(),
                    showDate = settings.showDate,
                    bigEmoji = settings.bigEmoji,
                    unitCount = settings.unitCount,
                )
            }
            GlanceTheme {
                val d = data ?: return@GlanceTheme
                if (d.countdown == null) Missing(context, appWidgetId)
                else Content(context, d.countdown, d.now, d.showDate, d.bigEmoji, d.unitCount)
            }
        }
    }

    private class WidgetData(
        val countdown: Countdown?,
        val now: Long,
        val showDate: Boolean,
        val bigEmoji: Boolean,
        val unitCount: Int,
    )

    companion object {
        private val REFRESH = longPreferencesKey("refresh")

        /** Reload and redraw one widget, e.g. right after its counter was chosen. */
        suspend fun refresh(context: Context, id: GlanceId) {
            updateAppWidgetState(context, id) { it[REFRESH] = System.nanoTime() }
            CountdownWidget().update(context, id)
        }

        /** Reload and redraw all placed widgets, e.g. after a counter was changed. */
        suspend fun refreshAll(context: Context) {
            GlanceAppWidgetManager(context).getGlanceIds(CountdownWidget::class.java).forEach { refresh(context, it) }
        }
    }
}

class CountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WidgetPrefs.remove(context, it) }
    }
}

/** Which counter each placed widget shows. */
object WidgetPrefs {
    private fun prefs(context: Context) = context.getSharedPreferences("widgets", Context.MODE_PRIVATE)
    fun get(context: Context, appWidgetId: Int): Long = prefs(context).getLong("w$appWidgetId", -1L)
    fun set(context: Context, appWidgetId: Int, countdownId: Long) =
        prefs(context).edit().putLong("w$appWidgetId", countdownId).commit()
    fun remove(context: Context, appWidgetId: Int) = prefs(context).edit().remove("w$appWidgetId").apply()
}

/** Same look as a card in the app: badge + title/date on top, the big time units below. */
@Composable
private fun Content(
    context: Context,
    countdown: Countdown,
    now: Long,
    showDate: Boolean,
    bigEmoji: Boolean,
    unitCount: Int,
) {
    val res = context.resources
    val state = countdown.state(now)
    val own = CardColor.of(countdown.color).takeIf { state != DisplayState.Expired }
    val colors = GlanceTheme.colors
    // Same roles as cardPalette() in the app, as day/night-aware providers.
    val container = when {
        own != null -> ColorProvider(day = own.palette(false).container, night = own.palette(true).container)
        state == DisplayState.Expired -> colors.surfaceVariant
        else -> colors.primaryContainer
    }
    val content = when {
        own != null -> ColorProvider(day = own.palette(false).content, night = own.palette(true).content)
        state == DisplayState.Expired -> colors.onSurfaceVariant
        else -> colors.onPrimaryContainer
    }
    val badge = when {
        own != null -> ColorProvider(day = own.palette(false).badge, night = own.palette(true).badge)
        state == DisplayState.Expired -> colors.surface
        else -> colors.primary
    }
    val onBadge = when {
        own != null -> ColorProvider(day = own.palette(false).onBadge, night = own.palette(true).onBadge)
        state == DisplayState.Expired -> colors.onSurface
        else -> colors.onPrimary
    }

    val size = LocalSize.current
    val parts = breakdown(Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()), countdown.target())
    // Narrow widget: drop the smaller unit instead of wrapping or cutting it off.
    // Same "Angezeigte Zeiteinheiten" setting as the app, then whatever fits the width.
    val units = fitUnits(widgetUnits(parts).take(unitCount), availableDp = size.width.value - 2 * PADDING, labelOf = res::unitLabel)
    val roomForDate = showDate && size.height.value >= 120

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(28.dp)
            .background(container)
            .padding(PADDING.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Badge(context, countdown, badge, onBadge, bigEmoji)
            Spacer(GlanceModifier.width(12.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(
                    countdown.name,
                    maxLines = 2,
                    style = TextStyle(color = content, fontSize = 17.sp, fontWeight = FontWeight.Bold),
                )
                if (roomForDate) {
                    Text(countdown.shortDateLine(), maxLines = 1, style = TextStyle(color = content, fontSize = 12.sp))
                }
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        when {
            state == DisplayState.Expired -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Abgelaufen",
                    modifier = GlanceModifier
                        .cornerRadius(8.dp)
                        .background(colors.errorContainer)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    style = TextStyle(color = colors.onErrorContainer, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                )
                Spacer(GlanceModifier.width(8.dp))
                Text(res.agoText(parts), maxLines = 1, style = TextStyle(color = content, fontSize = 14.sp, fontWeight = FontWeight.Medium))
            }
            units.isEmpty() -> Text(
                res.getString(if (countdown.kind == Kind.COUNTUP) R.string.widget_just_started else R.string.widget_almost),
                style = TextStyle(color = content, fontSize = 18.sp, fontWeight = FontWeight.Bold),
            )
            else -> Row(verticalAlignment = Alignment.Bottom) {
                units.forEachIndexed { i, unit ->
                    if (i > 0) Spacer(GlanceModifier.width(UNIT_GAP.dp))
                    Text(unit.value.toString(), style = TextStyle(color = content, fontSize = NUMBER_SP.sp, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.width(3.dp))
                    Text(
                        res.unitLabel(unit),
                        modifier = GlanceModifier.padding(bottom = 5.dp),
                        style = TextStyle(color = content, fontSize = LABEL_SP.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

/**
 * The emoji in the counter's own random shape, like in the app. Glance can't clip to custom shapes,
 * so the shape is a white mask bitmap tinted with the (day/night-aware) badge color.
 */
@Composable
private fun Badge(context: Context, countdown: Countdown, badge: ColorProvider, onBadge: ColorProvider, bigEmoji: Boolean) {
    if (bigEmoji && countdown.emoji != null) {
        Box(GlanceModifier.size(BADGE.dp), contentAlignment = Alignment.Center) {
            Text(countdown.emoji, style = TextStyle(fontSize = 32.sp))
        }
        return
    }
    Box(GlanceModifier.size(BADGE.dp), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(shapeMask(context, countdown.badgeShape())),
            contentDescription = null,
            colorFilter = ColorFilter.tint(badge),
            modifier = GlanceModifier.size(BADGE.dp),
        )
        Text(
            countdown.emoji ?: firstGrapheme(countdown.name).uppercase().ifEmpty { "⏳" },
            style = TextStyle(
                color = onBadge,
                fontSize = if (countdown.emoji != null) 20.sp else 18.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

private fun shapeMask(context: Context, polygon: RoundedPolygon): Bitmap {
    val px = (BADGE * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(1)
    val path = polygon.toPath(Path())
    val bounds = RectF().also { path.computeBounds(it, true) }
    path.transform(Matrix().apply { setRectToRect(bounds, RectF(0f, 0f, px.toFloat(), px.toFloat()), Matrix.ScaleToFit.FILL) })
    return Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888).also { bitmap ->
        Canvas(bitmap).drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE })
    }
}

private const val PADDING = 16f
private const val BADGE = 44f
private const val NUMBER_SP = 32f
private const val LABEL_SP = 14f
private const val UNIT_GAP = 12f

/**
 * Keeps as many units as fit into [availableDp] (always at least one), dropping the smallest first.
 * Widths are estimated from the font sizes – good enough to avoid cut-off text.
 */
internal fun fitUnits(units: List<UnitValue>, availableDp: Float, labelOf: (UnitValue) -> String): List<UnitValue> {
    fun width(u: UnitValue) = u.value.toString().length * NUMBER_SP * 0.6f + 3 + labelOf(u).length * LABEL_SP * 0.58f
    var kept = units
    while (kept.size > 1 && kept.sumOf { width(it).toDouble() } + (kept.size - 1) * UNIT_GAP > availableDp) {
        kept = kept.dropLast(1)
    }
    return kept
}

/** The counter was deleted (or the widget never got one): tap to choose again. */
@Composable
private fun Missing(context: Context, appWidgetId: Int) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .background(GlanceTheme.colors.surfaceVariant)
            .padding(16.dp)
            .clickable(
                actionStartActivity(
                    Intent(context, WidgetConfigActivity::class.java)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                )
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            context.getString(R.string.widget_choose_entry),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 15.sp, fontWeight = FontWeight.Bold),
        )
    }
}

/**
 * Widgets refresh only every 30 minutes, so seconds and minutes would be wrong most of the time:
 * the two largest units down to days, or just the hours on the last day.
 */
internal fun widgetUnits(parts: List<UnitValue>): List<UnitValue> {
    val days = parts.filter { it.unit <= CalendarUnit.DAYS && it.value != 0L }
    val hours = parts.filter { it.unit == CalendarUnit.HOURS && it.value != 0L }
    // Within the last week the hours are worth showing too ("5 Tage · 22 Std.").
    if (days.size == 1 && days[0].unit == CalendarUnit.DAYS) return days + hours
    if (days.isNotEmpty()) return days.take(2)
    return hours
}

/** Compact date for the small widget, e.g. "Do., 19. Nov. · 18:30" (year only when it isn't this year). */
private fun Countdown.shortDateLine(): String =
    shortDate(date) + (time?.let { " · " + shortTime(it) } ?: "")
