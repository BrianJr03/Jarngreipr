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

    private val _hiddenAppsByPage = MutableStateFlow<Map<Int, Set<String>>>(emptyMap())
    val hiddenAppsByPage = _hiddenAppsByPage.asStateFlow()

    private val _newAppsVisibleByDefault = MutableStateFlow(loadNewAppsVisibleByDefault())
    val newAppsVisibleByDefault = _newAppsVisibleByDefault.asStateFlow()

    var showAppNames by mutableStateOf(loadShowAppNames())
        private set

    var showFolderNames by mutableStateOf(loadShowFolderNames())
        private set

    var showSettingsBackButton by mutableStateOf(loadShowSettingsBackButton())
        private set

    init {
        loadAllPageData()
    }

    private fun loadAllPageData() {
        val maxPages = 10
        val hiddenAppsMap = mutableMapOf<Int, Set<String>>()
        for (pageIndex in 0 until maxPages) {
            val hiddenApps = loadHiddenAppsForPage(pageIndex)
            if (hiddenApps.isNotEmpty()) {
                hiddenAppsMap[pageIndex] = hiddenApps
            }
        }
        _hiddenAppsByPage.value = hiddenAppsMap
    }

    private fun loadHiddenAppsForPage(pageIndex: Int): Set<String> {
        val key = "${KEY_HIDDEN_APPS}_$pageIndex"
        val hiddenAppsString = prefs.getString(key, "") ?: ""
        return if (hiddenAppsString.isEmpty()) {
            emptySet()
        } else {
            hiddenAppsString.split(SEPARATOR).toSet()
        }
    }

    private fun loadNewAppsVisibleByDefault(): Boolean {
        return prefs.getBoolean(KEY_NEW_APPS_VISIBLE_BY_DEFAULT, true)
    }

    private fun loadShowAppNames(): Boolean {
        return prefs.getBoolean(KEY_SHOW_APP_NAMES, false)
    }

    private fun loadShowFolderNames(): Boolean {
        return prefs.getBoolean(KEY_SHOW_FOLDER_NAMES, true)
    }

    private fun loadShowSettingsBackButton(): Boolean {
        return prefs.getBoolean(KEY_SHOW_SETTINGS_BACK_BUTTON, true)
    }

    fun toggleShowAppNames() {
        showAppNames = !showAppNames
        prefs.edit().apply {
            putBoolean(KEY_SHOW_APP_NAMES, showAppNames)
            apply()
        }
    }

    fun toggleShowFolderNames() {
        showFolderNames = !showFolderNames
        prefs.edit().apply {
            putBoolean(KEY_SHOW_FOLDER_NAMES, showFolderNames)
            apply()
        }
    }

    fun toggleShowSettingsBackButton() {
        showSettingsBackButton = !showSettingsBackButton
        prefs.edit().apply {
            putBoolean(KEY_SHOW_SETTINGS_BACK_BUTTON, showSettingsBackButton)
            apply()
        }
    }

    fun setNewAppsVisibleByDefault(visible: Boolean) {
        _newAppsVisibleByDefault.value = visible
        prefs.edit().apply {
            putBoolean(KEY_NEW_APPS_VISIBLE_BY_DEFAULT, visible)
            apply()
        }
    }

    fun getHiddenApps(pageIndex: Int): Set<String> {
        return _hiddenAppsByPage.value[pageIndex] ?: emptySet()
    }

    fun hideApp(pageIndex: Int, packageName: String) {
        val currentHidden = getHiddenApps(pageIndex)
        val updated = currentHidden + packageName
        saveHiddenAppsForPage(pageIndex, updated)
    }

    fun showApp(pageIndex: Int, packageName: String) {
        val currentHidden = getHiddenApps(pageIndex)
        val updated = currentHidden - packageName
        saveHiddenAppsForPage(pageIndex, updated)
    }

    fun hideAllApps(pageIndex: Int, packageNames: List<String>) {
        val currentHidden = getHiddenApps(pageIndex)
        val updated = currentHidden + packageNames.toSet()
        saveHiddenAppsForPage(pageIndex, updated)
    }

    fun showAllApps(pageIndex: Int, packageNames: List<String>) {
        val currentHidden = getHiddenApps(pageIndex)
        val updated = currentHidden - packageNames.toSet()
        saveHiddenAppsForPage(pageIndex, updated)
    }

    fun isAppHidden(pageIndex: Int, packageName: String): Boolean {
        return packageName in getHiddenApps(pageIndex)
    }

    private fun saveHiddenAppsForPage(pageIndex: Int, hiddenApps: Set<String>) {
        val currentMap = _hiddenAppsByPage.value.toMutableMap()
        if (hiddenApps.isEmpty()) {
            currentMap.remove(pageIndex)
        } else {
            currentMap[pageIndex] = hiddenApps
        }
        _hiddenAppsByPage.value = currentMap

        prefs.edit().apply {
            val key = "${KEY_HIDDEN_APPS}_$pageIndex"
            if (hiddenApps.isEmpty()) {
                remove(key)
            } else {
                putString(key, hiddenApps.joinToString(SEPARATOR))
            }
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "app_visibility_prefs"
        private const val KEY_HIDDEN_APPS = "hidden_apps"
        private const val KEY_SHOW_APP_NAMES = "show_app_names"
        private const val KEY_SHOW_FOLDER_NAMES = "show_folder_names"
        private const val KEY_SHOW_SETTINGS_BACK_BUTTON = "show_settings_back_button"
        private const val KEY_NEW_APPS_VISIBLE_BY_DEFAULT = "new_apps_visible_by_default"
        private const val SEPARATOR = ","
    }
}
