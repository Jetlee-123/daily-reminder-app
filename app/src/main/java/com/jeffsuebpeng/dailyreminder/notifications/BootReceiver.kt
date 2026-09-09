package com.jeffsuebpeng.dailyreminder.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jeffsuebpeng.dailyreminder.data.TaskDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val dao = TaskDatabase.getDatabase(context).taskDao()
                val tasks = dao.getAllTasksSync()
                tasks.filter { !it.isCompleted }.forEach { task ->
                    AlarmScheduler.scheduleTask(context, task)
                }
            }
        }
    }
}
