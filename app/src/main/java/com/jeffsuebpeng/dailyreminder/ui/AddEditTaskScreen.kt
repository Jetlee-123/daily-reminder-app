package com.jeffsuebpeng.dailyreminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jeffsuebpeng.dailyreminder.TaskViewModel
import com.jeffsuebpeng.dailyreminder.data.CANONICAL_DAY_ORDER
import com.jeffsuebpeng.dailyreminder.data.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val DAY_FULL_NAMES = mapOf(
    "MON" to "Mon", "TUE" to "Tue", "WED" to "Wed", "THU" to "Thu",
    "FRI" to "Fri", "SAT" to "Sat", "SUN" to "Sun"
)

private val COLOR_PALETTE = listOf(
    "Purple" to Color(0xFF6750A4), "Red" to Color(0xFFE53935),
    "Orange" to Color(0xFFFB8C00), "Yellow" to Color(0xFFF9A825),
    "Green" to Color(0xFF43A047), "Teal" to Color(0xFF00897B),
    "Blue" to Color(0xFF1E88E5), "Pink" to Color(0xFFD81B60)
)
private val LEAD_PRESETS = listOf("At the time" to 0, "10 min before" to 10, "1 hour before" to 60, "1 day before" to 1440, "Custom" to -1)
private val LEAD_UNITS = listOf("Minutes before", "Hours before", "Days before")

