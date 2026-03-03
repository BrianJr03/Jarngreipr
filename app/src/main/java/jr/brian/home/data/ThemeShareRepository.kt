package jr.brian.home.data

import jr.brian.home.ui.theme.ColorTheme
import jr.brian.home.util.PingThemeUtil
import jr.brian.ping.PingProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeShareRepository @Inject constructor(
    pingBroadcastManager: PingBroadcastManager
) {
    private val _receivedThemes = MutableStateFlow<Map<String, List<ColorTheme>>>(emptyMap())
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
                    else currentMap + (senderName to (existing + newOnly))
                }
            }
        }
    }

    init {
        pingBroadcastManager.addListener(pingListener)
    }
}
