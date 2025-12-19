package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WhatsNewManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _shouldShowWhatsNew = MutableStateFlow(false)
    val shouldShowWhatsNew: StateFlow<Boolean> = _shouldShowWhatsNew.asStateFlow()

    fun checkAndShowWhatsNew(currentVersionName: String) {
        val lastSeenVersion = prefs.getString(KEY_LAST_SEEN_VERSION, "") ?: ""
        _shouldShowWhatsNew.value = currentVersionName != lastSeenVersion
    }

    fun markWhatsNewAsSeen(versionName: String) {
        _shouldShowWhatsNew.value = false
        prefs.edit().apply {
            putString(KEY_LAST_SEEN_VERSION, versionName)
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "whats_new_prefs"
        private const val KEY_LAST_SEEN_VERSION = "last_seen_version"
    }
}
