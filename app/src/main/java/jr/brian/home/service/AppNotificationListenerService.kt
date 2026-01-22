package jr.brian.home.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import jr.brian.home.data.NotificationCountManager

/**
 * Service that listens for notification events and updates the NotificationCountManager.
 * User must grant notification access permission for this service to work.
 */
class AppNotificationListenerService : NotificationListenerService() {
    
    private val notificationCountManager: NotificationCountManager
        get() = NotificationCountManager.getInstance()
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        refreshNotificationCounts()
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        refreshNotificationCounts()
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        refreshNotificationCounts()
    }
    
    /**
     * Refresh all notification counts by querying active notifications.
     */
    private fun refreshNotificationCounts() {
        try {
            val activeNotifications = activeNotifications ?: return
            val counts = mutableMapOf<String, Int>()
            
            for (notification in activeNotifications) {
                // Skip ongoing notifications (like music players, ongoing downloads, etc.)
                if (notification.isOngoing) continue
                
                // Skip group summary notifications to avoid double counting
                if (notification.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY != 0) continue
                
                val packageName = notification.packageName
                counts[packageName] = (counts[packageName] ?: 0) + 1
            }
            
            notificationCountManager.updateAllCounts(counts)
        } catch (e: Exception) {
            // Silently fail - notification access might not be granted
        }
    }
    
    companion object {
        /**
         * Check if notification listener permission is granted.
         */
        fun isNotificationAccessGranted(context: Context): Boolean {
            val componentName = ComponentName(context, AppNotificationListenerService::class.java)
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return enabledListeners?.contains(componentName.flattenToString()) == true
        }
        
        /**
         * Get the intent to open notification access settings.
         */
        fun getNotificationAccessSettingsIntent(): android.content.Intent {
            return android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }
    }
}
