package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The four behaviours the "Focus Lock mode" picker exposes in Extras:
 *  - [Off]: writer never fires. User manages AYN's Focus Lock tile
 *    themselves.
 *  - [Top]: all sync points write 1 (Top) regardless of context —
 *    forces gamepad onto the primary display at all times.
 *  - [Bottom]: all sync points write 2 (Bottom) regardless of context.
 *  - [Follow]: sync points write the display that matches context —
 *    frontend's assigned side after a swap, launched app's target
 *    display on tap, PRIMARY while a game is running, and back to the
 *    frontend's resting position whenever both of our activities
 *    regain focus. Default.
 */
enum class FocusLockMode {
    Off,
    Top,
    Bottom,
    Follow;

    companion object {
        val DEFAULT = Follow

        fun fromNameOrDefault(name: String?): FocusLockMode =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/**
 * Persists the user's [FocusLockMode] choice. Writes to AYN's
 * `screen_focus_lock` route through Shizuku, see
 * [ScreenFocusLockWriter].
 */
class FocusLockPrefsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(loadMode())
    val mode: StateFlow<FocusLockMode> = _mode.asStateFlow()

    private fun loadMode(): FocusLockMode {
        // Migrate the pre-v31 boolean (true -> Follow, false -> Off) on
        // first read if the new string key isn't set yet.
        val stored = prefs.getString(KEY_MODE, null)
        if (stored != null) return FocusLockMode.fromNameOrDefault(stored)
        if (prefs.contains(LEGACY_KEY_AUTO_MANAGE)) {
            val legacy = prefs.getBoolean(LEGACY_KEY_AUTO_MANAGE, true)
            val migrated = if (legacy) FocusLockMode.Follow else FocusLockMode.Off
            prefs.edit().putString(KEY_MODE, migrated.name).apply()
            return migrated
        }
        return FocusLockMode.DEFAULT
    }

    fun setMode(mode: FocusLockMode) {
        _mode.value = mode
        prefs.edit().apply {
            putString(KEY_MODE, mode.name)
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "focus_lock_prefs"
        private const val KEY_MODE = "focus_lock_mode"
        private const val LEGACY_KEY_AUTO_MANAGE = "auto_manage_enabled"
    }
}
