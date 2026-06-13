package com.chin.minddump.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Confirmation shown when closing the editor with unsaved edits. Offers three
 * explicit actions so a discard is always deliberate, never silent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnsavedEditSheet(
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun close(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "保留这次编辑吗？",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(
                onClick = { close(onSave) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存", fontWeight = FontWeight.SemiBold)
            }
            TextButton(
                onClick = { close(onDiscard) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("丢弃")
            }
            TextButton(
                onClick = { close(onKeepEditing) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("继续编辑")
            }
        }
    }
}
