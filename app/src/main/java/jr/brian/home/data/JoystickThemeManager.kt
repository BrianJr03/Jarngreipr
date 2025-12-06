package jr.brian.home.data

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val PREFS_NAME = "gaming_launcher_prefs"
private const val KEY_JOYSTICK_THEME_ENABLED = "joystick_theme_enabled"
private const val KEY_JOYSTICK_FLASHING_ENABLED = "joystick_flashing_enabled"
private const val KEY_JOYSTICK_FLASH_SPEED = "joystick_flash_speed"

class JoystickThemeManager(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.Default)
    private var flashingJob: Job? = null

    private val _isThemeEnabled = MutableStateFlow(loadThemeEnabled())
    val isThemeEnabled: StateFlow<Boolean> = _isThemeEnabled.asStateFlow()

    private val _isFlashingEnabled = MutableStateFlow(loadFlashingEnabled())
    val isFlashingEnabled: StateFlow<Boolean> = _isFlashingEnabled.asStateFlow()

    private val _flashSpeed = MutableStateFlow(loadFlashSpeed())
    val flashSpeed: StateFlow<FlashSpeed> = _flashSpeed.asStateFlow()

    private fun loadThemeEnabled(): Boolean {
        return prefs.getBoolean(KEY_JOYSTICK_THEME_ENABLED, false)
    }

    private fun loadFlashingEnabled(): Boolean {
        return prefs.getBoolean(KEY_JOYSTICK_FLASHING_ENABLED, false)
    }

    private fun loadFlashSpeed(): FlashSpeed {
        val speedValue = prefs.getInt(KEY_JOYSTICK_FLASH_SPEED, FlashSpeed.MEDIUM.value)
        return FlashSpeed.fromValue(speedValue)
    }

    fun setThemeEnabled(enabled: Boolean) {
        _isThemeEnabled.value = enabled
        prefs.edit { putBoolean(KEY_JOYSTICK_THEME_ENABLED, enabled) }

        if (!enabled) {
            stopFlashing()
            clearControllerLights()
        }
    }

    fun setFlashingEnabled(enabled: Boolean) {
        _isFlashingEnabled.value = enabled
        prefs.edit { putBoolean(KEY_JOYSTICK_FLASHING_ENABLED, enabled) }

        if (enabled && _isThemeEnabled.value) {
            startFlashing()
        } else {
            stopFlashing()
        }
    }

    fun setFlashSpeed(speed: FlashSpeed) {
        _flashSpeed.value = speed
        prefs.edit { putInt(KEY_JOYSTICK_FLASH_SPEED, speed.value) }

        if (_isFlashingEnabled.value && _isThemeEnabled.value) {
            stopFlashing()
            startFlashing()
        }
    }

    fun applyThemeColors(primaryColor: Color, secondaryColor: Color) {
        if (!_isThemeEnabled.value) return

        if (_isFlashingEnabled.value) {
            startFlashing(primaryColor, secondaryColor)
        } else {
            setControllerLight(primaryColor)
        }
    }

    private fun startFlashing(primaryColor: Color? = null, secondaryColor: Color? = null) {
        stopFlashing()

        flashingJob = scope.launch {
            val primary = primaryColor ?: Color.White
            val secondary = secondaryColor ?: Color.White
            val delayMs = _flashSpeed.value.delayMs

            while (isActive) {
                setControllerLight(primary)
                delay(delayMs)
                if (!isActive) break
                setControllerLight(secondary)
                delay(delayMs)
            }
        }
    }

    private fun stopFlashing() {
        flashingJob?.cancel()
        flashingJob = null
    }

    private fun setControllerLight(color: Color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val lightsManagerClass = Class.forName("android.hardware.lights.LightsManager")
                val lightsManager = context.getSystemService(lightsManagerClass)

                if (lightsManager != null) {
                    val getLightsMethod = lightsManagerClass.getMethod("getLights")
                    val lights = getLightsMethod.invoke(lightsManager) as? List<*>

                    lights?.forEach { light ->
                        try {
                            val lightClass = Class.forName("android.hardware.lights.Light")
                            val getTypeMethod = lightClass.getMethod("getType")
                            val hasRgbControlMethod = lightClass.getMethod("hasRgbControl")

                            val type = getTypeMethod.invoke(light) as? Int
                            val hasRgb = hasRgbControlMethod.invoke(light) as? Boolean

                            val LIGHT_TYPE_PLAYER_ID = 10

                            if (type == LIGHT_TYPE_PLAYER_ID || hasRgb == true) {
                                val lightStateBuilderClass =
                                    Class.forName("android.hardware.lights.LightState\$Builder")
                                val lightStateBuilder =
                                    lightStateBuilderClass.getDeclaredConstructor().newInstance()

                                val setColorMethod =
                                    lightStateBuilderClass.getMethod("setColor", Int::class.java)
                                setColorMethod.invoke(lightStateBuilder, color.toArgb())

                                val buildMethod = lightStateBuilderClass.getMethod("build")
                                val lightState = buildMethod.invoke(lightStateBuilder)

                                val openSessionMethod = lightsManagerClass.getMethod("openSession")
                                val session = openSessionMethod.invoke(lightsManager)

                                if (session != null) {
                                    val sessionClass =
                                        Class.forName("android.hardware.lights.LightsManager\$LightsSession")
                                    val requestBuilderClass =
                                        Class.forName("android.hardware.lights.LightsManager\$LightsSession\$LightsRequest\$Builder")
                                    val requestBuilder =
                                        requestBuilderClass.getDeclaredConstructor().newInstance()

                                    val lightStateClass =
                                        Class.forName("android.hardware.lights.LightState")
                                    val addLightMethod = requestBuilderClass.getMethod(
                                        "addLight",
                                        lightClass,
                                        lightStateClass
                                    )
                                    addLightMethod.invoke(requestBuilder, light, lightState)

                                    val buildRequestMethod = requestBuilderClass.getMethod("build")
                                    val request = buildRequestMethod.invoke(requestBuilder)

                                    val requestClass =
                                        Class.forName("android.hardware.lights.LightsManager\$LightsSession\$LightsRequest")
                                    val requestLightsMethod =
                                        sessionClass.getMethod("requestLights", requestClass)
                                    requestLightsMethod.invoke(session, request)

                                    val closeMethod = sessionClass.getMethod("close")
                                    closeMethod.invoke(session)
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun clearControllerLights() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setControllerLight(Color.Black)
        }
    }

    fun cleanup() {
        stopFlashing()
        if (_isThemeEnabled.value) {
            clearControllerLights()
        }
    }

    enum class FlashSpeed(val value: Int, val delayMs: Long) {
        SLOW(0, 1000L),
        MEDIUM(1, 500L),
        FAST(2, 250L);

        companion object {
            fun fromValue(value: Int): FlashSpeed {
                return entries.find { it.value == value } ?: MEDIUM
            }
        }
    }
}
