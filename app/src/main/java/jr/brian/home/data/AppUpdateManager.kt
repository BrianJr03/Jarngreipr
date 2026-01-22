package jr.brian.home.data

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "update_prefs"
private const val KEY_SKIPPED_VERSION = "skipped_version"
private const val KEY_DOWNLOADED_VERSION = "downloaded_version"

/**
 * Manager for app update related preferences and state.
 */
@Singleton
class AppUpdateManager @Inject constructor() {
    
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
        prefs.edit().putString(KEY_SKIPPED_VERSION, version).apply()
    }

    /**
     * Clear the skipped version (used when manually checking for updates).
     */
    fun clearSkippedVersion(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SKIPPED_VERSION).apply()
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
        prefs.edit().putString(KEY_DOWNLOADED_VERSION, version).apply()
    }

    /**
     * Clear the downloaded version marker.
     */
    fun clearDownloadedVersion(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_DOWNLOADED_VERSION).apply()
    }
    
    /**
     * Check if an update dialog should be shown for a version.
     * Returns false if version was skipped or already downloaded.
     */
    fun shouldShowUpdateDialog(context: Context, version: String): Boolean {
        return !isVersionSkipped(context, version) && !isVersionDownloaded(context, version)
    }
}
