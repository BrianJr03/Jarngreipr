package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the "Auto-manage Focus Lock" toggle, which gates whether we
 * write to AYN's `Settings.System.screen_focus_lock` on screen swaps,
 * app launches, game launches, and boot.
 *
 * When ON (default), [ScreenFocusLockWriter] pins AYN's Focus Lock to
 * whichever display the user is interacting with (the frontend after a
 * swap, the launched app on tap, PRIMARY while a game is running). When
 * OFF, we never touch the setting and the user manages it manually via
 * AYN's Dual Screen Assistant tile.
 */
class FocusLockPrefsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _autoManageEnabled = MutableStateFlow(loadAutoManage())
    val autoManageEnabled: StateFlow<Boolean> = _autoManageEnabled.asStateFlow()

    private fun loadAutoManage(): Boolean =
        prefs.getBoolean(KEY_AUTO_MANAGE, DEFAULT_AUTO_MANAGE)

    fun setAutoManageEnabled(enabled: Boolean) {
        _autoManageEnabled.value = enabled
        prefs.edit().apply {
            putBoolean(KEY_AUTO_MANAGE, enabled)
            apply()
        }
    }

    companion object {
        const val DEFAULT_AUTO_MANAGE = true
        private const val PREFS_NAME = "focus_lock_prefs"
        private const val KEY_AUTO_MANAGE = "auto_manage_enabled"
    }
}
