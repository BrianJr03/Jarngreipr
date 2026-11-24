package jr.brian.home.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "update_prefs"
private const val KEY_SKIPPED_VERSION = "skipped_version"
private const val KEY_DOWNLOADED_VERSION = "downloaded_version"
private const val KEY_RC_NOTIFICATIONS = "rc_notifications_enabled"

/**
 * Manager for app update related preferences and state.
 */
@Singleton
class AppUpdateManager @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    private val appPrefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Observable toggle for the "Get notified about RC builds" setting.
     * When on, update checks include pre-release GitHub releases (RC tags).
     * Default off so stable channel users don't see RC prompts.
     */
    private var _rcNotificationsEnabled: Boolean by mutableStateOf(
        appPrefs.getBoolean(KEY_RC_NOTIFICATIONS, false)
    )
    val rcNotificationsEnabled: Boolean
        get() = _rcNotificationsEnabled

    fun setRcNotificationsEnabled(enabled: Boolean) {
        if (_rcNotificationsEnabled == enabled) return
        _rcNotificationsEnabled = enabled
        appPrefs.edit { putBoolean(KEY_RC_NOTIFICATIONS, enabled) }
    }
    
    /**
     * Check if a version has been skipped by the user.
     */
    fun isVersionSkipped(context: Context, version: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SKIPPED_VERSION, null) == version
    }

    /**
     * Mark a version as skipped.
     */
    fun skipVersion(context: Context, version: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_SKIPPED_VERSION, version) }
    }

    /**
     * Clear the skipped version (used when manually checking for updates).
     */
    fun clearSkippedVersion(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { remove(KEY_SKIPPED_VERSION) }
    }
    
    /**
     * Check if a version has already been downloaded.
     */
    fun isVersionDownloaded(context: Context, version: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DOWNLOADED_VERSION, null) == version
    }

    /**
     * Mark a version as downloaded (called when download completes).
     */
    fun markVersionDownloaded(context: Context, version: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_DOWNLOADED_VERSION, version) }
    }

    /**
     * Clear the downloaded version marker.
     */
    fun clearDownloadedVersion(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { remove(KEY_DOWNLOADED_VERSION) }
    }
    
    /**
     * Check if an update dialog should be shown for a version.
     * Returns false if version was skipped or already downloaded.
     */
    fun shouldShowUpdateDialog(context: Context, version: String): Boolean {
        return !isVersionSkipped(context, version) && !isVersionDownloaded(context, version)
    }
}
