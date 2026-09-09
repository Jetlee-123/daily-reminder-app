package com.jeffsuebpeng.dailyreminder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jeffsuebpeng.dailyreminder.data.QuietHoursPrefs
import com.jeffsuebpeng.dailyreminder.data.VacationModePrefs
import com.jeffsuebpeng.dailyreminder.ui.theme.ThemePrefs
import java.text.SimpleDateFormat
import java.util.Locale

private fun formatTime12Hour(hour: Int, minute: Int): String {
    val period = if (hour >= 12) "PM" else "AM"
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    return String.format(Locale.getDefault(), "%d:%02d %s", h12, minute, period)
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(millis)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var isDarkMode by remember { mutableStateOf(ThemePrefs.isDarkMode(context)) }

    var quietEnabled by remember { mutableStateOf(QuietHoursPrefs.isEnabled(context)) }
    var startHour by remember { mutableStateOf(QuietHoursPrefs.getStart(context).first) }
    var startMinute by remember { mutableStateOf(QuietHoursPrefs.getStart(context).second) }
    var endHour by remember { mutableStateOf(QuietHoursPrefs.getEnd(context).first) }
    var endMinute by remember { mutableStateOf(QuietHoursPrefs.getEnd(context).second) }
    var editingStart by remember { mutableStateOf(true) }
    var showTimePicker by remember { mutableStateOf(false) }

    var vacationEnabled by remember { mutableStateOf(VacationModePrefs.isEnabled(context)) }
    var vacationRange by remember { mutableStateOf(VacationModePrefs.getRange(context)) }
    var pickingVacationStart by remember { mutableStateOf(true) }
    var showVacationDatePicker by remember { mutableStateOf(false) }
    var vacationDraftStart by remember { mutableStateOf(System.currentTimeMillis()) }
    var vacationDraftEnd by remember { mutableStateOf(System.currentTimeMillis() + 86_400_000L) }

    val timePickerState = rememberTimePickerState(
        initialHour = if (editingStart) startHour else endHour,
        initialMinute = if (editingStart) startMinute else endMinute,
        is24Hour = false
    )
    val vacationDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = if (pickingVacationStart) vacationDraftStart else vacationDraftEnd
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dark mode (Liquid Glass)")
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { isDarkMode = it; ThemePrefs.setDarkMode(context, it) }
                )
            }

            Text("Vacation mode", style = MaterialTheme.typography.titleMedium)
            Text(
                "Pause ALL notifications for a date range, without changing any individual task's settings.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable vacation mode")
                Switch(
                    checked = vacationEnabled,
                    onCheckedChange = { enabled ->
                        vacationEnabled = enabled
                        if (!enabled) {
                            VacationModePrefs.disable(context)
                        } else {
                            VacationModePrefs.setRange(context, vacationDraftStart, vacationDraftEnd)
                            vacationRange = vacationDraftStart to vacationDraftEnd
                        }
                    }
                )
            }
            if (vacationEnabled) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { pickingVacationStart = true; showVacationDatePicker = true }) {
                        Text("From: ${formatDate(vacationRange?.first ?: vacationDraftStart)}")
                    }
                    Button(onClick = { pickingVacationStart = false; showVacationDatePicker = true }) {
                        Text("To: ${formatDate(vacationRange?.second ?: vacationDraftEnd)}")
                    }
                }
            }

            Text("Quiet hours", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp))
            Text(
                "Mute reminder notifications during this window every day.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable quiet hours")
                Switch(
                    checked = quietEnabled,
                    onCheckedChange = { quietEnabled = it; QuietHoursPrefs.setEnabled(context, it) }
                )
            }
            if (quietEnabled) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { editingStart = true; showTimePicker = true }) {
                        Text("From: ${formatTime12Hour(startHour, startMinute)}")
                    }
                    Button(onClick = { editingStart = false; showTimePicker = true }) {
                        Text("To: ${formatTime12Hour(endHour, endMinute)}")
                    }
                }
            }

            if (showTimePicker) {
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            if (editingStart) { startHour = timePickerState.hour; startMinute = timePickerState.minute }
                            else { endHour = timePickerState.hour; endMinute = timePickerState.minute }
                            QuietHoursPrefs.setRange(context, startHour, startMinute, endHour, endMinute)
                            showTimePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
                    text = { TimePicker(state = timePickerState) }
                )
            }

            if (showVacationDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showVacationDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val picked = vacationDatePickerState.selectedDateMillis ?: return@TextButton
                            if (pickingVacationStart) vacationDraftStart = picked else vacationDraftEnd = picked
                            if (vacationEnabled) {
                                VacationModePrefs.setRange(context, vacationDraftStart, vacationDraftEnd)
                                vacationRange = vacationDraftStart to vacationDraftEnd
                            }
                            showVacationDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { showVacationDatePicker = false }) { Text("Cancel") } }
                ) { DatePicker(state = vacationDatePickerState) }
            }
        }
    }
}
