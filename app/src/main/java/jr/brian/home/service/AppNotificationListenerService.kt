package jr.brian.home.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.media.session.MediaSession
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.data.NotificationCountManager
import jr.brian.home.data.NowPlayingManager
import javax.inject.Inject

/**
 * Service that listens for notification events and updates the NotificationCountManager.
 * User must grant notification access permission for this service to work.
 * 
 * Uses Hilt for dependency injection - the NotificationCountManager is injected
 * as a singleton and shared across the entire application.
 */
@AndroidEntryPoint
class AppNotificationListenerService : NotificationListenerService() {
    
    @Inject
    lateinit var notificationCountManager: NotificationCountManager

    @Inject
    lateinit var nowPlayingManager: NowPlayingManager

    override fun onListenerConnected() {
        super.onListenerConnected()
        refreshNotificationCounts()
        refreshMediaSession()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        refreshNotificationCounts()
        val token = extractMediaToken(sbn) ?: return
        nowPlayingManager.onMediaSession(token)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        refreshNotificationCounts()
        if (extractMediaToken(sbn) != null) {
            nowPlayingManager.onMediaSessionRemoved(hasRemainingMediaSessions())
        }
    }
    
    private fun extractMediaToken(sbn: StatusBarNotification?): MediaSession.Token? {
        sbn ?: return null
        return sbn.notification.extras.getParcelable(
            Notification.EXTRA_MEDIA_SESSION,
            MediaSession.Token::class.java
        )
    }

    private fun hasRemainingMediaSessions(): Boolean {
        return try {
            activeNotifications?.any { extractMediaToken(it) != null } ?: false
        } catch (_: Exception) { false }
    }

    private fun refreshMediaSession() {
        try {
            val token = activeNotifications
                ?.firstNotNullOfOrNull { extractMediaToken(it) }
                ?: return
            nowPlayingManager.onMediaSession(token)
        } catch (_: Exception) {}
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
