package com.jeffsuebpeng.dailyreminder.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(taskId.toInt())
        AlarmScheduler.scheduleSnooze(context, taskId, minutesFromNow = 10)
    }
}
