package com.chin.minddump.ui

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.chin.minddump.storage.Space
import com.chin.minddump.R

/**
 * Callbacks for InputBar actions.
 */
data class InputBarActions(
    val onInputChange: (String) -> Unit,
    val onSubmit: () -> Unit,
    val onRecordClick: () -> Unit,
    val onCameraClick: () -> Unit,
    val onImportClick: () -> Unit,
    val onSpaceToggle: () -> Unit,
    val onFullscreenClick: () -> Unit = {},
)

@Composable
fun InputBar(
    inputText: String,
    isRecording: Boolean,
    currentSpace: Space,
    actions: InputBarActions,
    modifier: Modifier = Modifier,
) {
    val onInputChange = actions.onInputChange
    val onSubmit = actions.onSubmit
    val onRecordClick = actions.onRecordClick
    val onCameraClick = actions.onCameraClick
    val onImportClick = actions.onImportClick
    val onSpaceToggle = actions.onSpaceToggle
    val onFullscreenClick = actions.onFullscreenClick
    val backgroundTheme = LocalBackgroundTheme.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = backgroundTheme.tonalElevation,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .imePadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Row 1: Action tonal icon buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Recording — tonal button with animated content
                FilledTonalIconButton(
                    onClick = onRecordClick,
                    colors =
                        IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor =
                                if (isRecording) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                            contentColor =
                                if (isRecording) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        ),
                ) {
                    AnimatedContent(
                        targetState = isRecording,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                        label = "record_icon",
                    ) { recording ->
                        if (recording) {
                            Icon(
                                Icons.Filled.StopCircle,
                                contentDescription = stringResource(R.string.stop_recording),
                            )
                        } else {
                            Icon(
                                Icons.Filled.Mic,
                                contentDescription = stringResource(R.string.start_recording),
                            )
                        }
                    }
                }

                // Camera
                FilledTonalIconButton(
                    onClick = onCameraClick,
                    colors =
                        IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                ) {
                    Icon(
                        Icons.Filled.PhotoCamera,
                        contentDescription = stringResource(R.string.camera_capture),
                    )
                }

                // Import
                FilledTonalIconButton(
                    onClick = onImportClick,
                    colors =
                        IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                ) {
                    Icon(
                        Icons.Filled.AttachFile,
                        contentDescription = stringResource(R.string.import_file),
                    )
                }

                // Recording indicator chip
                if (isRecording) {
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = {
                            Text(
                                stringResource(R.string.recording),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Circle,
                                contentDescription = null,
                                modifier = Modifier.size(8.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.error,
                            ),
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Space switch
                SpaceSwitchButton(
                    currentSpace = currentSpace,
                    onClick = onSpaceToggle,
                )
            }

            // Row 2: Input field + send button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(stringResource(R.string.input_placeholder))
                    },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        ),
                )

                // Fullscreen expand button — only visible when text is entered
                if (inputText.isNotBlank()) {
                    FilledTonalIconButton(
                        onClick = onFullscreenClick,
                        colors =
                            IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                    ) {
                        Icon(
                            Icons.Filled.OpenInFull,
                            contentDescription = stringResource(R.string.fullscreen_edit),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                FilledIconButton(
                    onClick = onSubmit,
                    enabled = inputText.isNotBlank(),
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        ),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.send),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
