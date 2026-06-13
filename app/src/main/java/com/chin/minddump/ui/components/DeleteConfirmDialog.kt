package com.chin.minddump.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.ui.theme.HapticPattern
import com.chin.minddump.ui.theme.LocalExpressiveShapes
import com.chin.minddump.ui.theme.rememberPremiumHaptics

@Composable
fun DeleteConfirmDialog(
    entry: MindDumpEntry,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberPremiumHaptics()
    val shapes = LocalExpressiveShapes.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = shapes.cardMedium,
        title = { Text("删除记录") },
        text = { Text("确定要删除 \"${entry.file.name}\" 吗？") },
        confirmButton = {
            TextButton(onClick = {
                haptics.perform(HapticPattern.Thud)
                onConfirm()
            }) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                haptics.perform(HapticPattern.Cancel)
                onDismiss()
            }) {
                Text("取消")
            }
        },
    )
}
