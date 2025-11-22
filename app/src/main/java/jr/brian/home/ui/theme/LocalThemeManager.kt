package jr.brian.home.ui.theme

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

private const val PREFS_NAME = "gaming_launcher_prefs"
private const val KEY_THEME = "selected_theme"

class ThemeManager(
    private val context: Context,
) {
    var currentTheme by mutableStateOf(loadTheme())
        private set

    private fun loadTheme(): ColorTheme {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeId =
            prefs.getString(KEY_THEME, ColorTheme.PINK_VIOLET.id) ?: ColorTheme.PINK_VIOLET.id
        return ColorTheme.fromId(themeId)
    }

    fun setTheme(theme: ColorTheme) {
        currentTheme = theme
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_THEME, theme.id) }
    }
}

val LocalThemeManager =
    compositionLocalOf<ThemeManager> {
        error("ThemeManager not provided")
    }