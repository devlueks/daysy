package io.github.devlueks.daysy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.github.devlueks.daysy.R

// Material 3 Expressive default type scale (includes the *Emphasized styles).
val Typography = Typography()

private fun googleSansFlex(weight: Int) = Font(
    resId = R.font.google_sans_flex,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("ROND", 100f), // fully rounded terminals
    ),
)

/**
 * Google Sans Flex (variable font, SIL OFL – see licenses/GoogleSansFlex-OFL.txt)
 * with rounded terminals, used for headlines, card titles and the big numbers.
 */
val ExpressiveFont = FontFamily(
    googleSansFlex(500),
    googleSansFlex(600),
    googleSansFlex(700),
    googleSansFlex(800),
    googleSansFlex(900),
)

val ExpressiveHeadline = ExpressiveFont
