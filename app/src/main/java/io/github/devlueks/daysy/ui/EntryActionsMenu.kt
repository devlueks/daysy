package io.github.devlueks.daysy.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.devlueks.daysy.R

/** One menu row: label, icon, action. */
private class MenuAction(val label: Int, val icon: @Composable () -> Unit, val onClick: () -> Unit)

/**
 * The actions of one entry – used by the editor's "more" button and by a long press on a card,
 * so both always offer the same things. Pass null for an action that isn't available.
 * Each action closes the menu before it runs.
 *
 * M3 Expressive menu: rounded items in grouped "pills"; the destructive action sits in a group of
 * its own, a little apart; "pin" is a toggle whose item morphs into the selected shape.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EntryActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    pinned: Boolean,
    onTogglePin: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onReset: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    fun run(action: () -> Unit) = { onDismiss(); action() }
    val error = MaterialTheme.colorScheme.error

    // Main group: edit, pin, share, reset (whatever is available), in this order.
    val before = listOfNotNull(
        onEdit?.let { MenuAction(R.string.editor_edit, { Icon(Icons.Rounded.Edit, null) }, run(it)) },
    )
    val after = listOfNotNull(
        onShare?.let { MenuAction(R.string.share, { Icon(Icons.Rounded.Share, null) }, run(it)) },
        onReset?.let { MenuAction(R.string.reset, { Icon(Icons.Rounded.Refresh, null) }, run(it)) },
    )
    val count = before.size + 1 + after.size

    DropdownMenuPopup(expanded = expanded, onDismissRequest = onDismiss) {
        // Two raised groups on a light neutral surface (the vibrant tertiary tone clashed with the app colors).
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(0, if (onDelete != null) 2 else 1),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = 6.dp,
        ) {
            before.forEachIndexed { i, a ->
                DropdownMenuItem(
                    text = { Text(stringResource(a.label)) },
                    shape = MenuDefaults.itemShape(i, count).shape,
                    leadingIcon = a.icon,
                    onClick = a.onClick,
                )
            }
            // A toggle shown like the other rows; a check mark (in the accent color) while pinned.
            DropdownMenuItem(
                text = { Text(stringResource(R.string.pin)) },
                shape = MenuDefaults.itemShape(before.size, count).shape,
                leadingIcon = { Icon(painterResource(R.drawable.ic_pin), contentDescription = null) },
                trailingContent = {
                    if (pinned) Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.pinned), tint = MaterialTheme.colorScheme.primary)
                },
                onClick = run(onTogglePin),
            )
            after.forEachIndexed { i, a ->
                DropdownMenuItem(
                    text = { Text(stringResource(a.label)) },
                    shape = MenuDefaults.itemShape(before.size + 1 + i, count).shape,
                    leadingIcon = a.icon,
                    onClick = a.onClick,
                )
            }
        }
        if (onDelete != null) {
            Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(1, 2),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = 6.dp,
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete), color = error) },
                    shape = MenuDefaults.itemShape(0, 1).shape,
                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = error) },
                    onClick = run(onDelete),
                )
            }
        }
    }
}

/** Asks before a "time since" entry starts again at 0 (from the editor or the long-press menu). */
@Composable
fun ResetDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
        title = { Text(stringResource(R.string.reset_title)) },
        text = { Text(stringResource(R.string.reset_text, name.trim())) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.reset)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
