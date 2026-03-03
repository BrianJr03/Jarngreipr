package jr.brian.home.ui.theme.managers

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import jr.brian.home.data.PingBroadcastManager
import jr.brian.home.ui.theme.ColorTheme
import jr.brian.ping.PingProfile
import jr.brian.ping.PingService
import jr.brian.ping.asBoolean
import jr.brian.ping.asString
import jr.brian.ping.toPingValue

private const val PREFS_NAME = "gaming_launcher_prefs"
private const val KEY_THEME = "selected_theme"
private const val KEY_CUSTOM_THEMES = "custom_themes"
private const val CUSTOM_THEME_SEPARATOR = "|||"
private const val CUSTOM_THEME_FIELD_SEPARATOR = ":::"

class ThemeManager(
    private val context: Context,
    private val pingBroadcastManager: PingBroadcastManager
) {
    private val customThemes = mutableStateOf(loadCustomThemes())

    var currentTheme by mutableStateOf(loadTheme())
        private set

    val allThemes: List<ColorTheme>
        get() = ColorTheme.presetThemes + customThemes.value

    private val pingListener: (String, PingProfile) -> Unit = { _, remoteProfile ->
        if (remoteProfile.displayName == "ColorTheme") {
            val id = remoteProfile.customData["id"]?.asString()
            val name = remoteProfile.customData["name"]?.asString()
            val primaryColorHex = remoteProfile.customData["primaryColor"]?.asString()
            val secondaryColorHex = remoteProfile.customData["secondaryColor"]?.asString()
            val isSolid = remoteProfile.customData["isSolid"]?.asBoolean()

            if (id != null && name != null && primaryColorHex != null && secondaryColorHex != null && isSolid != null) {
                val newTheme = ColorTheme.fromCustomData(
                    id = id,
                    name = name,
                    primaryColorHex = primaryColorHex,
                    secondaryColorHex = secondaryColorHex,
                    isSolid = isSolid
                )
                addCustomTheme(newTheme)
            }
        }
    }

    init {
        pingBroadcastManager.addListener(pingListener)
    }

    private fun loadTheme(): ColorTheme {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeId =
            prefs.getString(KEY_THEME, ColorTheme.PINK_VIOLET.id)
                ?: ColorTheme.PINK_VIOLET.id

        // Check if it's a custom theme
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

            // If the deleted theme was selected, switch to default
            if (currentTheme.id == theme.id) {
                setTheme(ColorTheme.PINK_VIOLET)
            }
        }
    }

    fun shareCustomTheme(theme: ColorTheme) {
        if (!theme.isCustom) return

        val primaryColorHex = String.format("#%08X", theme.primaryColor.toArgb())
        val secondaryColorHex = String.format("#%08X", theme.secondaryColor.toArgb())

        val profile = PingProfile(
            displayName = "Samsung",
            customData = mapOf(
                "id" to theme.id.toPingValue(),
                "name" to (theme.customName ?: "").toPingValue(),
                "primaryColor" to primaryColorHex.toPingValue(),
                "secondaryColor" to secondaryColorHex.toPingValue(),
                "isSolid" to theme.isSolid.toPingValue()
            )
        )

        val intent = PingService.buildIntent(context, profile)
        context.startForegroundService(intent)
    }

    fun shareAllCustomThemes() {
        customThemes.value.forEach { shareCustomTheme(it) }
    }

    fun stopSharing() {
        context.stopService(Intent(context, PingService::class.java))
    }

    fun cleanup() {
        pingBroadcastManager.removeListener(pingListener)
    }
}

val LocalThemeManager =
    compositionLocalOf<ThemeManager> {
        error("ThemeManager not provided")
    }