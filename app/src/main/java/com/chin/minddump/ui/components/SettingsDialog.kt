package com.chin.minddump.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chin.minddump.R
import com.chin.minddump.ui.theme.HapticPattern
import com.chin.minddump.ui.theme.LocalExpressiveShapes
import com.chin.minddump.ui.theme.rememberPremiumHaptics

@Composable
fun SettingsDialog(
    workDir: String,
    onChangeDir: () -> Unit,
    onRebuildDatabase: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberPremiumHaptics()
    val shapes = LocalExpressiveShapes.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = shapes.cardMedium,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.work_directory),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    workDir,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onChangeDir,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.change_directory))
                }

                OutlinedButton(
                    onClick = {
                        haptics.perform(HapticPattern.Tick)
                        onRebuildDatabase()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.rebuild_database))
                }
                Text(
                    stringResource(R.string.rebuild_database_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                haptics.perform(HapticPattern.Tick)
                onDismiss()
            }) {
                Text(stringResource(R.string.done))
            }
        },
    )
}

/**
 * Confirmation + progress overlay for the database rebuild action.
 * Renders an indeterminate spinner with "Rebuilding…" while running, and a
 * confirm/cancel dialog otherwise.
 */
@Composable
fun RebuildDatabaseDialog(
    running: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val shapes = LocalExpressiveShapes.current

    if (running) {
        AlertDialog(
            onDismissRequest = {},
            shape = shapes.cardMedium,
            title = { Text(stringResource(R.string.rebuild_database)) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Text(
                        stringResource(R.string.rebuild_database_running),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            confirmButton = {},
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = shapes.cardMedium,
            title = { Text(stringResource(R.string.rebuild_database_confirm_title)) },
            text = {
                Text(stringResource(R.string.rebuild_database_confirm_message))
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.rebuild))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
