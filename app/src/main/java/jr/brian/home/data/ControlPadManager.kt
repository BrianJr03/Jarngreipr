package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import jr.brian.home.model.ControlPadItem
import jr.brian.home.model.PhysicalButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class JoystickMode {
    LEFT_ONLY,   // Entire screen controls left joystick (movement)
    RIGHT_ONLY,  // Entire screen controls right joystick (camera)
    LEFT_RIGHT   // Left half = left joystick, Right half = right joystick
}

class ControlPadManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _controlPadItems = MutableStateFlow(loadControlPadItems())
    val controlPadItems: StateFlow<List<ControlPadItem>> = _controlPadItems.asStateFlow()
    
    private val _cameraSensitivity = MutableStateFlow(loadCameraSensitivity())
    val cameraSensitivity: StateFlow<Float> = _cameraSensitivity.asStateFlow()
    
    private val _joystickMode = MutableStateFlow(loadJoystickMode())
    val joystickMode: StateFlow<JoystickMode> = _joystickMode.asStateFlow()
    
    private fun loadCameraSensitivity(): Float {
        return prefs.getFloat(KEY_CAMERA_SENSITIVITY, DEFAULT_CAMERA_SENSITIVITY)
    }
    
    fun setCameraSensitivity(sensitivity: Float) {
        val clamped = sensitivity.coerceIn(MIN_CAMERA_SENSITIVITY, MAX_CAMERA_SENSITIVITY)
        _cameraSensitivity.value = clamped
        prefs.edit().putFloat(KEY_CAMERA_SENSITIVITY, clamped).apply()
    }
    
    private fun loadJoystickMode(): JoystickMode {
        val modeName = prefs.getString(KEY_JOYSTICK_MODE, JoystickMode.RIGHT_ONLY.name)
        return try {
            JoystickMode.valueOf(modeName ?: JoystickMode.RIGHT_ONLY.name)
        } catch (_: IllegalArgumentException) {
            JoystickMode.RIGHT_ONLY
        }
    }
    
    fun setJoystickMode(mode: JoystickMode) {
        _joystickMode.value = mode
        prefs.edit().putString(KEY_JOYSTICK_MODE, mode.name).apply()
    }

    private fun loadControlPadItems(): List<ControlPadItem> {
        return (0 until CONTROL_PAD_COUNT).map { index ->
            val label = prefs.getString("${KEY_ITEM_LABEL}_$index", "${index + 1}") ?: "${index + 1}"
            val buttonName = prefs.getString("${KEY_ITEM_BUTTON}_$index", null)
            val mappedButton = buttonName?.let {
                try {
                    PhysicalButton.valueOf(it)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
            ControlPadItem(label = label, mappedButton = mappedButton)
        }
    }

    fun setControlPadItem(index: Int, item: ControlPadItem) {
        if (index !in 0 until CONTROL_PAD_COUNT) return

        val updatedItems = _controlPadItems.value.toMutableList()
        updatedItems[index] = item
        _controlPadItems.value = updatedItems

        prefs.edit().apply {
            putString("${KEY_ITEM_LABEL}_$index", item.label)
            putString("${KEY_ITEM_BUTTON}_$index", item.mappedButton?.name)
            apply()
        }
    }

    fun resetAllMappings() {
        val defaultItems = (0 until CONTROL_PAD_COUNT).map { index ->
            ControlPadItem(label = "${index + 1}", mappedButton = null)
        }
        _controlPadItems.value = defaultItems

        prefs.edit().apply {
            for (index in 0 until CONTROL_PAD_COUNT) {
                putString("${KEY_ITEM_LABEL}_$index", "${index + 1}")
                remove("${KEY_ITEM_BUTTON}_$index")
            }
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "control_pad_prefs"
        private const val KEY_ITEM_LABEL = "item_label"
        private const val KEY_ITEM_BUTTON = "item_button"
        private const val KEY_CAMERA_SENSITIVITY = "camera_sensitivity"
        private const val KEY_JOYSTICK_MODE = "joystick_mode"
        private const val CONTROL_PAD_COUNT = 6
        
        const val MIN_CAMERA_SENSITIVITY = 0.01f
        const val MAX_CAMERA_SENSITIVITY = 0.25f
        const val DEFAULT_CAMERA_SENSITIVITY = 0.05f
    }
}
