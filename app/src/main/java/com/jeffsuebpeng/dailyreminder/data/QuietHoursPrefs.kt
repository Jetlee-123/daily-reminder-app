package com.jeffsuebpeng.dailyreminder.data

import android.content.Context
import java.util.Calendar

object QuietHoursPrefs {
    private const val PREFS_NAME = "quiet_hours_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_START_HOUR = "start_hour"
    private const val KEY_START_MINUTE = "start_minute"
    private const val KEY_END_HOUR = "end_hour"
    private const val KEY_END_MINUTE = "end_minute"

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getStart(context: Context): Pair<Int, Int> {
        val p = prefs(context)
        return p.getInt(KEY_START_HOUR, 22) to p.getInt(KEY_START_MINUTE, 0)
    }

    fun getEnd(context: Context): Pair<Int, Int> {
        val p = prefs(context)
        return p.getInt(KEY_END_HOUR, 7) to p.getInt(KEY_END_MINUTE, 0)
    }

    fun setRange(context: Context, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        prefs(context).edit()
            .putInt(KEY_START_HOUR, startHour)
            .putInt(KEY_START_MINUTE, startMinute)
            .putInt(KEY_END_HOUR, endHour)
            .putInt(KEY_END_MINUTE, endMinute)
            .apply()
    }

    fun isWithinQuietHours(context: Context, calendar: Calendar = Calendar.getInstance()): Boolean {
        if (!isEnabled(context)) return false
        val (startH, startM) = getStart(context)
        val (endH, endM) = getEnd(context)
        val nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinutes = startH * 60 + startM
        val endMinutes = endH * 60 + endM
        if (startMinutes == endMinutes) return false
        return if (startMinutes < endMinutes) {
            nowMinutes in startMinutes until endMinutes
        } else {
            nowMinutes >= startMinutes || nowMinutes < endMinutes
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
