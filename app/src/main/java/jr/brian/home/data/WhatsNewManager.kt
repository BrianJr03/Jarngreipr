package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import jr.brian.home.ui.components.konfetti.KonfettiPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WhatsNewManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _shouldShowWhatsNew = MutableStateFlow(false)
    val shouldShowWhatsNew: StateFlow<Boolean> = _shouldShowWhatsNew.asStateFlow()

    var selectedKonfettiPreset by mutableStateOf(loadKonfettiPreset())
        private set

    fun checkAndShowWhatsNew(currentVersionName: String) {
        val lastSeenVersion = prefs.getString(KEY_LAST_SEEN_VERSION, "") ?: ""
        _shouldShowWhatsNew.value = currentVersionName != lastSeenVersion
    }

    fun markWhatsNewAsSeen(versionName: String) {
        _shouldShowWhatsNew.value = false
        prefs.edit().apply {
            putString(KEY_LAST_SEEN_VERSION, versionName)
            apply()
        }
    }

    fun setKonfettiPreset(preset: KonfettiPreset) {
        selectedKonfettiPreset = preset
        prefs.edit().apply {
            putString(KEY_KONFETTI_PRESET, preset.name)
            apply()
        }
    }

    private fun loadKonfettiPreset(): KonfettiPreset {
        val name = prefs.getString(KEY_KONFETTI_PRESET, KonfettiPreset.EXPLODE.name)
            ?: KonfettiPreset.EXPLODE.name
        return KonfettiPreset.fromName(name)
    }

    companion object {
        private const val PREFS_NAME = "whats_new_prefs"
        private const val KEY_LAST_SEEN_VERSION = "last_seen_version"
        private const val KEY_KONFETTI_PRESET = "konfetti_preset"
    }
}
