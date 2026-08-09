package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages preferences for which display each app should launch on.
 * PRIMARY_DISPLAY = Launch on the primary (top) display
 * CURRENT_DISPLAY = Launch on the current display (default behavior)
 */
class AppDisplayPreferenceManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setAppDisplayPreference(packageName: String, preference: DisplayPreference) {
        prefs.edit().apply {
            putString(KEY_PREFIX + packageName, preference.name)
            apply()
        }
    }

    fun getAppDisplayPreference(packageName: String): DisplayPreference {
        val prefString =
            prefs.getString(KEY_PREFIX + packageName, DisplayPreference.CURRENT_DISPLAY.name)
        return try {
            DisplayPreference.valueOf(prefString ?: DisplayPreference.CURRENT_DISPLAY.name)
        } catch (_: IllegalArgumentException) {
            DisplayPreference.CURRENT_DISPLAY
        }
    }

    @Suppress("unused")
    fun clearAppDisplayPreference(packageName: String) {
        prefs.edit().apply {
            remove(KEY_PREFIX + packageName)
            apply()
        }
    }

    fun getAllPreferences(): Map<String, String> {
        return prefs.all
            .filterKeys { it.startsWith(KEY_PREFIX) }
            .mapKeys { it.key.removePrefix(KEY_PREFIX) }
            .mapValues { it.value as? String ?: DisplayPreference.CURRENT_DISPLAY.name }
    }

    fun restoreAllPreferences(preferences: Map<String, String>) {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(KEY_PREFIX) }.forEach { editor.remove(it) }
        preferences.forEach { (pkg, prefName) -> editor.putString(KEY_PREFIX + pkg, prefName) }
        editor.apply()
    }

    /**
     * When true, tapping [packageName] surfaces a chooser dialog so the user
     * picks the display for that launch; the stored [DisplayPreference] is used
     * as the fallback (single-display devices) and is not modified by the pick.
     */
    fun getPromptForDisplayOnLaunch(packageName: String): Boolean {
        return prefs.getBoolean(PROMPT_PREFIX + packageName, false)
    }

    fun setPromptForDisplayOnLaunch(packageName: String, enabled: Boolean) {
        prefs.edit().apply {
            if (enabled) putBoolean(PROMPT_PREFIX + packageName, true)
            else remove(PROMPT_PREFIX + packageName)
            apply()
        }
    }

    /** Packages with the prompt-on-launch flag enabled. Used by backup export. */
    fun getPromptForDisplayPackages(): Set<String> {
        return prefs.all
            .filterKeys { it.startsWith(PROMPT_PREFIX) }
            .filterValues { it as? Boolean == true }
            .keys
            .mapTo(mutableSetOf()) { it.removePrefix(PROMPT_PREFIX) }
    }

    fun restorePromptForDisplayPackages(packages: Set<String>) {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(PROMPT_PREFIX) }.forEach { editor.remove(it) }
        packages.forEach { pkg -> editor.putBoolean(PROMPT_PREFIX + pkg, true) }
        editor.apply()
    }

    enum class DisplayPreference {
        CURRENT_DISPLAY,
        PRIMARY_DISPLAY
    }

    companion object {
        private const val PREFS_NAME = "app_display_prefs"
        private const val KEY_PREFIX = "display_pref_"
        private const val PROMPT_PREFIX = "prompt_display_"
    }
}
