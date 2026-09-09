package com.jeffsuebpeng.dailyreminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeffsuebpeng.dailyreminder.TaskViewModel
import com.jeffsuebpeng.dailyreminder.data.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val DAY_FULL_NAMES = mapOf(
    "MON" to "Mon", "TUE" to "Tue", "WED" to "Wed", "THU" to "Thu",
    "FRI" to "Fri", "SAT" to "Sat", "SUN" to "Sun"
)
private val TIME_BADGE_BACKGROUND = Color(0xFFFFEDD5)
private val TIME_BADGE_CONTENT = Color(0xFF9A5A00)
private val LEAD_BADGE_BACKGROUND = Color(0xFFE1F0FF)
private val LEAD_BADGE_CONTENT = Color(0xFF0B5FA5)
private val TAG_BADGE_BACKGROUND = Color(0xFFEDE7F6)
private val TAG_BADGE_CONTENT = Color(0xFF4527A0)

private fun formatTime12Hour(hour: Int, minute: Int): String {
    val period = if (hour >= 12) "PM" else "AM"
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    return String.format(Locale.getDefault(), "%d:%02d %s", h12, minute, period)
}
private fun formatAllTimes(task: Task) = task.parsedTimes().joinToString(", ") { (h, m) -> formatTime12Hour(h, m) }
private fun formatDueDate(millis: Long?): String {
    val cal = Calendar.getInstance(); if (millis != null) cal.timeInMillis = millis
    return SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(cal.time)
}
private fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) { Color(0xFF6750A4) }

private fun taskMatchesQuery(task: Task, query: String): Boolean {
    if (query.isBlank()) return true
    val q = query.lowercase()
    return task.title.lowercase().contains(q) ||
        task.notes.lowercase().contains(q) ||
        task.parsedTags().any { it.lowercase().contains(q) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()
    val lastDeleted by viewModel.lastDeletedTask.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(lastDeleted) {
        val deleted = lastDeleted ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Deleted \"${deleted.title}\"",
            actionLabel = "UNDO",
            withDismissAction = true
        )
        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
            viewModel.undoDelete()
        } else {
            viewModel.dismissUndo()
        }
    }

    val visibleTasks = tasks.filter { taskMatchesQuery(it, searchQuery) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Daily Tasks") },
                actions = {
                    IconButton(onClick = onOpenStats) { Icon(Icons.Default.BarChart, contentDescription = "Stats") }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) { Icon(Icons.Default.Add, contentDescription = "Add task") }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search tasks or tags...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                items(visibleTasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onToggle = { viewModel.toggleCompleted(task) },
                        onDelete = { viewModel.deleteTask(task) },
                        onSkip = { viewModel.skipNextOccurrence(task) },
                        onClick = { onEditTask(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskRow(task: Task, onToggle: () -> Unit, onDelete: () -> Unit, onSkip: () -> Unit, onClick: () -> Unit) {
    val taskColor = parseColor(task.colorHex)
    val dateBadgeBackground = taskColor.copy(alpha = 0.14f)

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(IntrinsicSize.Min)) {
        Box(modifier = Modifier.width(6.dp).fillMaxHeight().background(taskColor, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)))
        Card(
            modifier = Modifier.weight(1f).clickable(onClick = onClick),
            shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 14.dp, bottomEnd = 14.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = task.isCompleted, onCheckedChange = { onToggle() },
                        colors = CheckboxDefaults.colors(checkedColor = taskColor, uncheckedColor = taskColor)
                    )
                    Row(
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            task.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (task.isRecurring && task.streak > 0) {
                            Surface(color = Color(0xFFFFF3CD), shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    "\uD83D\uDD25 ${task.streak}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF8A6D00),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (task.isRecurring) {
                        IconButton(onClick = onSkip) {
                            Icon(Icons.Default.DateRange, contentDescription = "Skip next occurrence", tint = Color(0xFF0B5FA5))
                        }
                    }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                }

                if (task.notes.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(start = 42.dp, top = 4.dp).fillMaxWidth()
                    ) {
                        Text(
                            task.notes,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.padding(start = 42.dp, top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (task.isRecurring) {
                        val dayLabels = task.sortedDays().joinToString(", ") { DAY_FULL_NAMES[it] ?: it }
                        InfoBadge(Icons.Default.Notifications, dayLabels, dateBadgeBackground, taskColor)
                    } else {
                        InfoBadge(Icons.Default.DateRange, formatDueDate(task.dueDateMillis), dateBadgeBackground, taskColor)
                    }
                    InfoBadge(Icons.Default.Schedule, formatAllTimes(task), TIME_BADGE_BACKGROUND, TIME_BADGE_CONTENT)
                    task.leadLabel()?.let { InfoBadge(Icons.Default.Notifications, it, LEAD_BADGE_BACKGROUND, LEAD_BADGE_CONTENT) }
                }

                if (task.parsedTags().isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(start = 42.dp, top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        task.parsedTags().forEach { tag ->
                            Surface(color = TAG_BADGE_BACKGROUND, contentColor = TAG_BADGE_CONTENT, shape = RoundedCornerShape(20.dp)) {
                                Text("#$tag", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, background: Color, contentColor: Color) {
    Surface(color = background, contentColor = contentColor, shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null)
            Text(text, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}
