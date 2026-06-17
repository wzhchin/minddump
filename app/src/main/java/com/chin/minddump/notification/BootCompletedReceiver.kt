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

    @Inject
    lateinit var scheduler: EventScheduler

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
        var count = 0
        repository.getAllEntries(Space.PUBLIC).forEach { entry ->
            entry.events.forEach { ev ->
                if (ev.state == EventState.PENDING) {
                    scheduler.schedule(
                        owner = entry.file,
                        ownerName = entry.file.name,
                        space = Space.PUBLIC,
                        eventKey = ev.key(),
                        dueAtMillis = ev.dueMillis(),
                        alreadyFired = false,
                    )
                    count++
                }
            }
        }
        Timber.i("Re-registered %d public events after boot", count)
    }
}
