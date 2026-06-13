package com.chin.minddump.ui

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.chin.minddump.R
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.chin.minddump.camera.CameraManager

@Composable
fun CameraScreen(
    cameraManager: CameraManager,
    onClose: () -> Unit,
    onCaptured: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isVideoMode by remember { mutableStateOf(false) }
    var isRecordingVideo by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Camera preview
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

        // Controls overlay
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Mode switch
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = !isVideoMode,
                    onClick = {
                        isVideoMode = false
                        isRecordingVideo = false
                    },
                    label = { Text(stringResource(R.string.photo_mode)) },
                )
                FilterChip(
                    selected = isVideoMode,
                    onClick = { isVideoMode = true },
                    label = { Text(stringResource(R.string.video_mode)) },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Capture button
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Close
                IconButton(onClick = {
                    cameraManager.stopPreview()
                    onClose()
                }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close),
                        modifier = Modifier.size(32.dp),
                    )
                }

                // Capture / Record
                FilledIconButton(
                    onClick = {
                        if (isVideoMode) {
                            if (isRecordingVideo) {
                                cameraManager.stopVideoRecording()
                                isRecordingVideo = false
                                cameraManager.stopPreview()
                                onCaptured()
                            } else {
                                cameraManager.startVideoRecording(context, lifecycleOwner) {
                                    // onVideoSaved
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
                    modifier = Modifier.size(64.dp),
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor =
                                if (isRecordingVideo) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                        ),
                ) {
                    Icon(
                        imageVector =
                            if (isVideoMode && isRecordingVideo) {
                                Icons.Filled.Stop
                            } else {
                                Icons.Filled.Camera
                            },
                        contentDescription =
                            if (isVideoMode) {
                                stringResource(R.string.record_video)
                            } else {
                                stringResource(R.string.take_photo)
                            },
                        modifier = Modifier.size(32.dp),
                    )
                }

                // Switch camera
                IconButton(onClick = { cameraManager.switchCamera() }) {
                    Icon(
                        imageVector = Icons.Filled.Cameraswitch,
                        contentDescription = stringResource(R.string.switch_camera),
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}
