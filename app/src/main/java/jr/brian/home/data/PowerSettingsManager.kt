package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
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

    private val _wakeMethod = MutableStateFlow(loadWakeMethod())
    val wakeMethod: StateFlow<WakeMethod> = _wakeMethod.asStateFlow()

    private fun loadPowerButtonVisibility(): Boolean {
        return prefs.getBoolean(KEY_POWER_BUTTON_VISIBLE, false)
    }

    private fun loadQuickDeleteVisibility(): Boolean {
        return prefs.getBoolean(KEY_QUICK_DELETE_VISIBLE, false)
    }

    private fun loadWakeMethod(): WakeMethod {
        val methodName = prefs.getString(KEY_WAKE_METHOD, WakeMethod.SINGLE_TAP.name)
        return try {
            WakeMethod.valueOf(methodName ?: WakeMethod.SINGLE_TAP.name)
        } catch (_: IllegalArgumentException) {
            WakeMethod.SINGLE_TAP
        }
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

    fun setWakeMethod(method: WakeMethod) {
        _wakeMethod.value = method
        prefs.edit().apply {
            putString(KEY_WAKE_METHOD, method.name)
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "power_settings_prefs"
        private const val KEY_POWER_BUTTON_VISIBLE = "power_button_visible"
        private const val KEY_QUICK_DELETE_VISIBLE = "quick_delete_visible"
        private const val KEY_WAKE_METHOD = "wake_method"
    }
}
