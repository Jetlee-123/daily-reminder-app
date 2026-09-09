package com.jeffsuebpeng.dailyreminder.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jeffsuebpeng.dailyreminder.data.Task
import java.util.Calendar

object AlarmScheduler {

    private const val MAX_TIMES_PER_TASK = 12
    private const val SNOOZE_REQUEST_CODE_OFFSET = 900_000

    private val DAY_CODE_TO_CALENDAR_DAY = mapOf(
        "SUN" to Calendar.SUNDAY, "MON" to Calendar.MONDAY, "TUE" to Calendar.TUESDAY,
        "WED" to Calendar.WEDNESDAY, "THU" to Calendar.THURSDAY, "FRI" to Calendar.FRIDAY,
        "SAT" to Calendar.SATURDAY
    )

    private fun requestCode(taskId: Long, slotIndex: Int): Int = (taskId * 100 + slotIndex).toInt()
    private fun snoozeRequestCode(taskId: Long): Int = SNOOZE_REQUEST_CODE_OFFSET + taskId.toInt()

    fun scheduleTask(context: Context, task: Task) {
        cancelTask(context, task.id)
        if (!task.notifyEnabled) return

        task.parsedTimes().forEachIndexed { index, (hour, minute) ->
            val result = computeNextAlarmTime(task, hour, minute, Calendar.getInstance()) ?: return@forEachIndexed
            setExactAlarm(context, task.id, index, result)
        }
    }

    fun scheduleSingleSlot(context: Context, task: Task, slotIndex: Int) {
        if (!task.notifyEnabled) return
        val times = task.parsedTimes()
        if (slotIndex !in times.indices) return
        val (hour, minute) = times[slotIndex]
        val result = computeNextAlarmTime(task, hour, minute, Calendar.getInstance()) ?: return
        setExactAlarm(context, task.id, slotIndex, result)
    }

    fun scheduleSnooze(context: Context, taskId: Long, minutesFromNow: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("task_id", taskId)
            putExtra("time_slot_index", -1)
            putExtra("is_snooze", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, snoozeRequestCode(taskId), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + minutesFromNow * 60_000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private data class AlarmResult(val alarmMillis: Long, val dueMillis: Long)

    private fun computeNextAlarmTime(task: Task, hour: Int, minute: Int, searchFrom: Calendar): AlarmResult? {
        val now = System.currentTimeMillis()
        val leadMillis = task.leadMinutes * 60_000L
        var cursor = searchFrom.clone() as Calendar

        repeat(15) {
            val due = nextDueOccurrence(task, hour, minute, cursor) ?: return null
            val alarmMillis = due - leadMillis
            if (alarmMillis > now) return AlarmResult(alarmMillis, due)
            if (!task.isRecurring) return null
            cursor = Calendar.getInstance().apply { timeInMillis = due }
        }
        return null
    }

    private fun nextDueOccurrence(task: Task, hour: Int, minute: Int, notBefore: Calendar): Long? {
        if (task.isRecurring) {
            val days = task.sortedDays()
            if (days.isEmpty()) return null
            val targetCalendarDays = days.mapNotNull { DAY_CODE_TO_CALENDAR_DAY[it] }.toSet()
            if (targetCalendarDays.isEmpty()) return null

            val candidate = notBefore.clone() as Calendar
            candidate.set(Calendar.HOUR_OF_DAY, hour)
            candidate.set(Calendar.MINUTE, minute)
            candidate.set(Calendar.SECOND, 0)
            candidate.set(Calendar.MILLISECOND, 0)

            for (i in 0..10) {
                if (candidate.get(Calendar.DAY_OF_WEEK) in targetCalendarDays &&
                    candidate.after(notBefore) &&
                    !task.isDateSkipped(candidate.timeInMillis)
                ) {
                    return candidate.timeInMillis
                }
                candidate.add(Calendar.DAY_OF_YEAR, 1)
            }
            return null
        } else {
            val baseDate = task.dueDateMillis ?: System.currentTimeMillis()
            val candidate = Calendar.getInstance().apply {
                timeInMillis = baseDate
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return if (candidate.after(notBefore)) candidate.timeInMillis else null
        }
    }

    private fun setExactAlarm(context: Context, taskId: Long, slotIndex: Int, result: AlarmResult) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("task_id", taskId)
            putExtra("time_slot_index", slotIndex)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode(taskId, slotIndex), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, result.alarmMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, result.alarmMillis, pendingIntent)
        }
    }

    fun cancelTask(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (slot in 0 until MAX_TIMES_PER_TASK) {
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode(taskId, slot), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
        val snoozeIntent = Intent(context, ReminderReceiver::class.java)
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, snoozeRequestCode(taskId), snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(snoozePendingIntent)
    }
}
