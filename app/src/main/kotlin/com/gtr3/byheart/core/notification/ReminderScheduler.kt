package com.gtr3.byheart.core.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    /**
     * Schedule one or more reminders for a note.
     *
     * @param reminderDays  Set of day indices (0=Mon … 6=Sun). Empty = one-time reminder
     *                      at [triggerAtMillis]; non-empty = weekly repeat on those days
     *                      at the hour/minute encoded in [triggerAtMillis].
     */
    fun schedule(
        context: Context,
        noteId: Long,
        reminderTitle: String,
        noteTitle: String,
        triggerAtMillis: Long,
        reminderDays: Set<Int> = emptySet()
    ) {
        if (reminderDays.isEmpty()) {
            // One-time reminder at the exact timestamp
            enqueue(
                context, noteId, reminderTitle, noteTitle,
                triggerAtMillis, isRepeating = false, dayOfWeek = -1, hour = 0, minute = 0
            )
        } else {
            // Weekly repeat: one WorkRequest per selected day
            val cal    = Calendar.getInstance().apply { timeInMillis = triggerAtMillis }
            val hour   = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            reminderDays.forEach { dayIndex ->
                val next = nextOccurrence(dayIndex, hour, minute)
                enqueue(
                    context, noteId, reminderTitle, noteTitle,
                    next, isRepeating = true, dayOfWeek = dayIndex, hour = hour, minute = minute
                )
            }
        }
    }

    /**
     * Called by [ReminderWorker] to re-queue itself for the same day next week.
     */
    fun reschedule(
        context: Context,
        noteId: Long,
        reminderTitle: String,
        noteTitle: String,
        dayOfWeek: Int,
        hour: Int,
        minute: Int
    ) {
        // nextOccurrence with a 6-day minimum skips "today" and lands on next week
        val next = nextOccurrence(dayOfWeek, hour, minute, minDelayMs = 6L * 24 * 60 * 60 * 1000)
        enqueue(
            context, noteId, reminderTitle, noteTitle,
            next, isRepeating = true, dayOfWeek = dayOfWeek, hour = hour, minute = minute
        )
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun enqueue(
        context: Context,
        noteId: Long,
        reminderTitle: String,
        noteTitle: String,
        triggerAtMillis: Long,
        isRepeating: Boolean,
        dayOfWeek: Int,
        hour: Int,
        minute: Int
    ) {
        val delay = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)

        val data = Data.Builder()
            .putString(ReminderWorker.KEY_TITLE,      reminderTitle)
            .putString(ReminderWorker.KEY_NOTE_TITLE, noteTitle)
            .putLong(ReminderWorker.KEY_NOTE_ID,      noteId)
            .putBoolean(ReminderWorker.KEY_IS_REPEATING, isRepeating)
            .putInt(ReminderWorker.KEY_DAY_OF_WEEK,   dayOfWeek)
            .putInt(ReminderWorker.KEY_HOUR,          hour)
            .putInt(ReminderWorker.KEY_MINUTE,        minute)
            .build()

        // Unique tag per note + day so repeated enqueues replace the old one
        val tag = "reminder_${noteId}_$dayOfWeek"

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(tag)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(tag, ExistingWorkPolicy.REPLACE, request)
    }

    /**
     * Returns the epoch-millis of the next occurrence of [dayIndex] (0=Mon … 6=Sun)
     * at [hour]:[minute], at least [minDelayMs] milliseconds from now.
     */
    fun nextOccurrence(
        dayIndex: Int,
        hour: Int,
        minute: Int,
        minDelayMs: Long = 0L
    ): Long {
        val targetCalDay = when (dayIndex) {
            0 -> Calendar.MONDAY
            1 -> Calendar.TUESDAY
            2 -> Calendar.WEDNESDAY
            3 -> Calendar.THURSDAY
            4 -> Calendar.FRIDAY
            5 -> Calendar.SATURDAY
            6 -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }
        val earliest = System.currentTimeMillis() + minDelayMs
        val cal = Calendar.getInstance().apply {
            timeInMillis = earliest
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE,      minute)
            set(Calendar.SECOND,      0)
            set(Calendar.MILLISECOND, 0)
        }
        // If setting the time on today already put us in the past, start from tomorrow
        if (cal.timeInMillis <= earliest) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        // Advance day-by-day until we land on the right weekday
        while (cal.get(Calendar.DAY_OF_WEEK) != targetCalDay) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }
}
