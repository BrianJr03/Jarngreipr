package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import jr.brian.core.model.MainScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the user-controlled "Home Button Interception" toggle plus the
 * [mainScreen] preference consumed by `BOTH` routing.
 *
 * The interception path is opt-in and defaults to `false`. When enabled, the
 * accessibility service intercepts hardware `KEYCODE_HOME` presses and routes
 * them according to a gesture. Each gesture targets a *slot* (bottom / top /
 * both) rather than a specific activity, so the behaviour stays consistent
 * after the user swaps which screen hosts which app:
 *
 *  - single press  → restore whichever activity owns the bottom slot
 *  - double press  → restore whichever activity owns the top slot
 *  - long press    → restore both slots (coerced to bottom-only when the
 *                    frontend feature is off)
 *
 * [mainScreen] applies only to the long-press ("Both") routing and decides
 * which display `MainActivity` occupies — and therefore which display
 * receives input focus once both activities are up.
 */
class HomeButtonManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _interceptionEnabled = MutableStateFlow(loadInterceptionEnabled())
    val interceptionEnabled: StateFlow<Boolean> = _interceptionEnabled.asStateFlow()

    private val _mainScreen = MutableStateFlow(loadMainScreen())
    val mainScreen: StateFlow<MainScreen> = _mainScreen.asStateFlow()

    private fun loadInterceptionEnabled(): Boolean =
        prefs.getBoolean(KEY_INTERCEPTION_ENABLED, DEFAULT_INTERCEPTION_ENABLED)

    private fun loadMainScreen(): MainScreen =
        MainScreen.fromNameOrDefault(prefs.getString(KEY_MAIN_SCREEN, null))

    fun setInterceptionEnabled(enabled: Boolean) {
        _interceptionEnabled.value = enabled
        prefs.edit().apply {
            putBoolean(KEY_INTERCEPTION_ENABLED, enabled)
            apply()
        }
    }

    fun setMainScreen(screen: MainScreen) {
        _mainScreen.value = screen
        prefs.edit().apply {
            putString(KEY_MAIN_SCREEN, screen.name)
            apply()
        }
    }

    companion object {
        const val DEFAULT_INTERCEPTION_ENABLED = false

        private const val PREFS_NAME = "home_button_prefs"
        private const val KEY_INTERCEPTION_ENABLED = "home_interception_enabled"
        private const val KEY_MAIN_SCREEN = "home_main_screen"
    }
}