package com.chin.minddump.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chin.minddump.R

@Composable
fun MigrationDialog(
    currentFileCount: Int,
    newDirFileCount: Int,
    onConfirm: () -> Unit,
    onSkip: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.migration_title)) },
        text = {
            val lines = mutableListOf<String>()
            if (currentFileCount > 0) {
                lines.add(stringResource(R.string.migration_current_files, currentFileCount))
            }
            if (newDirFileCount > 0) {
                lines.add(stringResource(R.string.migration_new_files, newDirFileCount))
            }
            lines.add(stringResource(R.string.migration_ask))
            Text(lines.joinToString("\n"))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.move))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(onClick = onSkip) {
                    Text(stringResource(R.string.skip))
                }
            }
        },
    )
}
