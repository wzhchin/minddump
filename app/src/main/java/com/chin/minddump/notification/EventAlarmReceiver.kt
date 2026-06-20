package com.chin.minddump.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.chin.minddump.R
import com.chin.minddump.data.MindDumpRepository
import com.chin.minddump.storage.Space
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Fires when a scheduled event's exact alarm triggers. Posts a high-priority
 * reminder notification and writes the event's `state` to `fired` (so a restart
 * or re-reconcile never re-fires it).
 *
 * Receivers run on the main thread; DB writes happen on an IO scope.
 */
@AndroidEntryPoint
class EventAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: MindDumpRepository

    @Inject
    lateinit var scheduler: EventScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationActions.ACTION_EVENT_FIRE) return
        val ownerPath = intent.getStringExtra(NotificationActions.EXTRA_OWNER_PATH) ?: return
        val space = intent
            .getStringExtra(NotificationActions.EXTRA_SPACE)
            ?.let { runCatching { Space.valueOf(it) }.getOrNull() } ?: return
        val eventId = intent.getLongExtra(NotificationActions.EXTRA_EVENT_ID, -1L)
        if (eventId == -1L) return
        val ownerName = intent.getStringExtra(NotificationActions.EXTRA_OWNER_NAME).orEmpty()

        Timber.i("Event fired: %d for %s", eventId, ownerName)
        postNotification(context, ownerPath, space, ownerName)

        // goAsync keeps the receiver alive while the IO write completes.
        val pendingResult = goAsync()
        scope.launch {
            try {
                repository.markEventFired(ownerPath, eventId)
            } catch (t: Throwable) {
                Timber.e(t, "Failed to mark event fired")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postNotification(
        context: Context,
        ownerPath: String,
        space: Space,
        ownerName: String,
    ) {
        EventChannels.ensureCreated(context)
        val contentIntent: PendingIntent = buildOpenEntryPendingIntent(context, ownerPath, space)
        val title = context.getString(R.string.event_notification_title)
        val text = if (ownerName.isBlank()) title else ownerName

        val notification = NotificationCompat
            .Builder(context, EventChannels.REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        val manager = context.getSystemService<NotificationManager>()
        // A stable id per (owner, event) so the same event can't stack notifications.
        val notifId = (ownerPath + space.name).hashCode()
        manager?.notify(notifId, notification)
    }
}
