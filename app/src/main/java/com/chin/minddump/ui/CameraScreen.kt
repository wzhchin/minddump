package com.chin.minddump.ui

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.chin.minddump.R
import com.chin.minddump.camera.CameraManager
import com.chin.minddump.ui.theme.HapticPattern
import com.chin.minddump.ui.theme.LocalAnimationDuration
import com.chin.minddump.ui.theme.LocalExpressiveShapes
import com.chin.minddump.ui.theme.LocalMotionCurve
import com.chin.minddump.ui.theme.rememberPremiumHaptics

@Composable
fun CameraScreen(
    cameraManager: CameraManager,
    onClose: () -> Unit,
    onCaptured: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = rememberPremiumHaptics()
    val shapes = LocalExpressiveShapes.current
    val animDuration = LocalAnimationDuration.current
    val curve = LocalMotionCurve.current.decelerate

    var isVideoMode by remember { mutableStateOf(false) }
    var isRecordingVideo by remember { mutableStateOf(false) }
    // Flash is a UI-only toggle for now; CameraManager does not expose flash
    // control. Visual state is kept locally so the control responds, but it does
    // not yet drive the actual capture flash (would need a CameraManager API).
    var flashOn by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // ── Camera preview ──
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx)
                    .apply {
                        layoutParams =
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }.also { previewView ->
                        cameraManager.startPreview(previewView, lifecycleOwner)
                    }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // ── Top scrim + chrome (close, flash) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent,
                        ),
                    ),
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Close
                ChromeButton(onClick = {
                    cameraManager.stopPreview()
                    onClose()
                }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = Color.White,
                    )
                }

                // Flash toggle (UI-only; see flashOn TODO)
                ChromeButton(onClick = {
                    flashOn = !flashOn
                    haptics.perform(HapticPattern.Tick)
                }) {
                    Icon(
                        imageVector =
                            if (flashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = stringResource(R.string.flash),
                        tint = if (flashOn) Color.Yellow else Color.White,
                    )
                }
            }
        }

        // ── Bottom scrim + controls ──
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f),
                        ),
                    ),
                ).padding(bottom = 32.dp, top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Recording timer (only while recording) ──
            RecordingTimer(visible = isRecordingVideo)

            Spacer(modifier = Modifier.height(16.dp))

            // ── Mode segmented control (Photo / Video) ──
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = !isVideoMode,
                    onClick = {
                        if (isVideoMode) {
                            haptics.perform(HapticPattern.Tick)
                            isVideoMode = false
                            if (isRecordingVideo) {
                                cameraManager.stopVideoRecording()
                            }
                            isRecordingVideo = false
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    icon = {
                        SegmentedButtonDefaults.Icon(active = !isVideoMode) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                        }
                    },
                ) {
                    Text(stringResource(R.string.photo_mode))
                }
                SegmentedButton(
                    selected = isVideoMode,
                    onClick = {
                        if (!isVideoMode) {
                            haptics.perform(HapticPattern.Tick)
                            isVideoMode = true
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    icon = {
                        SegmentedButtonDefaults.Icon(active = isVideoMode) {
                            Icon(Icons.Filled.Videocam, contentDescription = null)
                        }
                    },
                ) {
                    Text(stringResource(R.string.video_mode))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Shutter + flanking actions ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Spacer + switch camera (kept on the left for thumb reach)
                ChromeButton(onClick = {
                    haptics.perform(HapticPattern.Tick)
                    cameraManager.switchCamera()
                }) {
                    Icon(
                        imageVector = Icons.Filled.Cameraswitch,
                        contentDescription = stringResource(R.string.switch_camera),
                        tint = Color.White,
                    )
                }

                // Shutter
                ShutterButton(
                    isVideoMode = isVideoMode,
                    isRecording = isRecordingVideo,
                    shape = shapes.buttonPill,
                    animDuration = animDuration.medium,
                    curve = curve,
                    onClick = {
                        haptics.perform(HapticPattern.Pop)
                        if (isVideoMode) {
                            if (isRecordingVideo) {
                                cameraManager.stopVideoRecording()
                                isRecordingVideo = false
                                cameraManager.stopPreview()
                                onCaptured()
                            } else {
                                cameraManager.startVideoRecording(context, lifecycleOwner) {
                                    isRecordingVideo = false
                                    cameraManager.stopPreview()
                                    onCaptured()
                                }
                                isRecordingVideo = true
                            }
                        } else {
                            cameraManager.takePhoto(context) {
                                cameraManager.stopPreview()
                                onCaptured()
                            }
                        }
                    },
                )

                // Balance the row symmetrically
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    }
}

// ──────────────────────────────────────────────
// Chrome: scrimmed circular icon button
// ──────────────────────────────────────────────

/**
 * A circular icon button sitting on the camera scrim: translucent dark disc with a
 * crisp white icon. Used for close / flash / switch-camera.
 */
@Composable
private fun ChromeButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick) { content() }
    }
}

// ──────────────────────────────────────────────
// Recording timer
// ──────────────────────────────────────────────

/**
 * Live `mm:ss` elapsed counter, shown only while recording. Ticks once per second
 * via [produceState] keyed on [visible]; resets to 00:00 each time recording starts.
 */
@Composable
private fun RecordingTimer(visible: Boolean) {
    val elapsedSec by produceState(initialValue = 0, key1 = visible) {
        if (!visible) {
            value = 0
            return@produceState
        }
        val start = System.currentTimeMillis()
        while (true) {
            value = ((System.currentTimeMillis() - start) / 1000).toInt()
            kotlinx.coroutines.delay(1000)
        }
    }

    AnimatedVisibility(visible = visible) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.Red),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatElapsed(elapsedSec),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private fun formatElapsed(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}

// ──────────────────────────────────────────────
// Shutter button
// ──────────────────────────────────────────────

/**
 * The pill/circle shutter. Photo mode shows a solid white disc with a camera glyph;
 * video idle shows a red ring; recording collapses the inner disc and shows a stop
 * glyph, with the ring animating to convey the active recording state.
 */
@Composable
private fun ShutterButton(
    isVideoMode: Boolean,
    isRecording: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    animDuration: Int,
    curve: androidx.compose.animation.core.Easing,
    onClick: () -> Unit,
) {
    val ringColor = if (isVideoMode) Color.Red else MaterialTheme.colorScheme.primary
    val innerScale by animateFloatAsState(
        targetValue = if (isRecording) 0.55f else 0.82f,
        animationSpec = tween(durationMillis = animDuration, easing = curve),
        label = "shutterInner",
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (isRecording) 1f else 0.85f,
        animationSpec = tween(durationMillis = animDuration, easing = curve),
        label = "shutterRing",
    )

    val icon = when {
        isRecording -> Icons.Filled.Stop
        isVideoMode -> Icons.Filled.Videocam
        else -> Icons.Filled.PhotoCamera
    }
    val description = when {
        isVideoMode -> stringResource(R.string.record_video)
        else -> stringResource(R.string.take_photo)
    }

    Box(
        modifier = Modifier
            .size(78.dp)
            .clip(shape)
            .background(ringColor.copy(alpha = ringAlpha)),
        contentAlignment = Alignment.Center,
    ) {
        // Inner disc that scales down while recording (record-button feel)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(innerScale)
                .clip(shape)
                .background(Color.White)
                .border(width = 2.dp, color = ringColor, shape = shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = ringColor,
                modifier = Modifier.size(if (isVideoMode) 28.dp else 30.dp),
            )
        }
        // Tap target spanning the whole control
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {}
    }
}
