package io.github.devlueks.daysy.ui

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.graphics.shapes.RoundedPolygon
import io.github.devlueks.daysy.data.Countdown
import kotlin.math.absoluteValue
import kotlin.random.Random

/** Material 3 Expressive shapes an emoji badge can get. Order is persisted – only append. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val BadgeShapes: List<RoundedPolygon> = listOf(
    MaterialShapes.Cookie9Sided,
    MaterialShapes.Clover4Leaf,
    MaterialShapes.Sunny,
    MaterialShapes.VerySunny,
    MaterialShapes.Cookie4Sided,
    MaterialShapes.Cookie6Sided,
    MaterialShapes.Cookie7Sided,
    MaterialShapes.Cookie12Sided,
    MaterialShapes.Clover8Leaf,
    MaterialShapes.SoftBurst,
    MaterialShapes.Flower,
    MaterialShapes.Puffy,
    MaterialShapes.PuffyDiamond,
    MaterialShapes.Pentagon,
    MaterialShapes.Gem,
    MaterialShapes.Ghostish,
    MaterialShapes.Bun,
    MaterialShapes.Heart,
    MaterialShapes.Arch,
    MaterialShapes.Slanted,
    MaterialShapes.Circle,
)

fun randomBadgeShape(): Int = Random.nextInt(BadgeShapes.size)

/** Stored shape, or – for entries created before shapes existed – a stable pick based on the id. */
fun Countdown.badgeShape(): RoundedPolygon =
    BadgeShapes[if (shape in BadgeShapes.indices) shape else (id.hashCode().absoluteValue % BadgeShapes.size)]
