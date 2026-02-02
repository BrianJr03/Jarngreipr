package jr.brian.home.esde.setup

import android.content.Context
import android.content.SharedPreferences

class SetupPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "SetupPrefs", Context.MODE_PRIVATE
    )

    var setupCompleted: Boolean
        get() = prefs.getBoolean("setup_completed", false)
        set(value) = prefs.edit().putBoolean("setup_completed", value).apply()

    var scriptsPath: String
        get() = prefs.getString("scripts_path", DEFAULT_SCRIPTS_PATH) ?: DEFAULT_SCRIPTS_PATH
        set(value) = prefs.edit().putString("scripts_path", value).apply()

    var mediaPath: String
        get() = prefs.getString("media_path", DEFAULT_MEDIA_PATH) ?: DEFAULT_MEDIA_PATH
        set(value) = prefs.edit().putString("media_path", value).apply()

    companion object {
        const val DEFAULT_SCRIPTS_PATH = "/storage/emulated/0/ES-DE/scripts"
        const val DEFAULT_MEDIA_PATH = "/storage/emulated/0/ES-DE/downloaded_media"
    }
}
