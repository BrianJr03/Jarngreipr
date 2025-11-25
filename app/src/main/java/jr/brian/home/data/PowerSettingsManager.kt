package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PowerSettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _powerButtonVisible = MutableStateFlow(loadPowerButtonVisibility())
    val powerButtonVisible: StateFlow<Boolean> = _powerButtonVisible.asStateFlow()

    private fun loadPowerButtonVisibility(): Boolean {
        return prefs.getBoolean(KEY_POWER_BUTTON_VISIBLE, false)
    }

    fun setPowerButtonVisibility(visible: Boolean) {
        _powerButtonVisible.value = visible
        prefs.edit().apply {
            putBoolean(KEY_POWER_BUTTON_VISIBLE, visible)
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "power_settings_prefs"
        private const val KEY_POWER_BUTTON_VISIBLE = "power_button_visible"
    }
}
