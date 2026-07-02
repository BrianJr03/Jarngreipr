package jr.brian.home.esde.data

import androidx.core.content.edit
import jr.brian.home.esde.model.BackgroundScaleMode
import jr.brian.home.esde.model.VideoScaleMode
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_BACKGROUND_COLOR
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_BACKGROUND_DIMMING
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_BACKGROUND_SCALE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_BACKGROUND_SCALE_MODE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_BLUR_LEVEL
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_BACKGROUND_DIMMING
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_BACKGROUND_SCALE_MODE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_BG_VIDEO_LOOPING
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_BG_VIDEO_MUTED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_BLUR_LEVEL
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_VIDEO_AUDIO_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_VIDEO_DELAY
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_VIDEO_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_VIDEO_OVERLAY_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_VIDEO_SCALE_MODE

fun ESDEPreferencesManager.setSystemBlurLevel(level: Int) {
    val coercedLevel = level.coerceIn(0, 25)
    _state.value = _state.value.copy(systemBlurLevel = coercedLevel)
    prefs.edit { putInt(KEY_SYSTEM_BLUR_LEVEL, coercedLevel) }
}

fun ESDEPreferencesManager.setGameBlurLevel(level: Int) {
    val coercedLevel = level.coerceIn(0, 25)
    _state.value = _state.value.copy(gameBlurLevel = coercedLevel)
    prefs.edit { putInt(KEY_GAME_BLUR_LEVEL, coercedLevel) }
}

fun ESDEPreferencesManager.setBackgroundColor(color: Int) {
    _state.value = _state.value.copy(backgroundColor = color)
    prefs.edit { putInt(KEY_BACKGROUND_COLOR, color) }
}

fun ESDEPreferencesManager.setVideoEnabled(enabled: Boolean) {
    _state.value = _state.value.copy(videoEnabled = enabled)
    prefs.edit { putBoolean(KEY_VIDEO_ENABLED, enabled) }
}

fun ESDEPreferencesManager.setVideoDelaySeconds(seconds: Int) {
    _state.value = _state.value.copy(videoDelaySeconds = seconds)
    prefs.edit { putInt(KEY_VIDEO_DELAY, seconds) }
}

fun ESDEPreferencesManager.setVideoAudioEnabled(enabled: Boolean) {
    _state.value = _state.value.copy(videoAudioEnabled = enabled)
    prefs.edit { putBoolean(KEY_VIDEO_AUDIO_ENABLED, enabled) }
}

fun ESDEPreferencesManager.setVideoScaleMode(mode: VideoScaleMode) {
    _state.value = _state.value.copy(videoScaleMode = mode)
    prefs.edit { putString(KEY_VIDEO_SCALE_MODE, mode.name) }
}

fun ESDEPreferencesManager.setVideoOverlayEnabled(enabled: Boolean) {
    _state.value = _state.value.copy(videoOverlayEnabled = enabled)
    prefs.edit { putBoolean(KEY_VIDEO_OVERLAY_ENABLED, enabled) }
}

fun ESDEPreferencesManager.setSystemBackgroundScaleMode(mode: BackgroundScaleMode) {
    _state.value = _state.value.copy(systemBackgroundScaleMode = mode)
    prefs.edit { putString(KEY_SYSTEM_BACKGROUND_SCALE_MODE, mode.name) }
}

fun ESDEPreferencesManager.setGameBackgroundScaleMode(mode: BackgroundScaleMode) {
    _state.value = _state.value.copy(gameBackgroundScaleMode = mode)
    prefs.edit { putString(KEY_GAME_BACKGROUND_SCALE_MODE, mode.name) }
}

fun ESDEPreferencesManager.setSystemBgVideoMuted(muted: Boolean) {
    _state.value = _state.value.copy(systemBgVideoMuted = muted)
    prefs.edit { putBoolean(KEY_SYSTEM_BG_VIDEO_MUTED, muted) }
}

fun ESDEPreferencesManager.setSystemBgVideoLooping(looping: Boolean) {
    _state.value = _state.value.copy(systemBgVideoLooping = looping)
    prefs.edit { putBoolean(KEY_SYSTEM_BG_VIDEO_LOOPING, looping) }
}

fun ESDEPreferencesManager.setGameBackgroundDimming(level: Int) {
    val coercedLevel = level.coerceIn(0, 70)
    _state.value = _state.value.copy(gameBackgroundDimming = coercedLevel)
    prefs.edit { putInt(KEY_GAME_BACKGROUND_DIMMING, coercedLevel) }
}

fun ESDEPreferencesManager.setSystemBackgroundDimming(level: Int) {
    val coercedLevel = level.coerceIn(0, 70)
    _state.value = _state.value.copy(systemBackgroundDimming = coercedLevel)
    prefs.edit { putInt(KEY_SYSTEM_BACKGROUND_DIMMING, coercedLevel) }
}

fun ESDEPreferencesManager.setGameBackgroundScale(scale: Float) {
    val coercedScale = scale.coerceIn(0.2f, 1.0f)
    _state.value = _state.value.copy(gameBackgroundScale = coercedScale)
    prefs.edit { putFloat(KEY_GAME_BACKGROUND_SCALE, coercedScale) }
}
