package jr.brian.home.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Container for system integration and user experience managers.
 * 
 * These managers handle system-level integration and UX features:
 * - Notification tracking
 * - Privileged operations (Shizuku)
 * - Update checking
 * - Onboarding and what's new dialogs
 */
@Singleton
data class SystemManagers @Inject constructor(
    /** Tracks notification counts per app (requires notification access) */
    val notificationCountManager: NotificationCountManager,
    
    /** Integrates with Shizuku for privileged operations */
    val shizukuManager: ShizukuManager,
    
    /** Checks for launcher updates */
    val appUpdateManager: AppUpdateManager,
    
    /** Manages first-time user onboarding flow */
    val onboardingManager: OnboardingManager,
    
    /** Tracks whether to show "What's New" dialog after updates */
    val whatsNewManager: WhatsNewManager,

    /** Manages broadcast communication with the Ping companion app */
    val pingBroadcastManager: PingBroadcastManager
) {
    init {
        // Initialize Shizuku early to prepare for privileged operations
        // This must happen before any features that depend on Shizuku
        shizukuManager.initialize()
    }
}
