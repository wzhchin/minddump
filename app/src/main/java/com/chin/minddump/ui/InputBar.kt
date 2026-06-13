package com.chin.minddump.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.chin.minddump.storage.Space
import com.chin.minddump.ui.theme.HapticPattern
import com.chin.minddump.ui.theme.LocalAnimationDuration
import com.chin.minddump.ui.theme.LocalExpressiveShapes
import com.chin.minddump.ui.theme.LocalMotionCurve
import com.chin.minddump.ui.theme.rememberPremiumHaptics
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

/**
 * Input bar visual states for animation transitions.
 */
sealed interface InputBarState {
    data object Collapsed : InputBarState
    data object Expanded : InputBarState
    data object Recording : InputBarState
    data object Sending : InputBarState
}

@Suppress("LongMethod")
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
    val animDuration = LocalAnimationDuration.current
    val curve = LocalMotionCurve.current
    val shapes = LocalExpressiveShapes.current
    val haptics = rememberPremiumHaptics()

    // Transition-driven colors
    val recordContainerColor by animateColorAsState(
        targetValue = if (isRecording) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(animDuration.medium),
        label = "record_container_color",
    )

    val recordContentColor by animateColorAsState(
        targetValue = if (isRecording) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(animDuration.medium),
        label = "record_content_color",
    )

    // Send button scale bounce on text change
    var sendScale by remember { mutableFloatStateOf(1f) }
    val sendScaleAnimated by animateFloatAsState(
        targetValue = sendScale,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
        label = "send_bounce",
    )
    // Trigger bounce when text changes
    if (inputText.isNotBlank()) {
        sendScale = 1.05f
    } else {
        sendScale = 1f
    }

    // Space switch Y rotation
    var spaceRotation by remember { mutableFloatStateOf(0f) }
    val spaceRotationAnimated by animateFloatAsState(
        targetValue = spaceRotation,
        animationSpec = tween(animDuration.medium, easing = curve.standard),
        label = "space_rotation",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = backgroundTheme.tonalElevation,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
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
                // Recording button with transition-driven colors
                FilledTonalIconButton(
                    onClick = {
                        haptics.perform(HapticPattern.Pop)
                        onRecordClick()
                    },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = recordContainerColor,
                        contentColor = recordContentColor,
                    ),
                ) {
                    AnimatedContent(
                        targetState = isRecording,
                        transitionSpec = {
                            fadeIn(tween(animDuration.short)) togetherWith
                                fadeOut(tween(animDuration.short))
                        },
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
                AnimatedVisibility(
                    visible = !isRecording,
                    enter = fadeIn(tween(animDuration.short)) + expandVertically(),
                    exit = fadeOut(tween(animDuration.short)) + shrinkVertically(),
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            haptics.perform(HapticPattern.Tick)
                            onCameraClick()
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Icon(
                            Icons.Filled.PhotoCamera,
                            contentDescription = stringResource(R.string.camera_capture),
                        )
                    }
                }

                // Import
                AnimatedVisibility(
                    visible = !isRecording,
                    enter = fadeIn(tween(animDuration.short)) + expandVertically(),
                    exit = fadeOut(tween(animDuration.short)) + shrinkVertically(),
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            haptics.perform(HapticPattern.Tick)
                            onImportClick()
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Icon(
                            Icons.Filled.AttachFile,
                            contentDescription = stringResource(R.string.import_file),
                        )
                    }
                }

                // Recording indicator with pulse
                AnimatedVisibility(
                    visible = isRecording,
                    enter = fadeIn(tween(animDuration.short)) + expandVertically(),
                    exit = fadeOut(tween(animDuration.short)) + shrinkVertically(),
                ) {
                    RecordingIndicatorChip()
                }

                Spacer(modifier = Modifier.weight(1f))

                // Space switch with rotation animation
                SpaceSwitchButton(
                    currentSpace = currentSpace,
                    onClick = {
                        spaceRotation += 180f
                        onSpaceToggle()
                    },
                    rotationY = spaceRotationAnimated,
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
                    shape = shapes.inputField,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    ),
                )

                // Fullscreen expand button
                AnimatedVisibility(
                    visible = inputText.isNotBlank(),
                    enter = fadeIn(tween(animDuration.short)) + expandVertically(),
                    exit = fadeOut(tween(animDuration.short)) + shrinkVertically(),
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            haptics.perform(HapticPattern.Tick)
                            onFullscreenClick()
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
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

                // Send button with bounce animation
                FilledIconButton(
                    onClick = {
                        haptics.perform(HapticPattern.Send)
                        onSubmit()
                    },
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier.graphicsLayer {
                        scaleX = sendScaleAnimated
                        scaleY = sendScaleAnimated
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
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

/**
 * Recording indicator chip with a pulsing red dot.
 */
@Composable
private fun RecordingIndicatorChip() {
    val animDuration = LocalAnimationDuration.current
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_dot",
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

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
            Box(
                modifier = Modifier
                    .size(8.dp * pulseScale)
                    .clip(CircleShape)
                    .graphicsLayer { alpha = pulseAlpha }
                    .background(MaterialTheme.colorScheme.error),
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.error,
        ),
    )
}
