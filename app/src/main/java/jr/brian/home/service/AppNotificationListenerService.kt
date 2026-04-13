package jr.brian.home.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.data.NotificationCountManager
import jr.brian.home.data.NotificationItem
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

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        refreshAll()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        refreshAll()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        refreshAll()
    }

    private fun refreshAll() {
        try {
            val active = activeNotifications ?: return
            val pm = applicationContext.packageManager
            val items = mutableListOf<NotificationItem>()

            for (sbn in active) {
                if (sbn.isOngoing) continue
                if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) continue

                val pkg = sbn.packageName
                val appLabel = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (_: Exception) { pkg }

                val extras = sbn.notification.extras
                val title = extras.getString(Notification.EXTRA_TITLE)
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

                items.add(
                    NotificationItem(
                        key = sbn.key,
                        packageName = pkg,
                        appLabel = appLabel,
                        title = title,
                        text = text,
                        postTime = sbn.postTime
                    )
                )
            }

            items.sortByDescending { it.postTime }
            notificationCountManager.updateActiveNotifications(items)
        } catch (_: Exception) {
            // Silently fail - notification access might not be granted
        }
    }

    companion object {
        private var instance: AppNotificationListenerService? = null

        fun cancel(key: String) {
            instance?.cancelNotification(key)
        }

        fun cancelAll() {
            instance?.cancelAllNotifications()
        }

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
