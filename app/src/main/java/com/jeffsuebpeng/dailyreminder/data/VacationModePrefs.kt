package com.jeffsuebpeng.dailyreminder.data

import android.content.Context

object VacationModePrefs {
    private const val PREFS_NAME = "vacation_mode_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_START_MILLIS = "start_millis"
    private const val KEY_END_MILLIS = "end_millis"

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setRange(context: Context, startMillis: Long, endMillis: Long) {
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, true)
            .putLong(KEY_START_MILLIS, startMillis)
            .putLong(KEY_END_MILLIS, endMillis)
            .apply()
    }

    fun disable(context: Context) {
        prefs(context).edit().putBoolean(KEY_ENABLED, false).apply()
    }

    fun getRange(context: Context): Pair<Long, Long>? {
        val p = prefs(context)
        if (!p.contains(KEY_START_MILLIS)) return null
        return p.getLong(KEY_START_MILLIS, 0L) to p.getLong(KEY_END_MILLIS, 0L)
    }

    fun isCurrentlyPaused(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        if (!isEnabled(context)) return false
        val (start, end) = getRange(context) ?: return false
        return now in start..end
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
