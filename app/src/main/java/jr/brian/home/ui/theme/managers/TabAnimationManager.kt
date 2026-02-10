package jr.brian.home.ui.theme.managers

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

private const val PREFS_NAME = "gaming_launcher_prefs"
private const val KEY_TAB_ANIMATION = "tab_animation_enabled"

class TabAnimationManager(
    private val context: Context,
) {
    var isTabAnimationEnabled by mutableStateOf(loadTabAnimation())
        private set

    private fun loadTabAnimation(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_TAB_ANIMATION, false) // Default to disabled
    }

    fun setTabAnimation(enabled: Boolean) {
        isTabAnimationEnabled = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_TAB_ANIMATION, enabled) }
    }

    fun toggleTabAnimation() {
        setTabAnimation(!isTabAnimationEnabled)
    }
}
