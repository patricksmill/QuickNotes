package io.github.patricksmill.quicknotes.view.compose.components

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.patricksmill.quicknotes.R
import io.github.patricksmill.quicknotes.view.compose.theme.QuickNotesTheme

@Composable
fun NotificationPermissionDialog(
    onAllow: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notification_permission_title)) },
        text = { Text(stringResource(R.string.notification_permission_message)) },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text(stringResource(R.string.notification_permission_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.notification_permission_cancel))
            }
        }
    )
}

@Preview
@Composable
private fun NotificationPermissionDialogPreview() {
    QuickNotesTheme {
        NotificationPermissionDialog(onAllow = {}, onDismiss = {})
    }
}
