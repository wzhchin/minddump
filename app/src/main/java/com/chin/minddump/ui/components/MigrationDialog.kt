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
import com.chin.minddump.ui.theme.HapticPattern
import com.chin.minddump.ui.theme.LocalExpressiveShapes
import com.chin.minddump.ui.theme.rememberPremiumHaptics

@Composable
fun MigrationDialog(
    currentFileCount: Int,
    newDirFileCount: Int,
    onConfirm: () -> Unit,
    onSkip: () -> Unit,
    onCancel: () -> Unit,
) {
    val haptics = rememberPremiumHaptics()
    val shapes = LocalExpressiveShapes.current

    AlertDialog(
        onDismissRequest = onCancel,
        shape = shapes.cardMedium,
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
            TextButton(onClick = {
                haptics.perform(HapticPattern.Tick)
                onConfirm()
            }) {
                Text(stringResource(R.string.move))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    haptics.perform(HapticPattern.Cancel)
                    onCancel()
                }) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(onClick = {
                    haptics.perform(HapticPattern.Tick)
                    onSkip()
                }) {
                    Text(stringResource(R.string.skip))
                }
            }
        },
    )
}
