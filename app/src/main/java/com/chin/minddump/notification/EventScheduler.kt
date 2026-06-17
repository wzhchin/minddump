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
 * Alarm identity is derived from the owner file path + event key, so cancel-then-set
 * is idempotent: re-registering the same event replaces rather than duplicates it.
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
        eventKey: String,
        dueAtMillis: Long,
        alreadyFired: Boolean,
    ) {
        if (alreadyFired) return
        val now = System.currentTimeMillis()
        if (dueAtMillis <= now) {
            Timber.d("Skip scheduling past event %s for %s", eventKey, owner.name)
            return
        }
        val am = alarmManager ?: run {
            Timber.w("No AlarmManager available; cannot schedule event %s", eventKey)
            return
        }
        val pi = buildFirePendingIntent(owner, ownerName, space, eventKey)
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
        Timber.d("Scheduled event %s at %d for %s", eventKey, dueAtMillis, owner.name)
    }

    /** Cancel a scheduled event (e.g. when removed or fired). */
    fun cancel(owner: File, space: Space, eventKey: String) {
        val am = alarmManager ?: return
        am.cancel(buildFirePendingIntent(owner, "", space, eventKey))
    }

    private fun buildFirePendingIntent(
        owner: File,
        ownerName: String,
        space: Space,
        eventKey: String,
    ): PendingIntent {
        val intent = Intent(context, EventAlarmReceiver::class.java).apply {
            action = NotificationActions.ACTION_EVENT_FIRE
            putExtra(NotificationActions.EXTRA_OWNER_PATH, owner.absolutePath)
            putExtra(NotificationActions.EXTRA_SPACE, space.name)
            putExtra(NotificationActions.EXTRA_EVENT_KEY, eventKey)
            putExtra(NotificationActions.EXTRA_OWNER_NAME, ownerName)
        }
        val requestCode = identityHashCode(owner.absolutePath, eventKey)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    /**
     * A stable int identity for the (ownerPath, eventKey) pair. Used as the
     * PendingIntent request code so each distinct event is independently
     * cancelable. Deterministic across process restarts.
     */
    private fun identityHashCode(ownerPath: String, eventKey: String): Int {
        var hash = 1
        for (c in ownerPath) hash = 31 * hash + c.code
        for (c in eventKey) hash = 31 * hash + c.code
        return hash
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
