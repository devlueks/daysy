package io.github.devlueks.daysy.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.devlueks.daysy.ui.theme.ExpressiveFont

/** Snackbar content with an optional leading icon (e.g. a bin for "gelöscht"). */
class IconSnackbarVisuals(
    override val message: String,
    val icon: ImageVector? = null,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Long,
) : SnackbarVisuals

/**
 * M3 Expressive snackbar: a floating, fully rounded pill on the inverse surface, the icon in a
 * soft cookie shape, and the action as a real (shape-morphing) button instead of plain text.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveSnackbar(data: SnackbarData) {
    val c = MaterialTheme.colorScheme
    val visuals = data.visuals
    val icon = (visuals as? IconSnackbarVisuals)?.icon
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = c.inverseSurface,
        contentColor = c.inverseOnSurface,
        shadowElevation = 6.dp,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .widthIn(max = 560.dp)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = if (icon != null) 10.dp else 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Surface(
                    shape = MaterialShapes.Cookie6Sided.toShape(),
                    color = c.inversePrimary,
                    contentColor = c.onPrimaryContainer,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = visuals.message,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = ExpressiveFont,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
            )
            visuals.actionLabel?.let { label ->
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = data::performAction,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(containerColor = c.inversePrimary, contentColor = c.onPrimaryContainer),
                ) { Text(label) }
            }
            if (visuals.withDismissAction) {
                IconButton(
                    onClick = data::dismiss,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = c.inverseOnSurface),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Schließen")
                }
            }
        }
    }
}
