package com.chin.minddump.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chin.minddump.data.MindDumpRepository
import com.chin.minddump.storage.EventState
import com.chin.minddump.storage.Space
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Exact alarms are cleared on reboot. After boot, re-register every pending
 * Public-space event (Private events are registered on the next Private unlock,
 * since their encrypted sidecars can't be read without the session credential).
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: MindDumpRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        scope.launch {
            try {
                reRegisterPublic()
            } catch (t: Throwable) {
                Timber.e(t, "Boot event re-registration failed")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun reRegisterPublic() {
        // registerAllPublicEvents re-arms every pending Public event by its DB row
        // id via the repository (cancel-then-set, idempotent).
        repository.registerAllPublicEvents()
        val count = repository.getAllEntries(Space.PUBLIC).sumOf { entry ->
            entry.events.count { it.state == EventState.PENDING }
        }
        Timber.i("Re-registered %d public events after boot", count)
    }
}
