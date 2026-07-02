package jr.brian.home.esde.data

import androidx.core.content.edit
import jr.brian.home.esde.model.PlatformImageFolderType
import jr.brian.home.esde.model.RomSearchCardMediaType
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_FOCUS_ANIMATION_SPIN
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_BLACK_BACKGROUND
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_CARD_MEDIA_TYPE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_DETAIL_IMAGE_HEIGHT_DP
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_FOCUS_ANIMATION_DELAY_MS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_FOCUS_ANIMATION_DISABLED_GAMES
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_GAME_MEDIA_MAP
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_HIDE_NO_IMAGE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_HIDE_NO_METADATA
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_HINTS_KB_VISIBLE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_PLATFORM_AUTO_FILTER
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_PLATFORM_IMAGES_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_PLATFORM_IMAGES_FOLDER_TYPE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_PLATFORM_IMAGES_FOLDER_URI
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_SHOW_ALL_ANDROID_APPS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_SYSTEM_MEDIA_MAP
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_USE_WALLPAPER
import org.json.JSONArray
import org.json.JSONObject

fun ESDEPreferencesManager.setRomSearchUseWallpaper(enabled: Boolean) {
    _state.value = _state.value.copy(romSearchUseWallpaper = enabled)
    prefs.edit { putBoolean(KEY_ROM_SEARCH_USE_WALLPAPER, enabled) }
}

fun ESDEPreferencesManager.setRomSearchCardMediaType(type: RomSearchCardMediaType) {
    _state.value = _state.value.copy(romSearchCardMediaType = type)
    prefs.edit { putString(KEY_ROM_SEARCH_CARD_MEDIA_TYPE, type.name) }
}

fun ESDEPreferencesManager.setGameMediaType(gameKey: String, type: RomSearchCardMediaType) {
    val updated = _state.value.romSearchGameMediaMap + (gameKey to type.name)
    _state.value = _state.value.copy(romSearchGameMediaMap = updated)
    prefs.edit { putString(KEY_ROM_SEARCH_GAME_MEDIA_MAP, JSONObject(updated).toString()) }
}

fun ESDEPreferencesManager.clearGameMediaType(gameKey: String) {
    val updated = _state.value.romSearchGameMediaMap - gameKey
    _state.value = _state.value.copy(romSearchGameMediaMap = updated)
    if (updated.isEmpty()) {
        prefs.edit { remove(KEY_ROM_SEARCH_GAME_MEDIA_MAP) }
    } else {
        prefs.edit { putString(KEY_ROM_SEARCH_GAME_MEDIA_MAP, JSONObject(updated).toString()) }
    }
}

fun ESDEPreferencesManager.setSystemMediaType(systemName: String, type: RomSearchCardMediaType) {
    val updated = _state.value.systemMediaMap + (systemName to type.name)
    _state.value = _state.value.copy(systemMediaMap = updated)
    prefs.edit { putString(KEY_ROM_SEARCH_SYSTEM_MEDIA_MAP, JSONObject(updated).toString()) }
}

fun ESDEPreferencesManager.clearSystemMediaType(systemName: String) {
    val updated = _state.value.systemMediaMap - systemName
    _state.value = _state.value.copy(systemMediaMap = updated)
    if (updated.isEmpty()) {
        prefs.edit { remove(KEY_ROM_SEARCH_SYSTEM_MEDIA_MAP) }
    } else {
        prefs.edit { putString(KEY_ROM_SEARCH_SYSTEM_MEDIA_MAP, JSONObject(updated).toString()) }
    }
}

fun ESDEPreferencesManager.setAllGameMediaMap(map: Map<String, String>) {
    _state.value = _state.value.copy(romSearchGameMediaMap = map)
    if (map.isEmpty()) {
        prefs.edit { remove(KEY_ROM_SEARCH_GAME_MEDIA_MAP) }
    } else {
        prefs.edit { putString(KEY_ROM_SEARCH_GAME_MEDIA_MAP, JSONObject(map).toString()) }
    }
}

fun ESDEPreferencesManager.setAllSystemMediaMap(map: Map<String, String>) {
    _state.value = _state.value.copy(systemMediaMap = map)
    if (map.isEmpty()) {
        prefs.edit { remove(KEY_ROM_SEARCH_SYSTEM_MEDIA_MAP) }
    } else {
        prefs.edit { putString(KEY_ROM_SEARCH_SYSTEM_MEDIA_MAP, JSONObject(map).toString()) }
    }
}

fun ESDEPreferencesManager.setRomSearchHideNoMetadata(enabled: Boolean) {
    _state.value = _state.value.copy(romSearchHideNoMetadata = enabled)
    prefs.edit { putBoolean(KEY_ROM_SEARCH_HIDE_NO_METADATA, enabled) }
}

