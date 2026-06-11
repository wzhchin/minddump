package com.chin.minddump.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Wrapper around MediaRecorder for M4A/AAC recording.
 */
class AudioRecorder {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(context: Context, outputFile: File) {
        this.outputFile = outputFile

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
    }

    fun stop() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
            // Recording may fail if too short
            outputFile?.let { if (it.exists()) it.delete() }
        }
        recorder = null
        outputFile = null
    }

    fun isRecording(): Boolean = recorder != null
}
