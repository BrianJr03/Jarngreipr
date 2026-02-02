package jr.brian.home.esde.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import jr.brian.home.esde.animation.AnimationStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Suppress("unused")
class ESDEPreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<ESDEPrefsState> = _state.asStateFlow()

    private fun loadState(): ESDEPrefsState {
        val styleName = prefs.getString(KEY_ANIMATION_STYLE, AnimationStyle.Fade.name)
        val animationStyle = try {
            AnimationStyle.valueOf(styleName ?: AnimationStyle.Fade.name)
        } catch (_: IllegalArgumentException) {
            AnimationStyle.Fade
        }
        
        val systemImageTypeName = prefs.getString(KEY_SYSTEM_IMAGE_TYPE, SystemImageType.Fanart.name)
        val systemImageType = try {
            SystemImageType.valueOf(systemImageTypeName ?: SystemImageType.Fanart.name)
        } catch (_: IllegalArgumentException) {
            SystemImageType.Fanart
        }
        
        return ESDEPrefsState(
            animationStyle = animationStyle,
            animationDuration = prefs.getInt(KEY_ANIMATION_DURATION, 300),
            animationScale = prefs.getInt(KEY_ANIMATION_SCALE, 90).toFloat() / 100f,
            blurLevel = prefs.getInt(KEY_BLUR_LEVEL, 0),
            dimmingLevel = prefs.getInt(KEY_DIMMING_LEVEL, 20),
            backgroundColor = prefs.getInt(KEY_BACKGROUND_COLOR, Color.Black.toArgb()),
            videoEnabled = prefs.getBoolean(KEY_VIDEO_ENABLED, false),
            videoDelaySeconds = prefs.getInt(KEY_VIDEO_DELAY, 3),
            videoAudioEnabled = prefs.getBoolean(KEY_VIDEO_AUDIO_ENABLED, false),
            esdeEnabled = prefs.getBoolean(KEY_ESDE_ENABLED, false),
            lastSelectedSystem = prefs.getString(KEY_LAST_SELECTED_SYSTEM, null),
            systemImageType = systemImageType
        )
    }

    fun setAnimationStyle(style: AnimationStyle) {
        _state.value = _state.value.copy(animationStyle = style)
        prefs.edit().putString(KEY_ANIMATION_STYLE, style.name).apply()
    }

    fun setAnimationDuration(duration: Int) {
        _state.value = _state.value.copy(animationDuration = duration)
        prefs.edit().putInt(KEY_ANIMATION_DURATION, duration).apply()
    }

    fun setAnimationScale(scale: Float) {
        _state.value = _state.value.copy(animationScale = scale)
        prefs.edit().putInt(KEY_ANIMATION_SCALE, (scale * 100).toInt()).apply()
    }

    fun setBlurLevel(level: Int) {
        val coercedLevel = level.coerceIn(0, 25)
        _state.value = _state.value.copy(blurLevel = coercedLevel)
        prefs.edit().putInt(KEY_BLUR_LEVEL, coercedLevel).apply()
    }

    fun setDimmingLevel(level: Int) {
        val coercedLevel = level.coerceIn(0, 100)
        _state.value = _state.value.copy(dimmingLevel = coercedLevel)
        prefs.edit().putInt(KEY_DIMMING_LEVEL, coercedLevel).apply()
    }

    fun setBackgroundColor(color: Int) {
        _state.value = _state.value.copy(backgroundColor = color)
        prefs.edit().putInt(KEY_BACKGROUND_COLOR, color).apply()
    }

    fun setVideoEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(videoEnabled = enabled)
        prefs.edit().putBoolean(KEY_VIDEO_ENABLED, enabled).apply()
    }

    fun setVideoDelaySeconds(seconds: Int) {
        _state.value = _state.value.copy(videoDelaySeconds = seconds)
        prefs.edit().putInt(KEY_VIDEO_DELAY, seconds).apply()
    }

    fun setVideoAudioEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(videoAudioEnabled = enabled)
        prefs.edit().putBoolean(KEY_VIDEO_AUDIO_ENABLED, enabled).apply()
    }

    fun setEsdeEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(esdeEnabled = enabled)
        prefs.edit().putBoolean(KEY_ESDE_ENABLED, enabled).apply()
    }

    fun setLastSelectedSystem(systemName: String?) {
        _state.value = _state.value.copy(lastSelectedSystem = systemName)
        if (systemName != null) {
            prefs.edit().putString(KEY_LAST_SELECTED_SYSTEM, systemName).apply()
        } else {
            prefs.edit().remove(KEY_LAST_SELECTED_SYSTEM).apply()
        }
    }

    fun setSystemImageType(type: SystemImageType) {
        _state.value = _state.value.copy(systemImageType = type)
        prefs.edit().putString(KEY_SYSTEM_IMAGE_TYPE, type.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "esde_prefs"
        private const val KEY_ANIMATION_STYLE = "animation_style"
        private const val KEY_ANIMATION_DURATION = "animation_duration"
        private const val KEY_ANIMATION_SCALE = "animation_scale"
        private const val KEY_BLUR_LEVEL = "blur_level"
        private const val KEY_DIMMING_LEVEL = "dimming_level"
        private const val KEY_BACKGROUND_COLOR = "background_color"
        private const val KEY_VIDEO_ENABLED = "video_enabled"
        private const val KEY_VIDEO_DELAY = "video_delay"
        private const val KEY_VIDEO_AUDIO_ENABLED = "video_audio_enabled"
        private const val KEY_ESDE_ENABLED = "esde_enabled"
        private const val KEY_LAST_SELECTED_SYSTEM = "last_selected_system"
        private const val KEY_SYSTEM_IMAGE_TYPE = "system_image_type"
    }
}