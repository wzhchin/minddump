package com.chin.minddump

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import com.chin.minddump.audio.AudioRecorder
import com.chin.minddump.camera.CameraManager
import com.chin.minddump.ui.MindDumpNavGraph
import com.chin.minddump.ui.MindDumpViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var cameraManager: CameraManager

    @Inject
    lateinit var audioRecorder: AudioRecorder

    // ViewModel reference for onNewIntent — set during setContent
    private var viewModel: MindDumpViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MindDumpViewModel = hiltViewModel()
            viewModel = vm
            // Handle share intent that launched the activity
            handleShareIfNeeded(vm, intent)
            MindDumpNavGraph(
                viewModel = vm,
                cameraManager = cameraManager,
                audioRecorder = audioRecorder,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel?.let { handleShareIfNeeded(it, intent) }
    }

    private fun handleShareIfNeeded(vm: MindDumpViewModel, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE) {
            vm.handleShareIntent(intent)
        }
    }
}
