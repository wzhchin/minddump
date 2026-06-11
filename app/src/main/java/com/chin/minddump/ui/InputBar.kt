package com.chin.minddump.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun InputBar(
    inputText: String,
    isRecording: Boolean,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onRecordClick: () -> Unit,
    onCameraClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Action buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Record button
            IconButton(
                onClick = onRecordClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Filled.StopCircle else Icons.Filled.Mic,
                    contentDescription = if (isRecording) "停止录音" else "录音",
                    tint = if (isRecording) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }

            // Camera button
            IconButton(
                onClick = onCameraClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = "拍照/录像",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Import file button
            IconButton(
                onClick = onImportClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AttachFile,
                    contentDescription = "导入文件",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (isRecording) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "录音中...",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Text input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入想法...") },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                shape = MaterialTheme.shapes.large
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilledIconButton(
                onClick = onSubmit,
                enabled = inputText.isNotBlank(),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "发送",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
