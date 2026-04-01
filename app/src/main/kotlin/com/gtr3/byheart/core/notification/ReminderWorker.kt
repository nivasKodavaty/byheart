package com.gtr3.byheart.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gtr3.byheart.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val title       = inputData.getString(KEY_TITLE)      ?: "Note Reminder"
        val noteTitle   = inputData.getString(KEY_NOTE_TITLE) ?: ""
        val noteId      = inputData.getLong(KEY_NOTE_ID,      -1L)
        val isRepeating = inputData.getBoolean(KEY_IS_REPEATING, false)
        val dayOfWeek   = inputData.getInt(KEY_DAY_OF_WEEK,   -1)
        val hour        = inputData.getInt(KEY_HOUR,          0)
        val minute      = inputData.getInt(KEY_MINUTE,        0)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Note Reminders", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Reminders set on notes" }
            )
        }

        // Tapping the notification opens the linked note in MainActivity
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (noteId != -1L) putExtra(MainActivity.EXTRA_NOTE_ID, noteId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            noteId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(noteTitle.ifBlank { "You have a note reminder." })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notifId = if (noteId != -1L && dayOfWeek != -1) {
            // Unique ID per note + day so multiple days don't collide
            (noteId * 10 + dayOfWeek).toInt()
        } else if (noteId != -1L) {
            noteId.toInt()
        } else {
            System.currentTimeMillis().toInt()
        }
        manager.notify(notifId, notification)

        // If this is a repeating reminder, schedule the same notification for next week
        if (isRepeating && dayOfWeek != -1) {
            ReminderScheduler.reschedule(context, noteId, title, noteTitle, dayOfWeek, hour, minute)
        }

        return Result.success()
    }

    companion object {
        const val CHANNEL_ID       = "note_reminders"
        const val KEY_TITLE        = "reminder_title"
        const val KEY_NOTE_TITLE   = "note_title"
        const val KEY_NOTE_ID      = "note_id"
        const val KEY_IS_REPEATING = "is_repeating"
        const val KEY_DAY_OF_WEEK  = "day_of_week"
        const val KEY_HOUR         = "hour"
        const val KEY_MINUTE       = "minute"
    }
}
