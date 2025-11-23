package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppVisibilityManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _hiddenApps = MutableStateFlow(loadHiddenApps())
    val hiddenApps = _hiddenApps.asStateFlow()

    var currentHiddenApps by mutableStateOf(loadHiddenApps())
        private set

    private fun loadHiddenApps(): Set<String> {
        val hiddenAppsString = prefs.getString(KEY_HIDDEN_APPS, "") ?: ""
        return if (hiddenAppsString.isEmpty()) {
            emptySet()
        } else {
            hiddenAppsString.split(SEPARATOR).toSet()
        }
    }

    fun hideApp(packageName: String) {
        val updated = currentHiddenApps + packageName
        saveHiddenApps(updated)
    }

    fun showApp(packageName: String) {
        val updated = currentHiddenApps - packageName
        saveHiddenApps(updated)
    }

    fun hideAllApps(packageNames: List<String>) {
        val updated = currentHiddenApps + packageNames.toSet()
        saveHiddenApps(updated)
    }

    fun showAllApps(packageNames: List<String>) {
        val updated = currentHiddenApps - packageNames.toSet()
        saveHiddenApps(updated)
    }

    fun isAppHidden(packageName: String): Boolean {
        return packageName in currentHiddenApps
    }

    private fun saveHiddenApps(hiddenApps: Set<String>) {
        currentHiddenApps = hiddenApps
        _hiddenApps.value = hiddenApps
        prefs.edit().apply {
            putString(KEY_HIDDEN_APPS, hiddenApps.joinToString(SEPARATOR))
            commit()
        }
    }

    companion object {
        private const val PREFS_NAME = "app_visibility_prefs"
        private const val KEY_HIDDEN_APPS = "hidden_apps"
        private const val SEPARATOR = ","
    }
}
