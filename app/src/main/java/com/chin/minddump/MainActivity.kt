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
import com.chin.minddump.ui.ShortcutActions
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
            // Handle the intent that launched the activity (share or shortcut).
            handleLaunchingIntent(vm, intent)
            MindDumpNavGraph(
                viewModel = vm,
                cameraManager = cameraManager,
                audioRecorder = audioRecorder,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel?.let { handleLaunchingIntent(it, intent) }
    }

    override fun onStop() {
        super.onStop()
        // Wipe transient plaintext files decrypted to .cache/ for viewing/sharing.
        viewModel?.clearDecryptedCache()
    }

    /**
     * Route a launching intent to its handler: an inbound share goes to the share
     * flow; a launcher-shortcut action goes to the shortcut dispatcher. Other
     * actions (a plain launcher tap) do nothing.
     */
    private fun handleLaunchingIntent(vm: MindDumpViewModel, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> vm.handleShareIntent(intent)
            ShortcutActions.NEW_TEXT,
            ShortcutActions.PHOTO,
            ShortcutActions.RECORD,
            ShortcutActions.OPEN_PUBLIC,
            ShortcutActions.OPEN_PRIVATE,
            -> vm.dispatchShortcutAction(action)
        }
    }
}
