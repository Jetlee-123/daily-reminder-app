package com.jeffsuebpeng.dailyreminder.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jeffsuebpeng.dailyreminder.data.QuietHoursPrefs
import com.jeffsuebpeng.dailyreminder.data.TaskDatabase
import com.jeffsuebpeng.dailyreminder.data.VacationModePrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

private fun formatTime12Hour(hour: Int, minute: Int): String {
    val period = if (hour >= 12) "PM" else "AM"
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    return String.format(Locale.getDefault(), "%d:%02d %s", h12, minute, period)
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("task_id", -1L)
        val slotIndex = intent.getIntExtra("time_slot_index", 0)
        val isSnooze = intent.getBooleanExtra("is_snooze", false)
        if (taskId == -1L) return

        CoroutineScope(Dispatchers.IO).launch {
            val dao = TaskDatabase.getDatabase(context).taskDao()
            val task = dao.getTaskById(taskId) ?: return@launch

            val suppressed = QuietHoursPrefs.isWithinQuietHours(context) || VacationModePrefs.isCurrentlyPaused(context)

            if (!suppressed) {
                val body = if (isSnooze) {
                    (if (task.notes.isNotBlank()) task.notes + "\n" else "") + "Snoozed reminder"
                } else {
                    val times = task.parsedTimes()
                    val (hour, minute) = times.getOrElse(slotIndex) { times.first() }
                    val leadLabel = task.leadLabel()
                    val parts = mutableListOf<String>()
                    if (task.notes.isNotBlank()) parts.add(task.notes)
                    if (leadLabel != null) parts.add("Due ${formatTime12Hour(hour, minute)} ($leadLabel)")
                    if (parts.isEmpty()) "Time to get this done!" else parts.joinToString("\n")
                }
                NotificationHelper.showNotification(context, task.id, task.title, body)
            }

            if (!isSnooze && task.isRecurring) {
                AlarmScheduler.scheduleSingleSlot(context, task, slotIndex)
            }
        }
    }
}
