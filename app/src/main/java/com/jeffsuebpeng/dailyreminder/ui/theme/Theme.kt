package com.jeffsuebpeng.dailyreminder.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

object ThemePrefs {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_DARK_MODE = "dark_mode"

    fun isDarkMode(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_DARK_MODE, true)

    fun setDarkMode(context: Context, dark: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_DARK_MODE, dark).apply()
    }
}

private val DailyReminderDarkColors = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color.White,
    secondary = AccentBlue,
    onSecondary = Color.White,
    background = AppBackgroundBottom,
    onBackground = TextPrimary,
    surface = Color(0xFF10152A),
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF1A2038),
    onSurfaceVariant = TextSecondary,
    secondaryContainer = Color(0xFF16324A),
    onSecondaryContainer = AccentCyan,
    error = Color(0xFFFF8A80),
    onError = Color.White
)

private val DailyReminderLightColors = lightColorScheme(
    primary = Color(0xFF1E88E5),
    onPrimary = Color.White,
    secondary = Color(0xFF0A84FF),
    onSecondary = Color.White,
    background = Color(0xFFEDF0F7),
    onBackground = Color(0xFF15151A),
    surface = Color.White,
    onSurface = Color(0xFF15151A),
    surfaceVariant = Color(0xFFF1F2F8),
    onSurfaceVariant = Color(0xFF5B6478),
    secondaryContainer = Color(0xFFE3F1FF),
    onSecondaryContainer = Color(0xFF0A55E0),
    error = Color(0xFFD32F2F),
    onError = Color.White
)

@Composable
fun DailyReminderTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var isDark by remember { mutableStateOf(ThemePrefs.isDarkMode(context)) }
    isDark = ThemePrefs.isDarkMode(context)

    MaterialTheme(
        colorScheme = if (isDark) DailyReminderDarkColors else DailyReminderLightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
