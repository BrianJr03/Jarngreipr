package jr.brian.home.ui.theme.managers

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import jr.brian.home.R
import jr.brian.home.ui.theme.ColorTheme
import jr.brian.home.util.PingThemeUtil
import jr.brian.ping.PingService
import jr.brian.ping.PingUtil.hasPingPermissions

private const val PREFS_NAME = "gaming_launcher_prefs"
private const val KEY_THEME = "selected_theme"
private const val KEY_CUSTOM_THEMES = "custom_themes"
private const val KEY_PING_AUTO_START = "ping_auto_start"
private const val KEY_PING_DISPLAY_NAME = "ping_display_name"
private const val CUSTOM_THEME_SEPARATOR = "|||"
private const val CUSTOM_THEME_FIELD_SEPARATOR = ":::"

class ThemeManager(private val context: Context) {
    private val customThemes = mutableStateOf(loadCustomThemes())

    var currentTheme by mutableStateOf(loadTheme())
        private set

    val allThemes: List<ColorTheme>
        get() = ColorTheme.presetThemes + customThemes.value

    var isPingAutoStart by mutableStateOf(loadPingAutoStart())
        private set

    var pingDisplayName by mutableStateOf(loadPingDisplayName())
        private set

    private fun loadTheme(): ColorTheme {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeId = prefs.getString(KEY_THEME, ColorTheme.PINK_VIOLET.id) ?: ColorTheme.PINK_VIOLET.id

        if (themeId.startsWith(ColorTheme.CUSTOM_THEME_PREFIX)) {
            val customThemesList = loadCustomThemes()
            return customThemesList.find { it.id == themeId } ?: ColorTheme.PINK_VIOLET
        }

        return ColorTheme.fromId(themeId)
    }

    private fun loadCustomThemes(): List<ColorTheme> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customThemesString = prefs.getString(KEY_CUSTOM_THEMES, "") ?: ""

        if (customThemesString.isEmpty()) return emptyList()

        return customThemesString.split(CUSTOM_THEME_SEPARATOR).mapNotNull { themeData ->
            try {
                val parts = themeData.split(CUSTOM_THEME_FIELD_SEPARATOR)
                if (parts.size >= 5) {
                    ColorTheme.fromCustomData(
                        id = parts[0],
                        name = parts[1],
                        primaryColorHex = parts[2],
                        secondaryColorHex = parts[3],
                        isSolid = parts[4].toBoolean()
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun loadPingAutoStart(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PING_AUTO_START, false)
    }

    fun updatePingAutoStart(enabled: Boolean) {
        isPingAutoStart = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_PING_AUTO_START, enabled) }
    }

    private fun loadPingDisplayName(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PING_DISPLAY_NAME, "") ?: ""
    }

    fun updatePingDisplayName(name: String) {
        pingDisplayName = name
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_PING_DISPLAY_NAME, name) }
    }

    private fun saveCustomThemes() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customThemesString = customThemes.value.joinToString(CUSTOM_THEME_SEPARATOR) { theme ->
            val primaryHex = String.format("#%08X", theme.primaryColor.toArgb())
            val secondaryHex = String.format("#%08X", theme.secondaryColor.toArgb())
            "${theme.id}$CUSTOM_THEME_FIELD_SEPARATOR${theme.customName}$CUSTOM_THEME_FIELD_SEPARATOR$primaryHex$CUSTOM_THEME_FIELD_SEPARATOR$secondaryHex$CUSTOM_THEME_FIELD_SEPARATOR${theme.isSolid}"
        }
        prefs.edit { putString(KEY_CUSTOM_THEMES, customThemesString) }
    }

    fun setTheme(theme: ColorTheme) {
        currentTheme = theme
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_THEME, theme.id) }
        if (isPingAutoStart && context.hasPingPermissions()) shareCurrentTheme()
    }

    fun addCustomTheme(theme: ColorTheme) {
        if (theme.isCustom) {
            if (customThemes.value.any { it.id == theme.id }) return
            customThemes.value += theme
            saveCustomThemes()
        }
    }

    fun deleteCustomTheme(theme: ColorTheme) {
        if (theme.isCustom) {
            customThemes.value = customThemes.value.filter { it.id != theme.id }
            saveCustomThemes()

            if (currentTheme.id == theme.id) {
                setTheme(ColorTheme.PINK_VIOLET)
            }
        }
    }

    fun shareCurrentTheme() {
        PingService.notificationTitle = context.getString(R.string.ping_notification_title)
        val profile = PingThemeUtil.buildProfile(
            currentTheme,
            pingDisplayName.ifBlank { android.os.Build.MODEL }
        )
        val intent = PingService.buildIntent(context, profile)
        context.startForegroundService(intent)
    }

    fun stopSharing() {
        context.stopService(Intent(context, PingService::class.java))
    }
}

val LocalThemeManager =
    compositionLocalOf<ThemeManager> {
        error("ThemeManager not provided")
    }
