package com.jeffsuebpeng.dailyreminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jeffsuebpeng.dailyreminder.data.CompletionLog
import com.jeffsuebpeng.dailyreminder.data.Task
import com.jeffsuebpeng.dailyreminder.data.TaskDatabase
import com.jeffsuebpeng.dailyreminder.data.TaskRepository
import com.jeffsuebpeng.dailyreminder.notifications.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository
    private val completionLogDao = TaskDatabase.getDatabase(application).completionLogDao()
    val allTasks: StateFlow<List<Task>>

    private val _lastDeletedTask = MutableStateFlow<Task?>(null)
    val lastDeletedTask: StateFlow<Task?> = _lastDeletedTask

    init {
        val dao = TaskDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(dao)
        allTasks = repository.allTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun loadTask(id: Long, onLoaded: (Task?) -> Unit) {
        viewModelScope.launch { onLoaded(repository.getById(id)) }
    }

    fun addOrUpdateTask(task: Task) {
        viewModelScope.launch {
            val id = if (task.id == 0L) repository.insert(task) else { repository.update(task); task.id }
            AlarmScheduler.scheduleTask(getApplication(), task.copy(id = id))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            AlarmScheduler.cancelTask(getApplication(), task.id)
            repository.delete(task)
            _lastDeletedTask.value = task
        }
    }

    fun undoDelete() {
        val task = _lastDeletedTask.value ?: return
        viewModelScope.launch {
            val newId = repository.insert(task.copy(id = 0))
            AlarmScheduler.scheduleTask(getApplication(), task.copy(id = newId))
            _lastDeletedTask.value = null
        }
    }

    fun dismissUndo() {
        _lastDeletedTask.value = null
    }

    fun toggleNotifications(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(notifyEnabled = !task.notifyEnabled)
            repository.update(updated)
            if (updated.notifyEnabled) {
                if (!updated.isCompleted) AlarmScheduler.scheduleTask(getApplication(), updated)
            } else {
                AlarmScheduler.cancelTask(getApplication(), updated.id)
            }
        }
    }

    fun toggleCompleted(task: Task) {
        viewModelScope.launch {
            val nowCompleted = !task.isCompleted
            val updated = task.copy(
                isCompleted = nowCompleted,
                lastCompletedDateMillis = if (nowCompleted) System.currentTimeMillis() else task.lastCompletedDateMillis,
                streak = when {
                    nowCompleted && task.isRecurring -> task.streak + 1
                    !nowCompleted && task.isRecurring -> (task.streak - 1).coerceAtLeast(0)
                    else -> task.streak
                }
            )
            repository.update(updated)

            if (nowCompleted) {
                completionLogDao.insert(CompletionLog(taskId = task.id, completedAtMillis = System.currentTimeMillis()))
            }

            if (!updated.isRecurring && updated.isCompleted) {
                AlarmScheduler.cancelTask(getApplication(), updated.id)
            }
        }
    }

    fun skipNextOccurrence(task: Task) {
        viewModelScope.launch {
            val (hour, minute) = task.parsedTimes().first()
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            if (cal.timeInMillis <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            val isoDate = Task.isoDateOf(cal.timeInMillis)
            val updatedSkipped = (task.parsedSkippedDates() + isoDate).joinToString(",")
            val updated = task.copy(skippedDates = updatedSkipped)
            repository.update(updated)
            AlarmScheduler.scheduleTask(getApplication(), updated)
        }
    }
}
