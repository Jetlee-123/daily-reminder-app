package com.jeffsuebpeng.dailyreminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Locale

val CANONICAL_DAY_ORDER = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
private val ISO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val isRecurring: Boolean,
    val recurrenceDays: String = "",
    val dueDateMillis: Long? = null,
    val reminderTimes: String = "09:00",
    val isCompleted: Boolean = false,
    val lastCompletedDateMillis: Long? = null,
    val colorHex: String = "#409CFF",
    val leadMinutes: Int = 0,
    val tags: String = "",
    val streak: Int = 0,
    val skippedDates: String = "",
    val notifyEnabled: Boolean = true
) {
    fun parsedTimes(): List<Pair<Int, Int>> {
        return reminderTimes.split(",")
            .filter { it.isNotBlank() }
            .map { part ->
                val (h, m) = part.trim().split(":").map { it.toInt() }
                h to m
            }
            .sortedBy { it.first * 60 + it.second }
            .ifEmpty { listOf(9 to 0) }
    }

    fun sortedDays(): List<String> {
        return recurrenceDays.split(",")
            .filter { it.isNotBlank() }
            .sortedBy { CANONICAL_DAY_ORDER.indexOf(it) }
    }

    fun parsedTags(): List<String> = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
    fun parsedSkippedDates(): Set<String> = skippedDates.split(",").filter { it.isNotBlank() }.toSet()
    fun isDateSkipped(millis: Long): Boolean = ISO_DATE_FORMAT.format(millis) in parsedSkippedDates()

    fun leadLabel(): String? {
        if (leadMinutes <= 0) return null
        return when {
            leadMinutes % 1440 == 0 -> { val d = leadMinutes / 1440; if (d == 1) "1 day before" else "$d days before" }
            leadMinutes % 60 == 0 -> { val h = leadMinutes / 60; if (h == 1) "1 hour before" else "$h hours before" }
            else -> "$leadMinutes min before"
        }
    }

    companion object {
        fun timesToString(times: List<Pair<Int, Int>>): String =
            times.sortedBy { it.first * 60 + it.second }.joinToString(",") { (h, m) -> String.format("%02d:%02d", h, m) }

        fun daysToString(days: Collection<String>): String =
            days.sortedBy { CANONICAL_DAY_ORDER.indexOf(it) }.joinToString(",")

        fun tagsToString(tags: Collection<String>): String = tags.joinToString(",")
        fun isoDateOf(millis: Long): String = ISO_DATE_FORMAT.format(millis)
    }
}
