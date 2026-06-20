package com.chin.minddump.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.chin.minddump.MainActivity
import com.chin.minddump.storage.Space
import timber.log.Timber
import java.io.File

/**
 * Schedules entry events as exact alarms so they fire at their precise local
 * `due` time even under Doze. Alarms are cleared on reboot, so [BootCompletedReceiver]
 * re-registers pending events after boot.
 *
 * Alarm identity is the event's DB row id (the `events` table primary key), so
 * cancel-then-set is idempotent: re-registering the same event replaces rather
 * than duplicates it. The id is stable for the life of the event row.
 */
class EventScheduler(
    private val context: Context,
) {
    private val alarmManager = context.getSystemService<AlarmManager>()

    /**
     * Schedule (or reschedule) an event. No-op when [dueAtMillis] is in the past
     * or when the event has already fired.
     */
    fun schedule(
        owner: File,
        ownerName: String,
        space: Space,
        eventId: Long,
        dueAtMillis: Long,
        alreadyFired: Boolean,
    ) {
        if (alreadyFired) return
        val now = System.currentTimeMillis()
        if (dueAtMillis <= now) {
            Timber.d("Skip scheduling past event %d for %s", eventId, owner.name)
            return
        }
        val am = alarmManager ?: run {
            Timber.w("No AlarmManager available; cannot schedule event %d", eventId)
            return
        }
        val pi = buildFirePendingIntent(owner, ownerName, space, eventId)
        am.cancel(pi) // idempotent: cancel any prior alarm for this identity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                dueAtMillis,
                pi,
            )
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, dueAtMillis, pi)
        }
        Timber.d("Scheduled event %d at %d for %s", eventId, dueAtMillis, owner.name)
    }

    /** Cancel a scheduled event by its DB row id. */
    fun cancelById(eventId: Long) {
        val am = alarmManager ?: return
        am.cancel(buildFirePendingIntent(File(""), "", Space.PUBLIC, eventId))
    }

    private fun buildFirePendingIntent(
        owner: File,
        ownerName: String,
        space: Space,
        eventId: Long,
    ): PendingIntent {
        val intent = Intent(context, EventAlarmReceiver::class.java).apply {
            action = NotificationActions.ACTION_EVENT_FIRE
            putExtra(NotificationActions.EXTRA_OWNER_PATH, owner.absolutePath)
            putExtra(NotificationActions.EXTRA_SPACE, space.name)
            putExtra(NotificationActions.EXTRA_EVENT_ID, eventId)
            putExtra(NotificationActions.EXTRA_OWNER_NAME, ownerName)
        }
        // Use the event id directly as the request code — one distinct alarm per event.
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, eventId.toInt(), intent, flags)
    }
}

/**
 * Deep-link PendingIntent that opens [MainActivity] to a specific entry. Used as
 * the notification content intent so tapping a reminder navigates to the entry.
 */
fun buildOpenEntryPendingIntent(
    context: Context,
    ownerPath: String,
    space: Space,
): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        action = NotificationActions.ACTION_OPEN_ENTRY
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(NotificationActions.EXTRA_OWNER_PATH, ownerPath)
        putExtra(NotificationActions.EXTRA_SPACE, space.name)
    }
    val requestCode = ownerPath.hashCode()
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    return PendingIntent.getActivity(context, requestCode, intent, flags)
}
