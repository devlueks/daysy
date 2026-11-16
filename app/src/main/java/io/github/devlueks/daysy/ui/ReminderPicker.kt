package io.github.devlueks.daysy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.devlueks.daysy.R
import io.github.devlueks.daysy.data.MAX_REMINDERS
import io.github.devlueks.daysy.data.REMINDER_PRESETS
import io.github.devlueks.daysy.data.normalizeReminders

/**
 * Several reminders at once: the quick choices (15 min, 1 h, 1 day, 1 week) toggle on and off,
 * own times show up as extra selected buttons (tap to remove), "+ Eigene" adds one more.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderChoices(
    selected: List<Long>,
    onChange: (List<Long>) -> Unit,
    onAddCustom: () -> Unit,
) {
    val res = LocalResources.current
    val custom = selected.filter { it !in REMINDER_PRESETS }
    val full = normalizeReminders(selected).size >= MAX_REMINDERS
    // Shortest first reads naturally: 15 min, 1 h, 1 day, … then own ones by length.
    val options = (REMINDER_PRESETS + custom).distinct().sorted()

    Column {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { minutes ->
                val checked = minutes in selected
                val isCustom = minutes !in REMINDER_PRESETS
                ToggleButton(
                    checked = checked,
                    // Adding is blocked once the limit is reached; removing always works.
                    enabled = checked || !full,
                    onCheckedChange = { on -> onChange(if (on) selected + minutes else selected - minutes) },
                    modifier = Modifier.semantics { role = Role.Checkbox },
                    colors = ToggleButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    Text(res.reminderLabel(minutes), maxLines = 1)
                    if (isCustom) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.reminder_remove, res.reminderLabel(minutes)),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = onAddCustom,
                enabled = !full,
                contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.reminder_custom), maxLines = 1)
            }
        }
        if (full) {
            Text(
                stringResource(R.string.reminder_max, MAX_REMINDERS),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
        }
    }
}

/** Number + unit (minutes … weeks) for one own reminder. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CustomReminderDialog(onAdd: (minutes: Long) -> Unit, onDismiss: () -> Unit) {
    val units = listOf(
        R.string.reminder_unit_minutes to 1L,
        R.string.reminder_unit_hours to 60L,
        R.string.reminder_unit_days to 24 * 60L,
        R.string.reminder_unit_weeks to 7 * 24 * 60L,
    )
    var amount by rememberSaveable { mutableStateOf("2") }
    var unit by rememberSaveable { mutableIntStateOf(2) } // days
    val value = amount.toLongOrNull()?.takeIf { it in 1..999 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder_custom_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.reminder_custom_hint), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit).take(3) },
                    label = { Text(stringResource(R.string.reminder_amount)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )
                // Connected button group, like the other single choices in the app.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    units.forEachIndexed { index, (label, _) ->
                        ToggleButton(
                            checked = unit == index,
                            onCheckedChange = { unit = index },
                            modifier = Modifier
                                .weight(1f)
                                .semantics { role = Role.RadioButton },
                            shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                units.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                            contentPadding = PaddingValues(horizontal = 2.dp),
                        ) { Text(stringResource(label), maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { value?.let { onAdd(it * units[unit].second) } },
                enabled = value != null,
            ) { Text(stringResource(R.string.add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
