package com.chin.minddump.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.chin.minddump.storage.Space

@Composable
fun InputBar(
    inputText: String,
    isRecording: Boolean,
    currentSpace: Space,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onRecordClick: () -> Unit,
    onCameraClick: () -> Unit,
    onImportClick: () -> Unit,
    onSpaceToggle: () -> Unit,
    onFullscreenClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val backgroundTheme = LocalBackgroundTheme.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = backgroundTheme.tonalElevation,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Action tonal icon buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Recording — tonal button with animated content
                FilledTonalIconButton(
                    onClick = onRecordClick,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (isRecording) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        contentColor = if (isRecording) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                ) {
                    AnimatedContent(
                        targetState = isRecording,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                        label = "record_icon"
                    ) { recording ->
                        if (recording) {
                            Icon(
                                Icons.Filled.StopCircle,
                                contentDescription = "停止录音"
                            )
                        } else {
                            Icon(
                                Icons.Filled.Mic,
                                contentDescription = "录音"
                            )
                        }
                    }
                }

                // Camera
                FilledTonalIconButton(
                    onClick = onCameraClick,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Filled.PhotoCamera,
                        contentDescription = "拍照/录像"
                    )
                }

                // Import
                FilledTonalIconButton(
                    onClick = onImportClick,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Filled.AttachFile,
                        contentDescription = "导入文件"
                    )
                }

                // Recording indicator chip
                if (isRecording) {
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = {
                            Text(
                                "录音中...",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Circle,
                                contentDescription = null,
                                modifier = Modifier.size(8.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.error
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Space switch
                SpaceSwitchButton(
                    currentSpace = currentSpace,
                    onClick = onSpaceToggle
                )
            }

            // Row 2: Input field + send button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("输入想法...")
                    },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    )
                )

                // Fullscreen expand button — only visible when text is entered
                if (inputText.isNotBlank()) {
                    FilledTonalIconButton(
                        onClick = onFullscreenClick,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            Icons.Filled.OpenInFull,
                            contentDescription = "全屏编辑",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                FilledIconButton(
                    onClick = onSubmit,
                    enabled = inputText.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
