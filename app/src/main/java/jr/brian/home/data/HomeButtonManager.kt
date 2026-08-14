package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the user-controlled "Home Button Interception" toggle.
 *
 * The interception path is opt-in and defaults to `false`. When enabled, the
 * accessibility service intercepts hardware `KEYCODE_HOME` presses and routes
 * [jr.brian.home.MainActivity] to the external display. When disabled the service
 * falls through to the platform's default Home handling.
 *
 * Stored in SharedPreferences to match the other feature managers in this package
 * ([QuickDeleteManager] is the DataStore-backed exception; the SharedPreferences
 * pattern used here is by far the more common convention across `data/`).
 */
class HomeButtonManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _interceptionEnabled = MutableStateFlow(loadInterceptionEnabled())
    val interceptionEnabled: StateFlow<Boolean> = _interceptionEnabled.asStateFlow()

    private fun loadInterceptionEnabled(): Boolean {
        return prefs.getBoolean(KEY_INTERCEPTION_ENABLED, DEFAULT_INTERCEPTION_ENABLED)
    }

    /**
     * Persists the toggle. The service reads this value on every key event, so the
     * change is picked up on the next hardware Home press with no further wiring.
     */
    fun setInterceptionEnabled(enabled: Boolean) {
        _interceptionEnabled.value = enabled
        prefs.edit().apply {
            putBoolean(KEY_INTERCEPTION_ENABLED, enabled)
            apply()
        }
    }

    companion object {
        const val DEFAULT_INTERCEPTION_ENABLED = false

        private const val PREFS_NAME = "home_button_prefs"
        private const val KEY_INTERCEPTION_ENABLED = "home_interception_enabled"
    }
}
