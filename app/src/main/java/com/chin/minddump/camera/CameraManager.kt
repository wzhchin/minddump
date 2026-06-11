package com.chin.minddump.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.Executors

/**
 * Manages CameraX operations: preview, photo capture, video recording.
 */
class CameraManager {

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var currentRecording: Recording? = null
    private var photoFile: File? = null
    private var videoFile: File? = null
    private var cachedLifecycleOwner: LifecycleOwner? = null

    private val executor = Executors.newSingleThreadExecutor()

    /**
     * Set the output files for capture operations.
     */
    fun setOutputFiles(photo: File, video: File) {
        photoFile = photo
        videoFile = video
    }

    /**
     * Start camera preview.
     */
    fun startPreview(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        val context = previewView.context
        cachedLifecycleOwner = lifecycleOwner

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            cameraProvider = future.get()

            preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            bindUseCases(lifecycleOwner)
        }, context.mainExecutor)
    }

    private fun bindUseCases(lifecycleOwner: LifecycleOwner) {
        cameraProvider?.unbindAll()
        try {
            cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                videoCapture
            )
        } catch (e: Exception) {
            // Some devices may not support video capture with this selector, bind without it
            cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        }
    }

    /**
     * Switch between front and back camera.
     */
    fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        cachedLifecycleOwner?.let { bindUseCases(it) }
    }

    /**
     * Take a photo.
     */
    fun takePhoto(context: Context, onSaved: () -> Unit) {
        val file = photoFile ?: return
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture?.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onSaved()
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraManager", "Photo capture failed", exc)
                }
            }
        )
    }

    /**
     * Start video recording.
     */
    fun startVideoRecording(context: Context, lifecycleOwner: LifecycleOwner, onSaved: () -> Unit) {
        val file = videoFile ?: return
        val recorder = videoCapture?.output ?: return

        val fileOutputOptions = FileOutputOptions.Builder(file).build()

        currentRecording = recorder
            .prepareRecording(context, fileOutputOptions)
            .start(executor) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            onSaved()
                        } else {
                            Log.e(
                                "CameraManager",
                                "Video recording failed: ${recordEvent.error}"
                            )
                        }
                    }
                }
            }
    }

    /**
     * Stop video recording.
     */
    fun stopVideoRecording() {
        currentRecording?.stop()
        currentRecording = null
    }

    /**
     * Stop camera and release resources.
     */
    fun stopPreview() {
        cameraProvider?.unbindAll()
    }
}
