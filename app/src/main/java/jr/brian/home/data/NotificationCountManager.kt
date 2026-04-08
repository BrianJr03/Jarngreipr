package jr.brian.home.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "notification_badge_prefs"
private const val KEY_BADGES_VISIBLE = "badges_visible"

/**
 * Manager class that tracks notification counts per app package.
 * Works in conjunction with AppNotificationListenerService to receive updates.
 *
 * This is a singleton scoped to the application lifecycle and is injected
 * via Hilt into both UI components and the NotificationListenerService.
 */
@Singleton
class NotificationCountManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    var badgesVisible by mutableStateOf(loadBadgesVisible())
        private set

    private fun loadBadgesVisible(): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BADGES_VISIBLE, true)
    }

    fun toggleBadgesVisible() {
        badgesVisible = !badgesVisible
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_BADGES_VISIBLE, badgesVisible) }
    }
    
    private val _notificationCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val notificationCounts: StateFlow<Map<String, Int>> = _notificationCounts.asStateFlow()
    
    /**
     * Get the notification count for a specific package.
     */
    fun getNotificationCount(packageName: String): Int {
        return _notificationCounts.value[packageName] ?: 0
    }
    
    /**
     * Update the notification count for a specific package.
     */
    fun setNotificationCount(packageName: String, count: Int) {
        val currentCounts = _notificationCounts.value.toMutableMap()
        if (count > 0) {
            currentCounts[packageName] = count
        } else {
            currentCounts.remove(packageName)
        }
        _notificationCounts.value = currentCounts
    }
    
    /**
     * Update all notification counts at once.
     * This is typically called when the notification listener service starts
     * or when we need to refresh all counts.
     */
    fun updateAllCounts(counts: Map<String, Int>) {
        _notificationCounts.value = counts.filterValues { it > 0 }
    }
    
    /**
     * Clear all notification counts.
     */
    fun clearAll() {
        _notificationCounts.value = emptyMap()
    }
    
    /**
     * Remove notification count for a specific package.
     */
    fun clearForPackage(packageName: String) {
        val currentCounts = _notificationCounts.value.toMutableMap()
        currentCounts.remove(packageName)
        _notificationCounts.value = currentCounts
    }
}
