package jr.brian.home.data

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.ui.theme.ColorTheme
import jr.brian.home.util.PingThemeUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "gaming_launcher_prefs"
private const val KEY_SHARED_THEMES = "shared_themes"
private const val RECORD_SEP = "|||"
private const val FIELD_SEP = ":::"

@Singleton
class ThemeShareRepository @Inject constructor(
    pingBroadcastManager: PingBroadcastManager,
    @param:ApplicationContext private val context: Context
) {
    private val _receivedThemes = MutableStateFlow(loadSharedThemes())
    val receivedThemes = _receivedThemes.asStateFlow()

    private val pingListener: PingListener = { _, remoteProfile ->
        if (PingThemeUtil.isThemeProfile(remoteProfile.customData)) {
            val newThemes = PingThemeUtil.parseThemes(remoteProfile.customData)
            val senderName = remoteProfile.displayName.ifBlank { remoteProfile.userId }
            if (newThemes.isNotEmpty()) {
                _receivedThemes.update { currentMap ->
                    val existing = currentMap[senderName].orEmpty()
                    val newOnly = newThemes.filter { new -> existing.none { it.id == new.id } }
                    if (newOnly.isEmpty()) currentMap
                    else {
                        val updated = currentMap + (senderName to (existing + newOnly))
                        saveSharedThemes(updated)
                        updated
                    }
                }
            }
        }
    }

    fun deleteSharedTheme(sender: String, themeId: String) {
        _receivedThemes.update { currentMap ->
            val themes = currentMap[sender]?.filter { it.id != themeId }.orEmpty()
            val updated = if (themes.isEmpty()) currentMap - sender
                          else currentMap + (sender to themes)
            saveSharedThemes(updated)
            updated
        }
    }

    init {
        pingBroadcastManager.addListener(pingListener)
    }

    private fun loadSharedThemes(): Map<String, List<ColorTheme>> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SHARED_THEMES, "") ?: ""
        if (raw.isBlank()) return emptyMap()

        val map = mutableMapOf<String, MutableList<ColorTheme>>()
        raw.split(RECORD_SEP).forEach { record ->
            val parts = record.split(FIELD_SEP)
            if (parts.size >= 6) {
                try {
                    val sender = parts[0]
                    val theme = ColorTheme.fromCustomData(
                        id = parts[1],
                        name = parts[2],
                        primaryColorHex = parts[3],
                        secondaryColorHex = parts[4],
                        isSolid = parts[5].toBoolean()
                    )
                    map.getOrPut(sender) { mutableListOf() }.add(theme)
                } catch (_: Exception) {}
            }
        }
        return map
    }

    private fun saveSharedThemes(map: Map<String, List<ColorTheme>>) {
        val raw = map.flatMap { (sender, themes) ->
            themes.map { theme ->
                val primary = String.format("#%08X", theme.primaryColor.toArgb())
                val secondary = String.format("#%08X", theme.secondaryColor.toArgb())
                "$sender$FIELD_SEP${theme.id}$FIELD_SEP${theme.customName.orEmpty()}$FIELD_SEP$primary$FIELD_SEP$secondary$FIELD_SEP${theme.isSolid}"
            }
        }.joinToString(RECORD_SEP)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_SHARED_THEMES, raw) }
    }
}
