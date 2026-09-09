package com.jeffsuebpeng.dailyreminder.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao) {

    val allTasks: Flow<List<Task>> = dao.getAllTasks()

    suspend fun insert(task: Task): Long = dao.insert(task)
    suspend fun update(task: Task) = dao.update(task)
    suspend fun delete(task: Task) = dao.delete(task)
    suspend fun getById(id: Long): Task? = dao.getTaskById(id)
    suspend fun getAllSync(): List<Task> = dao.getAllTasksSync()
}
