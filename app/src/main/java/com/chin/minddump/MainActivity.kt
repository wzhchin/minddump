package com.chin.minddump

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chin.minddump.camera.CameraManager
import com.chin.minddump.ui.MindDumpNavGraph
import com.chin.minddump.ui.MindDumpViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MindDumpViewModel = viewModel()
            val cameraManager = androidx.compose.runtime.remember { CameraManager() }
            MindDumpNavGraph(
                viewModel = viewModel,
                cameraManager = cameraManager,
            )
        }
    }
}