private fun colorToHex(color: Color): String {
    val argb = color.value
    val r = ((argb shr 40) and 0xFFuL).toInt(); val g = ((argb shr 32) and 0xFFuL).toInt(); val b = ((argb shr 24) and 0xFFuL).toInt()
    return String.format("#%02X%02X%02X", r, g, b)
}
private fun formatTime12Hour(hour: Int, minute: Int): String {
    val period = if (hour >= 12) "PM" else "AM"; val h12 = if (hour % 12 == 0) 12 else hour % 12
    return String.format(Locale.getDefault(), "%d:%02d %s", h12, minute, period)
}
private fun formatDateReadable(millis: Long): String = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(millis)
private fun leadLabelFor(minutes: Int): String? {
    if (minutes <= 0) return null
    return when {
        minutes % 1440 == 0 -> { val d = minutes / 1440; if (d == 1) "1 day before" else "$d days before" }
        minutes % 60 == 0 -> { val h = minutes / 60; if (h == 1) "1 hour before" else "$h hours before" }
        else -> "$minutes min before"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(viewModel: TaskViewModel, existingTaskId: Long?, onDone: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableStateOf(setOf<String>()) }
    val reminderTimes = remember { mutableStateListOf(9 to 0) }
    var existingId by remember { mutableStateOf(0L) }
    var editingTimeSlotIndex by remember { mutableStateOf<Int?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var dueDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedColor by remember { mutableStateOf(COLOR_PALETTE[0].second) }
    var selectedLeadPresetIndex by remember { mutableStateOf(0) }
    var customLeadValue by remember { mutableStateOf("15") }
    var customLeadUnitIndex by remember { mutableStateOf(0) }
    var showUnitMenu by remember { mutableStateOf(false) }
    var tagsText by remember { mutableStateOf("") }

    fun resolvedLeadMinutes(): Int {
        val preset = LEAD_PRESETS[selectedLeadPresetIndex].second
        if (preset != -1) return preset
        val value = customLeadValue.toIntOrNull() ?: 0
        return when (customLeadUnitIndex) { 2 -> value * 1440; 1 -> value * 60; else -> value }
    }

    LaunchedEffect(existingTaskId) {
        existingTaskId?.let { id ->
            viewModel.loadTask(id) { task ->
                task?.let {
                    existingId = it.id
                    title = it.title
                    notes = it.notes
                    tagsText = it.parsedTags().joinToString(", ")
                    isRecurring = it.isRecurring
                    selectedDays = it.recurrenceDays.split(",").filter { d -> d.isNotBlank() }.toSet()
                    reminderTimes.clear(); reminderTimes.addAll(it.parsedTimes())
                    dueDateMillis = it.dueDateMillis ?: System.currentTimeMillis()
                    selectedColor = try { Color(android.graphics.Color.parseColor(it.colorHex)) } catch (e: Exception) { COLOR_PALETTE[0].second }

                    val presetIdx = LEAD_PRESETS.indexOfFirst { p -> p.second == it.leadMinutes }
                    if (presetIdx != -1) selectedLeadPresetIndex = presetIdx
                    else {
                        selectedLeadPresetIndex = LEAD_PRESETS.lastIndex
                        when {
                            it.leadMinutes % 1440 == 0 -> { customLeadUnitIndex = 2; customLeadValue = (it.leadMinutes / 1440).toString() }
                            it.leadMinutes % 60 == 0 -> { customLeadUnitIndex = 1; customLeadValue = (it.leadMinutes / 60).toString() }
                            else -> { customLeadUnitIndex = 0; customLeadValue = it.leadMinutes.toString() }
                        }
                    }
                }
            }
        }
    }

    val activeTimeForPicker = editingTimeSlotIndex?.let { reminderTimes.getOrNull(it) } ?: (9 to 0)
    val timePickerState = rememberTimePickerState(initialHour = activeTimeForPicker.first, initialMinute = activeTimeForPicker.second, is24Hour = false)
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDateMillis)

    Scaffold(topBar = { TopAppBar(title = { Text(if (existingTaskId == null) "New Task" else "Edit Task") }) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 3
            )
            OutlinedTextField(
                value = tagsText, onValueChange = { tagsText = it },
                label = { Text("Tags (comma separated)") }, modifier = Modifier.fillMaxWidth()
            )

            Column {
                Text("Colour", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                ColorPalettePicker(selected = selectedColor, onSelect = { selectedColor = it })
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Repeat on specific days?", modifier = Modifier.weight(1f))
                Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
            }

            if (isRecurring) {
                DaySelector(selectedDays = selectedDays, onToggleDay = { day -> selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day })
            } else {
                Button(onClick = { showDatePicker = true }) { Text("Due date: ${formatDateReadable(dueDateMillis)}") }
            }

            Column {
                Text("Reminder time(s)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                reminderTimes.sortedBy { it.first * 60 + it.second }.forEach { time ->
                    val actualIndex = reminderTimes.indexOf(time)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { editingTimeSlotIndex = actualIndex; showTimePicker = true }, modifier = Modifier.weight(1f)) {
                            Text(formatTime12Hour(time.first, time.second))
                        }
                        if (reminderTimes.size > 1) {
                            IconButton(onClick = { reminderTimes.removeAt(actualIndex) }) { Icon(Icons.Default.Close, contentDescription = "Remove time") }
                        }
                    }
                }
                TextButton(onClick = { reminderTimes.add(12 to 0); editingTimeSlotIndex = reminderTimes.lastIndex; showTimePicker = true }) {
                    Icon(Icons.Default.Add, contentDescription = null); Text(" Add another time")
                }
            }

            Column {
                Text("Notify me", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    LEAD_PRESETS.forEachIndexed { index, (label, _) ->
                        FilterChip(selected = selectedLeadPresetIndex == index, onClick = { selectedLeadPresetIndex = index }, label = { Text(label) })
                    }
                }
                if (LEAD_PRESETS[selectedLeadPresetIndex].second == -1) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = customLeadValue, onValueChange = { customLeadValue = it.filter { c -> c.isDigit() } }, modifier = Modifier.size(width = 90.dp, height = 56.dp), label = { Text("Value") })
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { showUnitMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(LEAD_UNITS[customLeadUnitIndex]) }
                            DropdownMenu(expanded = showUnitMenu, onDismissRequest = { showUnitMenu = false }) {
                                LEAD_UNITS.forEachIndexed { idx, unitLabel -> DropdownMenuItem(text = { Text(unitLabel) }, onClick = { customLeadUnitIndex = idx; showUnitMenu = false }) }
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().background(selectedColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                val timesLabel = reminderTimes.sortedBy { it.first * 60 + it.second }.joinToString(", ") { formatTime12Hour(it.first, it.second) }
                var summary = if (isRecurring) {
                    if (selectedDays.isEmpty()) "Pick at least one day to repeat on"
                    else "Repeats every ${selectedDays.sortedBy { CANONICAL_DAY_ORDER.indexOf(it) }.joinToString(", ") { DAY_FULL_NAMES[it] ?: it }} at $timesLabel"
                } else "One-time reminder on ${formatDateReadable(dueDateMillis)} at $timesLabel"
                leadLabelFor(resolvedLeadMinutes())?.let { summary += " — notify $it" }
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = selectedColor)
            }

            if (showTimePicker) {
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            editingTimeSlotIndex?.let { idx -> if (idx in reminderTimes.indices) reminderTimes[idx] = timePickerState.hour to timePickerState.minute }
                            showTimePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
                    text = { TimePicker(state = timePickerState) }
                )
            }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { dueDateMillis = it }; showDatePicker = false }) { Text("OK") } },
                    dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
                ) { DatePicker(state = datePickerState) }
            }

            Button(
                onClick = {
                    val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val task = Task(
                        id = existingId, title = title, notes = notes,
                        isRecurring = isRecurring, recurrenceDays = Task.daysToString(selectedDays),
                        dueDateMillis = if (!isRecurring) dueDateMillis else null,
                        reminderTimes = Task.timesToString(reminderTimes.toList()),
                        colorHex = colorToHex(selectedColor), leadMinutes = resolvedLeadMinutes(),
                        tags = Task.tagsToString(tags)
                    )
                    viewModel.addOrUpdateTask(task)
                    onDone()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && (!isRecurring || selectedDays.isNotEmpty()) && reminderTimes.isNotEmpty()
            ) { Text("Save Task") }
        }
    }
}

@Composable
fun ColorPalettePicker(selected: Color, onSelect: (Color) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        COLOR_PALETTE.forEach { (name, color) ->
            val isSelected = color == selected
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(color)
                    .border(width = if (isSelected) 3.dp else 0.dp, color = MaterialTheme.colorScheme.onSurface, shape = CircleShape)
                    .clickable { onSelect(color) },
                contentAlignment = Alignment.Center
            ) { if (isSelected) Icon(Icons.Default.Check, contentDescription = name, tint = Color.White) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaySelector(selectedDays: Set<String>, onToggleDay: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        CANONICAL_DAY_ORDER.forEach { day -> FilterChip(selected = day in selectedDays, onClick = { onToggleDay(day) }, label = { Text(day.take(1)) }) }
    }
}
