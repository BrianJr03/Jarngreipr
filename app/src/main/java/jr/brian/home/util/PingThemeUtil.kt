package jr.brian.home.util

import android.os.Build
import androidx.compose.ui.graphics.toArgb
import jr.brian.home.ui.theme.ColorTheme
import jr.brian.ping.PingProfile
import jr.brian.ping.PingValue
import jr.brian.ping.asString
import jr.brian.ping.toPingValue

object PingThemeUtil {
    const val PROFILE_DISPLAY_NAME = "ColorTheme"

    // Marker stored in customData so filtering doesn't depend on displayName
    // (displayName is now the user's chosen broadcast name)
    const val MARKER_KEY = "_t"
    private const val MARKER_VALUE = "1"

    // Single compact key — all themes packed into one string value
    // Format per theme: "id|name|#primaryHex|#secondaryHex|isSolid"
    // Themes separated by ";"
    // One theme ≈ 65 bytes → 7 themes safely fit within the 511-byte ATT limit
    private const val KEY_THEMES = "t"
    private const val THEME_SEP = ";"
    private const val FIELD_SEP = "|"

    fun buildProfile(theme: ColorTheme, displayName: String = PROFILE_DISPLAY_NAME): PingProfile =
        buildProfile(listOf(theme), displayName)

    fun buildProfile(themes: List<ColorTheme>, displayName: String = PROFILE_DISPLAY_NAME): PingProfile {
        val packed = themes.joinToString(THEME_SEP) { theme ->
            val pri = String.format("#%08X", theme.primaryColor.toArgb())
            val sec = String.format("#%08X", theme.secondaryColor.toArgb())
            "${theme.id}$FIELD_SEP${theme.customName ?: ""}$FIELD_SEP$pri$FIELD_SEP$sec$FIELD_SEP${theme.isSolid}"
        }
        return PingProfile(
            userId = Build.MODEL,
            displayName = displayName,
            customData = mapOf(
                MARKER_KEY to MARKER_VALUE.toPingValue(),
                KEY_THEMES to packed.toPingValue()
            )
        )
    }

    fun isThemeProfile(customData: Map<String, PingValue>): Boolean =
        customData[MARKER_KEY]?.asString() == MARKER_VALUE

    fun parseThemes(customData: Map<String, PingValue>): List<ColorTheme> {
        val packed = customData[KEY_THEMES]?.asString() ?: return emptyList()
        if (packed.isBlank()) return emptyList()
        return packed.split(THEME_SEP).mapNotNull { entry ->
            val parts = entry.split(FIELD_SEP)
            if (parts.size < 5) return@mapNotNull null
            runCatching {
                ColorTheme.fromCustomData(
                    id = parts[0],
                    name = parts[1],
                    primaryColorHex = parts[2],
                    secondaryColorHex = parts[3],
                    isSolid = parts[4].toBoolean()
                )
            }.getOrNull()
        }
    }
}
