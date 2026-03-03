package jr.brian.home.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jr.brian.home.data.PingBroadcastManager
import jr.brian.home.ui.theme.ColorTheme
import jr.brian.home.util.PingThemeUtil
import jr.brian.ping.PingProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ReceivedThemesViewModel @Inject constructor(
    private val pingBroadcastManager: PingBroadcastManager
) : ViewModel() {

    private val _receivedThemes = MutableStateFlow<Map<String, List<ColorTheme>>>(emptyMap())
    val receivedThemes = _receivedThemes.asStateFlow()

    private val pingListener: (String, PingProfile) -> Unit = { _, remoteProfile ->
        if (PingThemeUtil.isThemeProfile(remoteProfile.customData)) {
            val newThemes = PingThemeUtil.parseThemes(remoteProfile.customData)
            val senderName = remoteProfile.displayName.ifBlank { remoteProfile.userId }
            if (newThemes.isNotEmpty()) {
                _receivedThemes.update { currentMap ->
                    val existing = currentMap[senderName].orEmpty()
                    val merged = existing + newThemes.filter { new ->
                        existing.none { it.id == new.id }
                    }
                    currentMap + (senderName to merged)
                }
            }
        }
    }

    init {
        pingBroadcastManager.addListener(pingListener)
    }

    override fun onCleared() {
        super.onCleared()
        pingBroadcastManager.removeListener(pingListener)
    }
}
