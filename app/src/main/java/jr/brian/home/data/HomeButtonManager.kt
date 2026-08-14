package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import jr.brian.home.model.HomeTarget
import jr.brian.home.model.MainScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the user-controlled "Home Button Interception" toggle and the
 * companion display-target selector.
 *
 * The interception path is opt-in and defaults to `false`. When enabled, the
 * accessibility service intercepts hardware `KEYCODE_HOME` presses and calls
 * [jr.brian.home.ui.util.routeHome] with the target below. When disabled the
 * service falls through to the platform's default Home handling.
 *
 * ### Target selector
 *
 * [homeTarget] decides which display(s) a Home press wakes:
 *
 *  - [HomeTarget.BOTTOM] — `MainActivity` on the external/bottom display only.
 *    Matches the launcher's pre-selector behaviour when the frontend feature
 *    is disabled.
 *  - [HomeTarget.TOP] — `MainActivity` on the primary/top display only.
 *  - [HomeTarget.BOTH] — `MainActivity` on one display and `FrontEndActivity`
 *    on the other; requires the frontend feature to be enabled.
 *
 * [mainScreen] applies only when [HomeTarget.BOTH] is active and decides which
 * display MainActivity occupies (and therefore which display receives input
 * focus once both activities are up).
 *
 * ### Migration
 *
 * Users who upgrade from a pre-selector build have neither value persisted.
 * The pre-selector `routeHome()` fired MainActivity on the bottom and — when
 * the frontend was enabled — FrontEndActivity on the top. [resolveHomeTarget]
 * reproduces that exactly: [HomeTarget.BOTH] when `frontendEnabled` is true at
 * read time, otherwise [HomeTarget.BOTTOM]. Once the user picks a value via
 * Settings the stored choice takes over and this fallback is no longer
 * consulted.
 *
 * Stored in SharedPreferences to match the other feature managers in this
 * package ([QuickDeleteManager] is the DataStore-backed exception; the
 * SharedPreferences pattern used here is by far the more common convention
 * across `data/`).
 */
class HomeButtonManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _interceptionEnabled = MutableStateFlow(loadInterceptionEnabled())
    val interceptionEnabled: StateFlow<Boolean> = _interceptionEnabled.asStateFlow()

    private val _homeTarget = MutableStateFlow(loadStoredHomeTarget())
    /**
     * Raw persisted target, or `null` when the user has never picked one.
     * Callers that need a concrete target should use [resolveHomeTarget], which
     * folds in the migration default derived from the frontend-enabled flag.
     */
    val homeTarget: StateFlow<HomeTarget?> = _homeTarget.asStateFlow()

    private val _mainScreen = MutableStateFlow(loadMainScreen())
    val mainScreen: StateFlow<MainScreen> = _mainScreen.asStateFlow()

    private fun loadInterceptionEnabled(): Boolean =
        prefs.getBoolean(KEY_INTERCEPTION_ENABLED, DEFAULT_INTERCEPTION_ENABLED)

    private fun loadStoredHomeTarget(): HomeTarget? =
        prefs.getString(KEY_HOME_TARGET, null)?.let { name ->
            HomeTarget.entries.firstOrNull { it.name == name }
        }

    private fun loadMainScreen(): MainScreen =
        MainScreen.fromNameOrDefault(prefs.getString(KEY_MAIN_SCREEN, null))

    /**
     * Persists the toggle. The service reads this value on every key event, so
     * the change is picked up on the next hardware Home press with no further
     * wiring.
     */
    fun setInterceptionEnabled(enabled: Boolean) {
        _interceptionEnabled.value = enabled
        prefs.edit().apply {
            putBoolean(KEY_INTERCEPTION_ENABLED, enabled)
            apply()
        }
    }

    fun setHomeTarget(target: HomeTarget) {
        _homeTarget.value = target
        prefs.edit().apply {
            putString(KEY_HOME_TARGET, target.name)
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

    /**
     * Returns the target to use for a Home press right now.
     *
     *  - If the user has persisted a choice, that choice is returned as-is —
     *    with one coercion: [HomeTarget.BOTH] collapses to [HomeTarget.BOTTOM]
     *    when [frontendEnabled] is false, since BOTH's second activity is
     *    FrontEndActivity and it cannot be shown while disabled.
     *  - Otherwise the pre-selector default is returned: [HomeTarget.BOTH]
     *    when [frontendEnabled] is true (matches the old dual-launch path) or
     *    [HomeTarget.BOTTOM] when it is false (matches the old single launch).
     */
    fun resolveHomeTarget(frontendEnabled: Boolean): HomeTarget {
        val stored = _homeTarget.value
        val effective = stored ?: if (frontendEnabled) HomeTarget.BOTH else HomeTarget.BOTTOM
        return if (effective == HomeTarget.BOTH && !frontendEnabled) HomeTarget.BOTTOM else effective
    }

    companion object {
        const val DEFAULT_INTERCEPTION_ENABLED = false

        private const val PREFS_NAME = "home_button_prefs"
        private const val KEY_INTERCEPTION_ENABLED = "home_interception_enabled"
        private const val KEY_HOME_TARGET = "home_target"
        private const val KEY_MAIN_SCREEN = "home_main_screen"
    }
}
