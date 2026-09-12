package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the "Select + Volume adjusts the bottom screen" chord toggle.
 *
 * Opt-in and defaults to `false`, matching the [HomeButtonManager] pattern.
 * When enabled, [jr.brian.home.service.HomeInterceptorService] runs the chord
 * filter on every key event and routes matching presses to
 * [jr.brian.home.util.ThorVolume.adjustBottom]. When disabled the filter falls
 * through immediately so Volume keys behave exactly as they do on stock.
 */
class VolumeChordManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _chordEnabled = MutableStateFlow(loadChordEnabled())
    val chordEnabled: StateFlow<Boolean> = _chordEnabled.asStateFlow()

    private fun loadChordEnabled(): Boolean =
        prefs.getBoolean(KEY_CHORD_ENABLED, DEFAULT_CHORD_ENABLED)

    fun setChordEnabled(enabled: Boolean) {
        _chordEnabled.value = enabled
        prefs.edit().apply {
            putBoolean(KEY_CHORD_ENABLED, enabled)
            apply()
        }
    }

    companion object {
        const val DEFAULT_CHORD_ENABLED = false

        private const val PREFS_NAME = "volume_chord_prefs"
        private const val KEY_CHORD_ENABLED = "volume_chord_enabled"
    }
}