fun ESDEPreferencesManager.setRomSearchHideNoImage(enabled: Boolean) {
    _state.value = _state.value.copy(romSearchHideNoImage = enabled)
    prefs.edit { putBoolean(KEY_ROM_SEARCH_HIDE_NO_IMAGE, enabled) }
}

fun ESDEPreferencesManager.setRomSearchDiscSpin(enabled: Boolean) {
    _state.value = _state.value.copy(romSearchDiscSpin = enabled)
    prefs.edit { putBoolean(KEY_ROM_FOCUS_ANIMATION_SPIN, enabled) }
}

fun ESDEPreferencesManager.setRomSearchBlackBackground(enabled: Boolean) {
    _state.value = _state.value.copy(romSearchBlackBackground = enabled)
    prefs.edit { putBoolean(KEY_ROM_SEARCH_BLACK_BACKGROUND, enabled) }
}

fun ESDEPreferencesManager.setRomSearchPlatformAutoFilter(enabled: Boolean) {
    _state.value = _state.value.copy(romSearchPlatformAutoFilter = enabled)
    prefs.edit { putBoolean(KEY_ROM_SEARCH_PLATFORM_AUTO_FILTER, enabled) }
}

fun ESDEPreferencesManager.setRomSearchShowAllAndroidApps(enabled: Boolean) {
    _state.value = _state.value.copy(romSearchShowAllAndroidApps = enabled)
    prefs.edit { putBoolean(KEY_ROM_SEARCH_SHOW_ALL_ANDROID_APPS, enabled) }
}

fun ESDEPreferencesManager.setRomSearchFocusAnimationDelayMs(delayMs: Int) {
    _state.value = _state.value.copy(romSearchFocusAnimationDelayMs = delayMs)
    prefs.edit { putInt(KEY_ROM_SEARCH_FOCUS_ANIMATION_DELAY_MS, delayMs) }
}

fun ESDEPreferencesManager.setRomSearchPlatformImagesEnabled(enabled: Boolean) {
    _state.value = _state.value.copy(romSearchPlatformImagesEnabled = enabled)
    prefs.edit { putBoolean(KEY_ROM_SEARCH_PLATFORM_IMAGES_ENABLED, enabled) }
}

fun ESDEPreferencesManager.setRomSearchPlatformImagesFolderUri(uri: String?) {
    _state.value = _state.value.copy(romSearchPlatformImagesFolderUri = uri)
    prefs.edit {
        if (uri != null) putString(KEY_ROM_SEARCH_PLATFORM_IMAGES_FOLDER_URI, uri)
        else remove(KEY_ROM_SEARCH_PLATFORM_IMAGES_FOLDER_URI)
    }
}

fun ESDEPreferencesManager.setRomSearchPlatformImagesFolderType(type: PlatformImageFolderType) {
    _state.value = _state.value.copy(romSearchPlatformImagesFolderType = type)
    prefs.edit { putString(KEY_ROM_SEARCH_PLATFORM_IMAGES_FOLDER_TYPE, type.name) }
}

fun ESDEPreferencesManager.setRomSearchDetailImageHeightDp(heightDp: Int) {
    _state.value = _state.value.copy(romSearchDetailImageHeightDp = heightDp)
    prefs.edit { putInt(KEY_ROM_SEARCH_DETAIL_IMAGE_HEIGHT_DP, heightDp) }
}

fun ESDEPreferencesManager.setRomSearchHintsKbVisible(visible: Boolean) {
    _state.value = _state.value.copy(romSearchHintsKbVisible = visible)
    prefs.edit { putBoolean(KEY_ROM_SEARCH_HINTS_KB_VISIBLE, visible) }
}

fun ESDEPreferencesManager.disableFocusAnimation(gameKey: String) {
    val updated = _state.value.romSearchFocusAnimationDisabledGames + gameKey
    _state.value = _state.value.copy(romSearchFocusAnimationDisabledGames = updated)
    prefs.edit { putString(KEY_ROM_SEARCH_FOCUS_ANIMATION_DISABLED_GAMES, JSONArray(updated.toList()).toString()) }
}

fun ESDEPreferencesManager.enableFocusAnimation(gameKey: String) {
    val updated = _state.value.romSearchFocusAnimationDisabledGames - gameKey
    _state.value = _state.value.copy(romSearchFocusAnimationDisabledGames = updated)
    if (updated.isEmpty()) {
        prefs.edit { remove(KEY_ROM_SEARCH_FOCUS_ANIMATION_DISABLED_GAMES) }
    } else {
        prefs.edit { putString(KEY_ROM_SEARCH_FOCUS_ANIMATION_DISABLED_GAMES, JSONArray(updated.toList()).toString()) }
    }
}
