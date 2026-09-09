package com.jeffsuebpeng.dailyreminder.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.jeffsuebpeng.dailyreminder.MainActivity
import com.jeffsuebpeng.dailyreminder.R

object NotificationHelper {

    const val CHANNEL_ID = "task_reminders"
    const val ACTION_SNOOZE = "com.jeffsuebpeng.dailyreminder.ACTION_SNOOZE"
    const val EXTRA_TASK_ID = "task_id"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Task Reminders", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Daily reminders for your to-do tasks" }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, taskId: Long, title: String, body: String) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_task_id", taskId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, taskId.toInt(), contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, (taskId + 500_000).toInt(), snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Snooze 10 min", snoozePendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(taskId.toInt(), notification)
    }
}
