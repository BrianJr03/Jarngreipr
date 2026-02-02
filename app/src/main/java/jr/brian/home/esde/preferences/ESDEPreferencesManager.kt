package jr.brian.home.esde.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import jr.brian.home.esde.animation.AnimationStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

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
        
        val gameImageTypeName = prefs.getString(KEY_GAME_IMAGE_TYPE, GameImageType.Screenshots.name)
        val gameImageType = try {
            GameImageType.valueOf(gameImageTypeName ?: GameImageType.Screenshots.name)
        } catch (_: IllegalArgumentException) {
            GameImageType.Screenshots
        }
        
        val logoAlignmentName = prefs.getString(KEY_LOGO_ALIGNMENT, LogoAlignment.Center.name)
        val logoAlignment = try {
            LogoAlignment.valueOf(logoAlignmentName ?: LogoAlignment.Center.name)
        } catch (_: IllegalArgumentException) {
            LogoAlignment.Center
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
            systemImageType = systemImageType,
            gameImageType = gameImageType,
            showSystemLogo = prefs.getBoolean(KEY_SHOW_SYSTEM_LOGO, true),
            logoAlignment = logoAlignment,
            randomSystemImage = prefs.getBoolean(KEY_RANDOM_SYSTEM_IMAGE, false)
        )
    }

    fun setAnimationStyle(style: AnimationStyle) {
        _state.value = _state.value.copy(animationStyle = style)
        prefs.edit { putString(KEY_ANIMATION_STYLE, style.name) }
    }

    fun setAnimationDuration(duration: Int) {
        _state.value = _state.value.copy(animationDuration = duration)
        prefs.edit { putInt(KEY_ANIMATION_DURATION, duration) }
    }

    fun setAnimationScale(scale: Float) {
        _state.value = _state.value.copy(animationScale = scale)
        prefs.edit { putInt(KEY_ANIMATION_SCALE, (scale * 100).toInt()) }
    }

    fun setBlurLevel(level: Int) {
        val coercedLevel = level.coerceIn(0, 25)
        _state.value = _state.value.copy(blurLevel = coercedLevel)
        prefs.edit { putInt(KEY_BLUR_LEVEL, coercedLevel) }
    }

    fun setDimmingLevel(level: Int) {
        val coercedLevel = level.coerceIn(0, 100)
        _state.value = _state.value.copy(dimmingLevel = coercedLevel)
        prefs.edit { putInt(KEY_DIMMING_LEVEL, coercedLevel) }
    }

    fun setBackgroundColor(color: Int) {
        _state.value = _state.value.copy(backgroundColor = color)
        prefs.edit { putInt(KEY_BACKGROUND_COLOR, color) }
    }

    fun setVideoEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(videoEnabled = enabled)
        prefs.edit { putBoolean(KEY_VIDEO_ENABLED, enabled) }
    }

    fun setVideoDelaySeconds(seconds: Int) {
        _state.value = _state.value.copy(videoDelaySeconds = seconds)
        prefs.edit { putInt(KEY_VIDEO_DELAY, seconds) }
    }

    fun setVideoAudioEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(videoAudioEnabled = enabled)
        prefs.edit { putBoolean(KEY_VIDEO_AUDIO_ENABLED, enabled) }
    }

    fun setEsdeEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(esdeEnabled = enabled)
        prefs.edit { putBoolean(KEY_ESDE_ENABLED, enabled) }
    }

    fun setLastSelectedSystem(systemName: String?) {
        _state.value = _state.value.copy(lastSelectedSystem = systemName)
        if (systemName != null) {
            prefs.edit { putString(KEY_LAST_SELECTED_SYSTEM, systemName) }
        } else {
            prefs.edit { remove(KEY_LAST_SELECTED_SYSTEM) }
        }
    }

    fun setSystemImageType(type: SystemImageType) {
        _state.value = _state.value.copy(systemImageType = type)
        prefs.edit { putString(KEY_SYSTEM_IMAGE_TYPE, type.name) }
    }

    fun setGameImageType(type: GameImageType) {
        _state.value = _state.value.copy(gameImageType = type)
        prefs.edit { putString(KEY_GAME_IMAGE_TYPE, type.name) }
    }

    fun setShowSystemLogo(show: Boolean) {
        _state.value = _state.value.copy(showSystemLogo = show)
        prefs.edit { putBoolean(KEY_SHOW_SYSTEM_LOGO, show) }
    }

    fun setLogoAlignment(alignment: LogoAlignment) {
        _state.value = _state.value.copy(logoAlignment = alignment)
        prefs.edit { putString(KEY_LOGO_ALIGNMENT, alignment.name) }
    }

    fun setRandomSystemImage(random: Boolean) {
        _state.value = _state.value.copy(randomSystemImage = random)
        prefs.edit { putBoolean(KEY_RANDOM_SYSTEM_IMAGE, random) }
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
        private const val KEY_GAME_IMAGE_TYPE = "game_image_type"
        private const val KEY_SHOW_SYSTEM_LOGO = "show_system_logo"
        private const val KEY_LOGO_ALIGNMENT = "logo_alignment"
        private const val KEY_RANDOM_SYSTEM_IMAGE = "random_system_image"
    }
}