package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import jr.brian.home.model.BackButtonShortcut
import jr.brian.home.model.WakeMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PowerSettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _powerButtonVisible = MutableStateFlow(loadPowerButtonVisibility())
    val powerButtonVisible: StateFlow<Boolean> = _powerButtonVisible.asStateFlow()

    private val _quickDeleteVisible = MutableStateFlow(loadQuickDeleteVisibility())
    val quickDeleteVisible: StateFlow<Boolean> = _quickDeleteVisible.asStateFlow()

    private val _headerVisible = MutableStateFlow(loadHeaderVisibility())
    val headerVisible: StateFlow<Boolean> = _headerVisible.asStateFlow()

    private val _wakeMethod = MutableStateFlow(loadWakeMethod())
    val wakeMethod: StateFlow<WakeMethod> = _wakeMethod.asStateFlow()

    private val _backButtonShortcutEnabled = MutableStateFlow(loadBackButtonShortcutEnabled())
    val backButtonShortcutEnabled: StateFlow<Boolean> = _backButtonShortcutEnabled.asStateFlow()

    private val _backButtonShortcut = MutableStateFlow(loadBackButtonShortcut())
    val backButtonShortcut: StateFlow<BackButtonShortcut> = _backButtonShortcut.asStateFlow()

    private val _backButtonShortcutAppPackage = MutableStateFlow(loadBackButtonShortcutAppPackage())
    val backButtonShortcutAppPackage: StateFlow<String?> = _backButtonShortcutAppPackage.asStateFlow()

    private fun loadPowerButtonVisibility(): Boolean {
        return prefs.getBoolean(KEY_POWER_BUTTON_VISIBLE, false)
    }

    private fun loadQuickDeleteVisibility(): Boolean {
        return prefs.getBoolean(KEY_QUICK_DELETE_VISIBLE, false)
    }

    private fun loadHeaderVisibility(): Boolean {
        return prefs.getBoolean(KEY_HEADER_VISIBLE, true)
    }

    private fun loadWakeMethod(): WakeMethod {
        val methodName = prefs.getString(KEY_WAKE_METHOD, WakeMethod.SINGLE_TAP.name)
        return try {
            WakeMethod.valueOf(methodName ?: WakeMethod.SINGLE_TAP.name)
        } catch (_: IllegalArgumentException) {
            WakeMethod.SINGLE_TAP
        }
    }

    private fun loadBackButtonShortcutEnabled(): Boolean {
        return prefs.getBoolean(KEY_BACK_BUTTON_SHORTCUT_ENABLED, false)
    }

    private fun loadBackButtonShortcut(): BackButtonShortcut {
        val shortcutName = prefs.getString(KEY_BACK_BUTTON_SHORTCUT, BackButtonShortcut.NONE.name)
        return try {
            BackButtonShortcut.valueOf(shortcutName ?: BackButtonShortcut.NONE.name)
        } catch (_: IllegalArgumentException) {
            BackButtonShortcut.NONE
        }
    }

    private fun loadBackButtonShortcutAppPackage(): String? {
        return prefs.getString(KEY_BACK_BUTTON_SHORTCUT_APP_PACKAGE, null)
    }

    fun setPowerButtonVisibility(visible: Boolean) {
        _powerButtonVisible.value = visible
        prefs.edit().apply {
            putBoolean(KEY_POWER_BUTTON_VISIBLE, visible)
            apply()
        }
    }

    fun setQuickDeleteVisibility(visible: Boolean) {
        _quickDeleteVisible.value = visible
        prefs.edit().apply {
            putBoolean(KEY_QUICK_DELETE_VISIBLE, visible)
            apply()
        }
    }

    fun setHeaderVisibility(visible: Boolean) {
        _headerVisible.value = visible
        prefs.edit().apply {
            putBoolean(KEY_HEADER_VISIBLE, visible)
            apply()
        }
    }

    fun setWakeMethod(method: WakeMethod) {
        _wakeMethod.value = method
        prefs.edit().apply {
            putString(KEY_WAKE_METHOD, method.name)
            apply()
        }
    }

    fun setBackButtonShortcutEnabled(enabled: Boolean) {
        _backButtonShortcutEnabled.value = enabled
        prefs.edit().apply {
            putBoolean(KEY_BACK_BUTTON_SHORTCUT_ENABLED, enabled)
            apply()
        }
    }

    fun setBackButtonShortcut(shortcut: BackButtonShortcut) {
        _backButtonShortcut.value = shortcut
        prefs.edit().apply {
            putString(KEY_BACK_BUTTON_SHORTCUT, shortcut.name)
            apply()
        }
    }

    fun setBackButtonShortcutAppPackage(packageName: String?) {
        _backButtonShortcutAppPackage.value = packageName
        prefs.edit().apply {
            putString(KEY_BACK_BUTTON_SHORTCUT_APP_PACKAGE, packageName)
            apply()
        }
    }

    fun resetBackButtonShortcut() {
        _backButtonShortcut.value = BackButtonShortcut.NONE
        _backButtonShortcutAppPackage.value = null
        prefs.edit().apply {
            putString(KEY_BACK_BUTTON_SHORTCUT, BackButtonShortcut.NONE.name)
            putString(KEY_BACK_BUTTON_SHORTCUT_APP_PACKAGE, null)
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "power_settings_prefs"
        private const val KEY_POWER_BUTTON_VISIBLE = "power_button_visible"
        private const val KEY_QUICK_DELETE_VISIBLE = "quick_delete_visible"
        private const val KEY_HEADER_VISIBLE = "header_visible"
        private const val KEY_WAKE_METHOD = "wake_method"
        private const val KEY_BACK_BUTTON_SHORTCUT_ENABLED = "back_button_shortcut_enabled"
        private const val KEY_BACK_BUTTON_SHORTCUT = "back_button_shortcut"
        private const val KEY_BACK_BUTTON_SHORTCUT_APP_PACKAGE = "back_button_shortcut_app_package"
    }
}
