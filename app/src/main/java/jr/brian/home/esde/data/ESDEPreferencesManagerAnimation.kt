package jr.brian.home.esde.data

import androidx.core.content.edit
import jr.brian.home.esde.model.AnimationStyle
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ANIMATION_DURATION
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ANIMATION_SCALE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ANIMATION_STYLE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_LOGO_CHANGE_ANIMATION
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_LOGO_VISIBILITY_ANIMATION

fun ESDEPreferencesManager.setAnimationStyle(style: AnimationStyle) {
    _state.value = _state.value.copy(animationStyle = style)
    prefs.edit { putString(KEY_ANIMATION_STYLE, style.name) }
}

fun ESDEPreferencesManager.setAnimationDuration(duration: Int) {
    _state.value = _state.value.copy(animationDuration = duration)
    prefs.edit { putInt(KEY_ANIMATION_DURATION, duration) }
}

fun ESDEPreferencesManager.setAnimationScale(scale: Float) {
    _state.value = _state.value.copy(animationScale = scale)
    prefs.edit { putInt(KEY_ANIMATION_SCALE, (scale * 100).toInt()) }
}

fun ESDEPreferencesManager.setLogoVisibilityAnimation(enabled: Boolean) {
    _state.value = _state.value.copy(logoVisibilityAnimation = enabled)
    prefs.edit { putBoolean(KEY_LOGO_VISIBILITY_ANIMATION, enabled) }
}

fun ESDEPreferencesManager.setLogoChangeAnimation(enabled: Boolean) {
    _state.value = _state.value.copy(logoChangeAnimation = enabled)
    prefs.edit { putBoolean(KEY_LOGO_CHANGE_ANIMATION, enabled) }
}
