package com.jeffsuebpeng.dailyreminder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sub_tasks")
data class SubTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val title: String,
    val isDone: Boolean = false,
    val sortOrder: Int = 0
)

@Dao
interface SubTaskDao {
    @Query("SELECT * FROM sub_tasks WHERE taskId = :taskId ORDER BY sortOrder")
    fun getForTask(taskId: Long): Flow<List<SubTask>>

    @Query("SELECT * FROM sub_tasks WHERE taskId = :taskId ORDER BY sortOrder")
    suspend fun getForTaskSync(taskId: Long): List<SubTask>

    @Insert
    suspend fun insert(subTask: SubTask): Long

    @Update
    suspend fun update(subTask: SubTask)

    @Delete
    suspend fun delete(subTask: SubTask)

    @Query("DELETE FROM sub_tasks WHERE taskId = :taskId")
    suspend fun deleteAllForTask(taskId: Long)
}
