package com.chin.minddump.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chin.minddump.R
import com.chin.minddump.storage.ShareItem
import com.chin.minddump.ui.theme.HapticPattern
import com.chin.minddump.ui.theme.LocalExpressiveShapes
import com.chin.minddump.ui.theme.rememberPremiumHaptics

@Composable
fun SpaceSelectionDialog(
    items: List<ShareItem>,
    onPublicSelected: () -> Unit,
    onPrivateSelected: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberPremiumHaptics()
    val shapes = LocalExpressiveShapes.current

    val summary = summarizeShareItems(items)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = shapes.cardMedium,
        title = { Text(stringResource(R.string.share_title)) },
        text = { Text(summary) },
        confirmButton = {
            TextButton(onClick = {
                haptics.perform(HapticPattern.Tick)
                onPublicSelected()
            }) {
                Text(stringResource(R.string.share_to_public))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                haptics.perform(HapticPattern.Tick)
                onPrivateSelected()
            }) {
                Text(stringResource(R.string.share_to_private))
            }
        },
    )
}

@Composable
private fun summarizeShareItems(items: List<ShareItem>): String {
    val textCount = items.count { it is ShareItem.Text }
    val fileCount = items.count { it is ShareItem.File }
    val total = items.size

    return when {
        textCount == 1 && fileCount == 0 -> stringResource(R.string.share_summary_text)
        fileCount == 1 && textCount == 0 -> stringResource(R.string.share_summary_file)
        textCount == 0 && fileCount > 1 -> stringResource(R.string.share_summary_files, fileCount)
        else -> stringResource(R.string.share_summary_mixed, total)
    }
}
