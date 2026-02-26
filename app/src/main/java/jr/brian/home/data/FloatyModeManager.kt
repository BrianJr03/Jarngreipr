package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/**
 * Manages the "Floaty Mode" easter egg state.
 *
 * The easter egg is **unlocked** by tapping the build version text 7 times
 * in Settings (like Android developer options). Once unlocked a toggle
 * appears in the Extras section. The toggle defaults to **false** so the
 * user must explicitly enable it.
 *
 * When active, apps in FreePositionedAppsLayout float around the screen
 * with physics-based bouncing and collision detection.
 */
class FloatyModeManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Whether the user has unlocked the easter egg (tapped version 7×). */
    var isUnlocked by mutableStateOf(prefs.getBoolean(KEY_UNLOCKED, false))
        private set

    /** Whether the floating-apps effect is currently turned on. */
    var isFloatyModeActive by mutableStateOf(prefs.getBoolean(KEY_FLOATY_MODE, false))
        private set
    
    /** Tabs where floaty mode is enabled. Empty means all tabs. */
    var enabledTabs by mutableStateOf(loadEnabledTabs())
        private set

    /** Call once when the user taps the version 7 times. */
    fun unlock() {
        if (!isUnlocked) {
            isUnlocked = true
            prefs.edit { putBoolean(KEY_UNLOCKED, true) }
        }
    }

    fun setFloatyMode(enabled: Boolean) {
        isFloatyModeActive = enabled
        prefs.edit { putBoolean(KEY_FLOATY_MODE, enabled) }
    }
    
    fun setTabEnabled(
        pageIndex: Int,
        enabled: Boolean,
        totalPages: Int
    ) {
        val currentTabs = enabledTabs.toMutableSet()
        if (currentTabs.isEmpty()) {
            repeat(totalPages) { index ->
                currentTabs.add(index)
            }
        }
        if (enabled) {
            currentTabs.add(pageIndex)
        } else {
            currentTabs.remove(pageIndex)
        }
        enabledTabs = currentTabs
        saveEnabledTabs(currentTabs)
    }
    
    fun isTabEnabled(pageIndex: Int): Boolean {
        return enabledTabs.isEmpty() || enabledTabs.contains(pageIndex)
    }
    
    fun isFloatyEnabledOnTab(pageIndex: Int): Boolean {
        return isFloatyModeActive && isTabEnabled(pageIndex)
    }
    
    /** Resets the easter egg state; user must tap version 7x again. */
    fun reset() {
        isFloatyModeActive = false
        isUnlocked = false
        enabledTabs = emptySet()
        prefs.edit {
            putBoolean(KEY_FLOATY_MODE, false)
                .putBoolean(KEY_UNLOCKED, false)
                .remove(KEY_ENABLED_TABS)
        }
    }
    
    private fun loadEnabledTabs(): Set<Int> {
        val tabsString = prefs.getString(KEY_ENABLED_TABS, null)
        return if (tabsString.isNullOrBlank()) {
            emptySet()
        } else {
            tabsString.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        }
    }
    
    private fun saveEnabledTabs(tabs: Set<Int>) {
        val tabsString = tabs.joinToString(",")
        prefs.edit { putString(KEY_ENABLED_TABS, tabsString) }
    }

    companion object {
        private const val PREFS_NAME = "floaty_mode_prefs"
        private const val KEY_UNLOCKED = "floaty_mode_unlocked"
        private const val KEY_FLOATY_MODE = "floaty_mode_active"
        private const val KEY_ENABLED_TABS = "floaty_mode_enabled_tabs"
    }
}
