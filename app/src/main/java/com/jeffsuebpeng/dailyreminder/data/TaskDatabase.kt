package com.jeffsuebpeng.dailyreminder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Task::class, CompletionLog::class, SubTask::class], version = 6, exportSchema = false)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun completionLogDao(): CompletionLogDao
    abstract fun subTaskDao(): SubTaskDao

    companion object {
        @Volatile private var INSTANCE: TaskDatabase? = null

        fun getDatabase(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "task_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
