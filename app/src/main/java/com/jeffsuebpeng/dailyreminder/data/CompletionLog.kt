package com.jeffsuebpeng.dailyreminder.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "completion_log")
data class CompletionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val completedAtMillis: Long
)

@Dao
interface CompletionLogDao {
    @Insert
    suspend fun insert(log: CompletionLog)

    @Query("SELECT * FROM completion_log WHERE taskId = :taskId ORDER BY completedAtMillis DESC")
    fun getLogsForTask(taskId: Long): Flow<List<CompletionLog>>

    @Query("SELECT * FROM completion_log")
    suspend fun getAllLogs(): List<CompletionLog>

    @Query("DELETE FROM completion_log WHERE taskId = :taskId")
    suspend fun deleteLogsForTask(taskId: Long)
}
