package com.chin.minddump.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import com.chin.minddump.R

/**
 * Notification channel identifiers and creation. Two channels are created at app
 * startup so both event reminders and (future) digest/background jobs can post
 * with independent user-controllable importance.
 */
object EventChannels {
    const val REMINDERS = "reminders"
    const val DIGEST = "digest"

    /**
     * Create both channels. Idempotent — calling repeatedly is a no-op once they
     * exist. Safe to call on every app start.
     */
    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return

        val reminders = NotificationChannel(
            REMINDERS,
            context.getString(R.string.channel_reminders_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_reminders_desc)
            enableVibration(true)
        }
        val digest = NotificationChannel(
            DIGEST,
            context.getString(R.string.channel_digest_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_digest_desc)
        }
        manager.createNotificationChannels(listOf(reminders, digest))
    }
}

/** Custom intent actions used by the notification/scheduling subsystem. */
object NotificationActions {
    /** Fired by AlarmManager when an event is due. */
    const val ACTION_EVENT_FIRE = "com.chin.minddump.action.EVENT_FIRE"

    /** Deep link action to open a specific entry (notification tap). */
    const val ACTION_OPEN_ENTRY = "com.chin.minddump.action.OPEN_ENTRY"

    const val EXTRA_OWNER_PATH = "owner_path"
    const val EXTRA_SPACE = "space"

    /** The firing event's DB row id (events table). Replaces the old due#state key. */
    const val EXTRA_EVENT_ID = "event_id"

    /** PendingIntent request-code namespace base for event alarms. */
    const val EXTRA_OWNER_NAME = "owner_name"
}
