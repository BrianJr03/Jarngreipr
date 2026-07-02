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
    
    /** Whether section-header taps should trigger konfetti burst. */
    var isSectionTapKonfettiEnabled by mutableStateOf(
        prefs.getBoolean(KEY_SECTION_TAP_KONFETTI_ENABLED, false)
    )
        private set
    
    /** Whether Powered Off screen should use floaty info effect. */
    var isPoweredOffFloatyEffectEnabled by mutableStateOf(
        prefs.getBoolean(KEY_POWERED_OFF_FLOATY_EFFECT_ENABLED, false)
    )
        private set
    
    /** Whether Apps modal should use floaty layout effect. */
    var isAppsModalFloatyEffectEnabled by mutableStateOf(
        prefs.getBoolean(KEY_APPS_MODAL_FLOATY_EFFECT_ENABLED, false)
    )
        private set
    
    /** Max app count to show in floaty app drawer mode; 0 means all. */
    var appDrawerFloatyAppCount by mutableStateOf(
        prefs.getInt(KEY_APP_DRAWER_FLOATY_APP_COUNT, 0).coerceIn(0, 100)
    )
        private set
    
    /** Whether Apps modal floaty items pop when tapped. */
    var isAppDrawerBubblePopEnabled by mutableStateOf(
        prefs.getBoolean(KEY_APP_DRAWER_BUBBLE_POP_ENABLED, false)
    )
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
    
    fun updateSectionTapKonfettiEffectEnabled(enabled: Boolean) {
        isSectionTapKonfettiEnabled = enabled
        prefs.edit { putBoolean(KEY_SECTION_TAP_KONFETTI_ENABLED, enabled) }
    }
    
    fun updatePoweredOffFloatyEffectEnabled(enabled: Boolean) {
        isPoweredOffFloatyEffectEnabled = enabled
        prefs.edit { putBoolean(KEY_POWERED_OFF_FLOATY_EFFECT_ENABLED, enabled) }
    }
    
    fun updateAppsModalFloatyEffectEnabled(enabled: Boolean) {
        isAppsModalFloatyEffectEnabled = enabled
        prefs.edit { putBoolean(KEY_APPS_MODAL_FLOATY_EFFECT_ENABLED, enabled) }
    }
    
    fun updateAppDrawerFloatyAppCount(count: Int) {
        val coerced = count.coerceIn(0, 100)
        appDrawerFloatyAppCount = coerced
        prefs.edit { putInt(KEY_APP_DRAWER_FLOATY_APP_COUNT, coerced) }
    }
    
    fun updateAppDrawerBubblePopEnabled(enabled: Boolean) {
        isAppDrawerBubblePopEnabled = enabled
        prefs.edit { putBoolean(KEY_APP_DRAWER_BUBBLE_POP_ENABLED, enabled) }
    }
    
    fun restoreEnabledTabs(tabs: Set<Int>) {
        enabledTabs = tabs
        saveEnabledTabs(tabs)
    }

    fun reorderPages(oldIndicesInNewOrder: Map<Int, Int>) {
        val oldTabs = enabledTabs
        if (oldTabs.isEmpty()) return
        val oldToNew = oldIndicesInNewOrder.entries.associate { (newIdx, oldIdx) -> oldIdx to newIdx }
        val newTabs = oldTabs.mapNotNull { oldToNew[it] }.toSet()
        enabledTabs = newTabs
        saveEnabledTabs(newTabs)
    }

    fun removePage(pageIndex: Int) {
        val oldTabs = enabledTabs
        if (oldTabs.isEmpty()) return
        val newTabs = oldTabs.mapNotNull { idx ->
            when {
                idx < pageIndex -> idx
                idx > pageIndex -> idx - 1
                else -> null
            }
        }.toSet()
        enabledTabs = newTabs
        saveEnabledTabs(newTabs)
    }

    /** Resets the easter egg state; user must tap version 7x again. */
    fun reset() {
        isFloatyModeActive = false
        isUnlocked = false
        enabledTabs = emptySet()
        isSectionTapKonfettiEnabled = false
        isPoweredOffFloatyEffectEnabled = false
        isAppsModalFloatyEffectEnabled = false
        appDrawerFloatyAppCount = 0
        isAppDrawerBubblePopEnabled = false
        prefs.edit {
            putBoolean(KEY_FLOATY_MODE, false)
                .putBoolean(KEY_UNLOCKED, false)
                .putBoolean(KEY_SECTION_TAP_KONFETTI_ENABLED, false)
                .putBoolean(KEY_POWERED_OFF_FLOATY_EFFECT_ENABLED, false)
                .putBoolean(KEY_APPS_MODAL_FLOATY_EFFECT_ENABLED, false)
                .putInt(KEY_APP_DRAWER_FLOATY_APP_COUNT, 0)
                .putBoolean(KEY_APP_DRAWER_BUBBLE_POP_ENABLED, false)
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
        private const val KEY_SECTION_TAP_KONFETTI_ENABLED = "section_tap_konfetti_enabled"
        private const val KEY_POWERED_OFF_FLOATY_EFFECT_ENABLED = "powered_off_floaty_effect_enabled"
        private const val KEY_APPS_MODAL_FLOATY_EFFECT_ENABLED = "apps_modal_floaty_effect_enabled"
        private const val KEY_APP_DRAWER_FLOATY_APP_COUNT = "app_drawer_floaty_app_count"
        private const val KEY_APP_DRAWER_BUBBLE_POP_ENABLED = "app_drawer_bubble_pop_enabled"
    }
}
