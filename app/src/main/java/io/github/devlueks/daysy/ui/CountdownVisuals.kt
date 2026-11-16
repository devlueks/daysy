package io.github.devlueks.daysy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import io.github.devlueks.daysy.data.Countdown
import io.github.devlueks.daysy.data.Kind
import io.github.devlueks.daysy.ui.theme.ExpressiveFont
import kotlinx.coroutines.delay

/** How an entry is shown right now; decides its tab, colors and texts. */
enum class DisplayState { Running, Expired, CountingUp, NotStarted }

fun Countdown.state(now: Long): DisplayState {
    val past = targetMillis() <= now
    return when (kind) {
        Kind.COUNTDOWN -> when {
            !past -> DisplayState.Running
            autoCountUp -> DisplayState.CountingUp
            else -> DisplayState.Expired
        }
        Kind.COUNTUP -> if (past) DisplayState.CountingUp else DisplayState.NotStarted
    }
}

/** Card colors: role pairs from the system color scheme, or from the counter's own [CardColor]. */
data class CardPalette(
    val container: Color,
    val content: Color,
    val badge: Color,
    val onBadge: Color,
)

/**
 * An own color wins in every state except "Abgelaufen" – expired cards stay muted grey,
 * so they still stand apart from the running ones.
 */
@Composable
fun cardPalette(state: DisplayState, color: String? = null): CardPalette {
    val c = MaterialTheme.colorScheme
    val own = CardColor.of(color)
    if (own != null && state != DisplayState.Expired) return own.palette(isAppDark())
    return when (state) {
        // Countdowns and "Zeit seit" look the same – only expired ones stand apart.
        DisplayState.Running, DisplayState.CountingUp, DisplayState.NotStarted ->
            CardPalette(c.primaryContainer, c.onPrimaryContainer, c.primary, c.onPrimary)
        DisplayState.Expired -> CardPalette(c.surfaceContainer, c.onSurfaceVariant, c.surfaceContainerHighest, c.onSurface)
    }
}

fun Countdown.dateLine(): String = buildString {
    append(fullDate(date))
    time?.let { append(" · ").append(shortTime(it)) }
}

/** Current time in millis, updated exactly on every full second. */
@Composable
fun rememberNow(): State<Long> {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now.longValue = System.currentTimeMillis()
            delay(1000 - now.longValue % 1000)
        }
    }
    return now
}

@Composable
fun Tag(text: String, container: Color, content: Color) {
    Surface(shape = MaterialTheme.shapes.small, color = container, contentColor = content) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EmojiBadge(
    emoji: String?,
    fallback: String,
    color: Color,
    size: Int,
    shape: RoundedPolygon = MaterialShapes.Cookie9Sided,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(shape.toShape())
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji ?: firstGrapheme(fallback).uppercase().ifEmpty { "⏳" },
            fontSize = (size * 0.45f).sp,
            lineHeight = (size * 0.5f).sp,
            fontFamily = if (emoji == null) ExpressiveFont else null,
            fontWeight = if (emoji == null) FontWeight.ExtraBold else null,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
    }
}

/** The emoji of a counter: in its random shape, or large and free-standing ("Emoji ohne Hintergrund"). */
@Composable
fun CounterBadge(
    countdown: Countdown,
    palette: CardPalette,
    size: Int,
    bigEmoji: Boolean,
    modifier: Modifier = Modifier,
) {
    if (bigEmoji && countdown.emoji != null) {
        Box(modifier.size(size.dp), contentAlignment = Alignment.Center) {
            Text(countdown.emoji, fontSize = (size * 0.78f).sp, lineHeight = (size * 0.86f).sp, textAlign = TextAlign.Center)
        }
    } else {
        EmojiBadge(
            emoji = countdown.emoji,
            fallback = countdown.name,
            color = palette.badge,
            contentColor = palette.onBadge,
            size = size,
            shape = countdown.badgeShape(),
            modifier = modifier,
        )
    }
}

