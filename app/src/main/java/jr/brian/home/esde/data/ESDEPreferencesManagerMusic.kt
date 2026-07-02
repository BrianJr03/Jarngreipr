package jr.brian.home.esde.data

import androidx.core.content.edit
import jr.brian.home.esde.model.MusicVideoBehavior
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_GAME_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_IGNORE_AUDIO_FOCUS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_LOOP_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_PATH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_SCREENSAVER_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_SYSTEM_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_USE_SYSTEM_SPECIFIC
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_VIDEO_BEHAVIOR
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_VOLUME

fun ESDEPreferencesManager.setMusicEnabled(enabled: Boolean) {
    _state.value = _state.value.copy(musicEnabled = enabled)
    prefs.edit { putBoolean(KEY_MUSIC_ENABLED, enabled) }
}

fun ESDEPreferencesManager.setMusicPath(path: String?) {
    _state.value = _state.value.copy(musicPath = path)
    if (path != null) {
        prefs.edit { putString(KEY_MUSIC_PATH, path) }
    } else {
        prefs.edit { remove(KEY_MUSIC_PATH) }
    }
}

fun ESDEPreferencesManager.setMusicSystemEnabled(enabled: Boolean) {
    _state.value = _state.value.copy(musicSystemEnabled = enabled)
    prefs.edit { putBoolean(KEY_MUSIC_SYSTEM_ENABLED, enabled) }
}

fun ESDEPreferencesManager.setMusicGameEnabled(enabled: Boolean) {
    _state.value = _state.value.copy(musicGameEnabled = enabled)
    prefs.edit { putBoolean(KEY_MUSIC_GAME_ENABLED, enabled) }
}

fun ESDEPreferencesManager.setMusicScreensaverEnabled(enabled: Boolean) {
    _state.value = _state.value.copy(musicScreensaverEnabled = enabled)
    prefs.edit { putBoolean(KEY_MUSIC_SCREENSAVER_ENABLED, enabled) }
}

fun ESDEPreferencesManager.setMusicVideoBehavior(behavior: MusicVideoBehavior) {
    _state.value = _state.value.copy(musicVideoBehavior = behavior)
    prefs.edit { putString(KEY_MUSIC_VIDEO_BEHAVIOR, behavior.value) }
}

fun ESDEPreferencesManager.setMusicVolume(volume: Int) {
    val coercedVolume = volume.coerceIn(0, 100)
    _state.value = _state.value.copy(musicVolume = coercedVolume)
    prefs.edit { putInt(KEY_MUSIC_VOLUME, coercedVolume) }
}

fun ESDEPreferencesManager.setMusicUseSystemSpecific(useSystemSpecific: Boolean) {
    _state.value = _state.value.copy(musicUseSystemSpecific = useSystemSpecific)
    prefs.edit { putBoolean(KEY_MUSIC_USE_SYSTEM_SPECIFIC, useSystemSpecific) }
}

fun ESDEPreferencesManager.setMusicLoopEnabled(loopEnabled: Boolean) {
    _state.value = _state.value.copy(musicLoopEnabled = loopEnabled)
    prefs.edit { putBoolean(KEY_MUSIC_LOOP_ENABLED, loopEnabled) }
}

fun ESDEPreferencesManager.setMusicIgnoreAudioFocus(ignoreAudioFocus: Boolean) {
    _state.value = _state.value.copy(musicIgnoreAudioFocus = ignoreAudioFocus)
    prefs.edit { putBoolean(KEY_MUSIC_IGNORE_AUDIO_FOCUS, ignoreAudioFocus) }
}
