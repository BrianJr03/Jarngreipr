package jr.brian.home.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jr.brian.home.data.PingBroadcastManager
import jr.brian.home.ui.theme.ColorTheme
import jr.brian.ping.PingProfile
import jr.brian.ping.asBoolean
import jr.brian.ping.asString
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
                
                val displayName = remoteProfile.displayName ?: "Unknown User"
                _receivedThemes.update { currentMap ->
                    val userThemes = currentMap[displayName].orEmpty()
                    if (userThemes.any { it.id == newTheme.id }) {
                        currentMap
                    } else {
                        currentMap + (displayName to (userThemes + newTheme))
                    }
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