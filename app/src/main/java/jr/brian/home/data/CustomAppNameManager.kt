package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CustomAppNameManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _customNames = MutableStateFlow(loadAllCustomNames())
    val customNames: StateFlow<Map<String, String>> = _customNames.asStateFlow()

    private fun loadAllCustomNames(): Map<String, String> {
        return prefs.all
            .filterKeys { it.startsWith(KEY_PREFIX) }
            .mapKeys { it.key.removePrefix(KEY_PREFIX) }
            .mapValues { it.value as String }
    }

    fun setCustomName(packageName: String, customName: String) {
        if (customName.isBlank()) {
            removeCustomName(packageName)
            return
        }
        prefs.edit().putString(KEY_PREFIX + packageName, customName.trim()).apply()
        _customNames.value = loadAllCustomNames()
    }

    fun getCustomName(packageName: String): String? {
        return prefs.getString(KEY_PREFIX + packageName, null)
    }

    fun removeCustomName(packageName: String) {
        prefs.edit().remove(KEY_PREFIX + packageName).apply()
        _customNames.value = loadAllCustomNames()
    }

    companion object {
        private const val PREFS_NAME = "custom_app_names"
        private const val KEY_PREFIX = "name_"
    }
}
