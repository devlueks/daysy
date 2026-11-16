package io.github.devlueks.daysy.ui

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import io.github.devlueks.daysy.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Own colors a counter can use instead of the system colors. Each one is a small tonal
 * palette (like the system's own roles) with fitting light and dark variants.
 * [key] is persisted – don't rename.
 */
enum class CardColor(val key: String, @StringRes val labelRes: Int, hue: Float, saturation: Float) {
    RED("rot", R.string.color_red, 355f, 0.62f),
    ORANGE("orange", R.string.color_orange, 24f, 0.70f),
    YELLOW("gelb", R.string.color_yellow, 46f, 0.72f),
    GREEN("gruen", R.string.color_green, 125f, 0.40f),
    MINT("mint", R.string.color_mint, 162f, 0.45f),
    TEAL("tuerkis", R.string.color_teal, 188f, 0.55f),
    BLUE("blau", R.string.color_blue, 212f, 0.62f),
    INDIGO("indigo", R.string.color_indigo, 238f, 0.50f),
    PURPLE("lila", R.string.color_purple, 272f, 0.48f),
    PINK("pink", R.string.color_pink, 325f, 0.55f),
    GRAY("grau", R.string.color_gray, 220f, 0.08f),
    ;

    // Fairly vivid on purpose: an own color should clearly stand out from the calmer system colors.
    // Text stays dark-on-light / light-on-dark, so it remains easy to read.
    private val light = CardPalette(
        container = Color.hsl(hue, (saturation * 1.3f).coerceAtMost(0.95f), 0.80f),
        content = Color.hsl(hue, saturation.coerceAtMost(0.6f), 0.13f),
        badge = Color.hsl(hue, saturation, 0.38f),
        onBadge = Color.White,
    )
    private val dark = CardPalette(
        container = Color.hsl(hue, (saturation * 1.05f).coerceAtMost(0.9f), 0.30f),
        content = Color.hsl(hue, saturation, 0.92f),
        badge = Color.hsl(hue, saturation, 0.76f),
        onBadge = Color.hsl(hue, saturation * 0.7f, 0.14f),
    )

    /** Swatch shown in the picker. */
    val swatch: Color get() = light.badge

    fun palette(dark: Boolean) = if (dark) this.dark else light

    companion object {
        fun of(key: String?): CardColor? = entries.firstOrNull { it.key == key }
    }
}

/** True when the app currently shows its dark theme (which may differ from the system setting). */
@Composable
fun isAppDark(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f
