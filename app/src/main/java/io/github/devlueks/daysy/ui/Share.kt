package io.github.devlueks.daysy.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import io.github.devlueks.daysy.R
import io.github.devlueks.daysy.data.Countdown
import io.github.devlueks.daysy.data.Settings
import io.github.devlueks.daysy.data.breakdown
import io.github.devlueks.daysy.data.topUnits
import io.github.devlueks.daysy.ui.theme.ExpressiveFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId

/**
 * The text that goes along with a shared card, e.g.
 *   🎸 Concert
 *   2 weeks, 5 days, 7 hrs to go
 *   Saturday, December 5, 2026 · 8:00 PM
 */
fun Resources.shareText(countdown: Countdown, now: Long): String {
    val parts = breakdown(Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()), countdown.target())
    val units = topUnits(parts).joinToString(", ") { "${it.value} ${unitLabel(it)}" }
    val status = when (countdown.state(now)) {
        DisplayState.Running, DisplayState.NotStarted -> getString(R.string.share_to_go, units)
        DisplayState.CountingUp -> getString(R.string.share_already, units)
        DisplayState.Expired -> getString(R.string.share_ended, agoText(parts))
    }
    return listOfNotNull(
        listOfNotNull(countdown.emoji, countdown.name).joinToString(" "),
        status,
        countdown.dateLine(),
    ).joinToString("\n")
}

/**
 * Preview of the card as it will be shared. "Teilen" captures exactly what is shown
 * (via a graphics layer) and hands image + text to the system share sheet.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShareDialog(countdown: Countdown, settings: Settings, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val res = LocalResources.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    val now by rememberNow()
    val c = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        // Nearly full width, so the card wraps like it does in the list instead of being squeezed.
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(horizontal = 12.dp),
        icon = { Icon(Icons.Rounded.Share, contentDescription = null) },
        title = { Text(stringResource(R.string.share_title)) },
        text = {
            // What gets captured: the card on the app background, with a small "Daysy" signature.
            Column(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraLarge)
                    .drawWithContent {
                        layer.record { this@drawWithContent.drawContent() }
                        drawLayer(layer)
                    }
                    .background(c.surface)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EntryCard(
                    countdown = countdown,
                    state = countdown.state(now),
                    now = now,
                    settings = settings,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "🌼 " + stringResource(R.string.app_name),
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = ExpressiveFont,
                    fontWeight = FontWeight.Bold,
                    color = c.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        val text = res.shareText(countdown, System.currentTimeMillis())
                        val ok = runCatching { shareImage(context, layer.toImageBitmap().asAndroidBitmap(), text) }.isSuccess
                        if (!ok) {
                            Toast.makeText(context, res.getString(R.string.share_failed), Toast.LENGTH_SHORT).show()
                            shareTextOnly(context, text)
                        }
                        onDismiss()
                    }
                },
                shapes = ButtonDefaults.shapes(),
            ) { Text(stringResource(R.string.share)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Saves the image in the app's cache (overwritten each time) and opens the share sheet. */
private suspend fun shareImage(context: Context, bitmap: Bitmap, text: String) {
    val file = withContext(Dispatchers.IO) {
        File(context.cacheDir, "shared").apply { mkdirs() }.resolve("daysy.png").also { f ->
            f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, text)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri(null, uri) // lets the share sheet show the image preview
    context.startActivity(Intent.createChooser(send, context.getString(R.string.share_chooser)))
}

private fun shareTextOnly(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, context.getString(R.string.share_chooser)))
}
