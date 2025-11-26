package jr.brian.home.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

private const val PREFS_NAME = "gaming_launcher_prefs"
private const val KEY_OLED_MODE = "oled_mode_enabled"

class OledModeManager(
    private val context: Context,
) {
    var isOledModeEnabled by mutableStateOf(loadOledMode())
        private set

    private fun loadOledMode(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_OLED_MODE, false)
    }

    fun setOledMode(enabled: Boolean) {
        isOledModeEnabled = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_OLED_MODE, enabled) }
    }

    fun toggleOledMode() {
        setOledMode(!isOledModeEnabled)
    }
}
