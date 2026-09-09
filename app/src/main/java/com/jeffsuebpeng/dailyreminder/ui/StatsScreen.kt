package com.jeffsuebpeng.dailyreminder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jeffsuebpeng.dailyreminder.data.Task
import com.jeffsuebpeng.dailyreminder.data.TaskDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class TaskStats(val task: Task, val last7: Int, val last30: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var stats by remember { mutableStateOf<List<TaskStats>>(emptyList()) }

    LaunchedEffect(Unit) {
        val db = TaskDatabase.getDatabase(context)
        val tasks = db.taskDao().getAllTasksSync()
        val logs = db.completionLogDao().getAllLogs()
        val now = System.currentTimeMillis()
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000

        stats = withContext(Dispatchers.Default) {
            tasks.map { task ->
                val taskLogs = logs.filter { it.taskId == task.id }
                val last7 = taskLogs.count { now - it.completedAtMillis <= sevenDaysMs }
                val last30 = taskLogs.count { now - it.completedAtMillis <= thirtyDaysMs }
                TaskStats(task, last7, last30)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stats") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        if (stats.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalArrangement = Arrangement.Center
            ) {}
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
                items(stats) { stat ->
                    val color = try {
                        Color(android.graphics.Color.parseColor(stat.task.colorHex))
                    } catch (e: Exception) { MaterialTheme.colorScheme.primary }

                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                stat.task.title + if (stat.task.streak > 0) "  \uD83D\uDD25 ${stat.task.streak}" else "",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Last 7 days: ${stat.last7} completions  \u00B7  Last 30 days: ${stat.last30}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LinearProgressIndicator(
                                progress = { (stat.last7 / 7f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp),
                                color = color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
