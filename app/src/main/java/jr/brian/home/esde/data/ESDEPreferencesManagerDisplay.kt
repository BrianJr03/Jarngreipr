package jr.brian.home.esde.data

import androidx.core.content.edit
import jr.brian.home.esde.model.GameImageType
import jr.brian.home.esde.model.LogoAlignment
import jr.brian.home.esde.model.ScreensaverBehavior
import jr.brian.home.esde.model.SystemImageType
import jr.brian.home.esde.model.WallpaperToggleTarget
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_APP_DRAWER_OPACITY
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_CUSTOM_MEDIA_PATH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_CUSTOM_SYSTEM_IMAGES_PATH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_CUSTOM_SYSTEM_LOGOS_PATH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_EFFECTS_EXCLUDED_PAGES
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_IMAGE_TYPE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_HIDE_UI_FOR_GAME_BROWSING
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_LAST_SELECTED_SYSTEM
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_LOGO_ALIGNMENT
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_LOGO_OFFSET_X
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_LOGO_OFFSET_Y
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_PERSIST_BACKGROUND_BRIGHTNESS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_PERSIST_LOGO_BRIGHTNESS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_PERSIST_ON_GAME_LAUNCH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_POWER_EVENTS_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_RANDOM_SYSTEM_IMAGE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROMS_PATHS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SAF_TREE_URIS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SCREENSAVER_BEHAVIOR
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SCREENSAVER_FLOATY_APP_COUNT
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SELECT_BUTTON_WALLPAPER_TOGGLE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SINGLE_GAME_IMAGE_PATH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SINGLE_GAME_LOGO_PATH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SINGLE_SYSTEM_IMAGE_PATH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SINGLE_SYSTEM_LOGO_PATH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_IMAGE_TYPE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_WALLPAPER_TOGGLE_TARGET
import org.json.JSONArray
import org.json.JSONObject

fun ESDEPreferencesManager.setLastSelectedSystem(systemName: String?) {
    _state.value = _state.value.copy(lastSelectedSystem = systemName)
    if (systemName != null) {
        prefs.edit { putString(KEY_LAST_SELECTED_SYSTEM, systemName) }
    } else {
        prefs.edit { remove(KEY_LAST_SELECTED_SYSTEM) }
    }
}

fun ESDEPreferencesManager.setSystemImageType(type: SystemImageType) {
    _state.value = _state.value.copy(systemImageType = type)
    prefs.edit { putString(KEY_SYSTEM_IMAGE_TYPE, type.name) }
}

fun ESDEPreferencesManager.setGameImageType(type: GameImageType) {
    _state.value = _state.value.copy(gameImageType = type)
    prefs.edit { putString(KEY_GAME_IMAGE_TYPE, type.name) }
}

fun ESDEPreferencesManager.setLogoAlignment(alignment: LogoAlignment) {
    _state.value = _state.value.copy(logoAlignment = alignment)
    prefs.edit { putString(KEY_LOGO_ALIGNMENT, alignment.name) }
}

fun ESDEPreferencesManager.setLogoOffset(x: Float, y: Float) {
    _state.value = _state.value.copy(logoOffsetX = x, logoOffsetY = y)
    prefs.edit {
        putFloat(KEY_LOGO_OFFSET_X, x)
        putFloat(KEY_LOGO_OFFSET_Y, y)
    }
}

fun ESDEPreferencesManager.setRandomSystemImage(random: Boolean) {
    _state.value = _state.value.copy(randomSystemImage = random)
    prefs.edit { putBoolean(KEY_RANDOM_SYSTEM_IMAGE, random) }
}

fun ESDEPreferencesManager.setPowerEventsEnabled(enabled: Boolean) {
    _state.value = _state.value.copy(powerEventsEnabled = enabled)
    prefs.edit { putBoolean(KEY_POWER_EVENTS_ENABLED, enabled) }
}

fun ESDEPreferencesManager.setPersistOnGameLaunch(persist: Boolean) {
    _state.value = _state.value.copy(persistOnGameLaunch = persist)
    prefs.edit { putBoolean(KEY_PERSIST_ON_GAME_LAUNCH, persist) }
}

fun ESDEPreferencesManager.setPersistBackgroundBrightness(brightness: Int) {
    val coercedBrightness = brightness.coerceIn(30, 100)
    _state.value = _state.value.copy(persistBackgroundBrightness = coercedBrightness)
    prefs.edit { putInt(KEY_PERSIST_BACKGROUND_BRIGHTNESS, coercedBrightness) }
}

fun ESDEPreferencesManager.setPersistLogoBrightness(brightness: Int) {
    val coercedBrightness = brightness.coerceIn(30, 100)
    _state.value = _state.value.copy(persistLogoBrightness = coercedBrightness)
    prefs.edit { putInt(KEY_PERSIST_LOGO_BRIGHTNESS, coercedBrightness) }
}

fun ESDEPreferencesManager.setCustomSystemLogosPath(path: String?) {
    _state.value = _state.value.copy(customSystemLogosPath = path)
    if (path != null) {
        prefs.edit { putString(KEY_CUSTOM_SYSTEM_LOGOS_PATH, path) }
    } else {
        prefs.edit { remove(KEY_CUSTOM_SYSTEM_LOGOS_PATH) }
    }
}

fun ESDEPreferencesManager.setCustomSystemImagesPath(path: String?) {
    _state.value = _state.value.copy(customSystemImagesPath = path)
    if (path != null) {
        prefs.edit { putString(KEY_CUSTOM_SYSTEM_IMAGES_PATH, path) }
    } else {
        prefs.edit { remove(KEY_CUSTOM_SYSTEM_IMAGES_PATH) }
    }
}

fun ESDEPreferencesManager.setSingleSystemImagePath(path: String?) {
    _state.value = _state.value.copy(singleSystemImagePath = path)
    if (path != null) {
        prefs.edit { putString(KEY_SINGLE_SYSTEM_IMAGE_PATH, path) }
    } else {
        prefs.edit { remove(KEY_SINGLE_SYSTEM_IMAGE_PATH) }
    }
}

fun ESDEPreferencesManager.setSingleSystemLogoPath(path: String?) {
    _state.value = _state.value.copy(singleSystemLogoPath = path)
    if (path != null) {
        prefs.edit { putString(KEY_SINGLE_SYSTEM_LOGO_PATH, path) }
    } else {
        prefs.edit { remove(KEY_SINGLE_SYSTEM_LOGO_PATH) }
    }
}

fun ESDEPreferencesManager.setSingleGameImagePath(path: String?) {
    _state.value = _state.value.copy(singleGameImagePath = path)
    if (path != null) {
        prefs.edit { putString(KEY_SINGLE_GAME_IMAGE_PATH, path) }
    } else {
        prefs.edit { remove(KEY_SINGLE_GAME_IMAGE_PATH) }
    }
}

fun ESDEPreferencesManager.setSingleGameLogoPath(path: String?) {
    _state.value = _state.value.copy(singleGameLogoPath = path)
    if (path != null) {
        prefs.edit { putString(KEY_SINGLE_GAME_LOGO_PATH, path) }
    } else {
        prefs.edit { remove(KEY_SINGLE_GAME_LOGO_PATH) }
    }
}

fun ESDEPreferencesManager.setScreensaverBehavior(behavior: ScreensaverBehavior) {
    _state.value = _state.value.copy(screensaverBehavior = behavior)
    prefs.edit { putString(KEY_SCREENSAVER_BEHAVIOR, behavior.name) }
}

fun ESDEPreferencesManager.setScreensaverFloatyAppCount(count: Int) {
    val coercedCount = count.coerceIn(0, 100)
    _state.value = _state.value.copy(screensaverFloatyAppCount = coercedCount)
    prefs.edit { putInt(KEY_SCREENSAVER_FLOATY_APP_COUNT, coercedCount) }
}

fun ESDEPreferencesManager.setAppDrawerOpacity(opacity: Int) {
    val coercedOpacity = opacity.coerceIn(0, 100)
    _state.value = _state.value.copy(appDrawerOpacity = coercedOpacity)
    prefs.edit { putInt(KEY_APP_DRAWER_OPACITY, coercedOpacity) }
}

fun ESDEPreferencesManager.setCustomMediaPath(path: String?) {
    _state.value = _state.value.copy(customMediaPath = path)
    if (path != null) {
        prefs.edit { putString(KEY_CUSTOM_MEDIA_PATH, path) }
    } else {
        prefs.edit { remove(KEY_CUSTOM_MEDIA_PATH) }
    }
}

fun ESDEPreferencesManager.toggleEffectsExcludedPage(pageIndex: Int) {
    val excludedPages = _state.value.effectsExcludedPages
    val newPages = if (excludedPages.contains(pageIndex)) {
        excludedPages - pageIndex
    } else {
        excludedPages + pageIndex
    }

    _state.value = _state.value.copy(effectsExcludedPages = newPages)
    if (newPages.isEmpty()) {
        prefs.edit { remove(KEY_EFFECTS_EXCLUDED_PAGES) }
    } else {
        prefs.edit { putString(KEY_EFFECTS_EXCLUDED_PAGES, newPages.joinToString(",")) }
    }
}

fun ESDEPreferencesManager.setHideUIForGameBrowsing(hide: Boolean) {
    _state.value = _state.value.copy(hideUIForGameBrowsing = hide)
    prefs.edit { putBoolean(KEY_HIDE_UI_FOR_GAME_BROWSING, hide) }
}

fun ESDEPreferencesManager.setSelectButtonWallpaperToggle(enabled: Boolean) {
    _state.value = _state.value.copy(selectButtonWallpaperToggle = enabled)
    prefs.edit { putBoolean(KEY_SELECT_BUTTON_WALLPAPER_TOGGLE, enabled) }
}

fun ESDEPreferencesManager.setWallpaperToggleTarget(target: WallpaperToggleTarget) {
    _state.value = _state.value.copy(wallpaperToggleTarget = target)
    prefs.edit { putString(KEY_WALLPAPER_TOGGLE_TARGET, target.name) }
}

fun ESDEPreferencesManager.addRomsPath(path: String) {
    val current = _state.value.romsPaths
    if (current.contains(path)) return
    val updated = current + path
    _state.value = _state.value.copy(romsPaths = updated)
    prefs.edit { putString(KEY_ROMS_PATHS, JSONArray(updated).toString()) }
}

fun ESDEPreferencesManager.removeRomsPath(path: String) {
    val updated = _state.value.romsPaths - path
    _state.value = _state.value.copy(romsPaths = updated)
    if (updated.isEmpty()) {
        prefs.edit { remove(KEY_ROMS_PATHS) }
    } else {
        prefs.edit { putString(KEY_ROMS_PATHS, JSONArray(updated).toString()) }
    }
}

/** Returns the persisted SAF tree URI string for the given SD card volume ID, or null. */
fun ESDEPreferencesManager.getSafTreeUri(volumeId: String): String? {
    val json = prefs.getString(KEY_SAF_TREE_URIS, null) ?: return null
    return try { JSONObject(json).optString(volumeId, null) } catch (_: Exception) { null }
}

/** Persists a SAF tree URI for the given SD card volume ID. */
fun ESDEPreferencesManager.setSafTreeUri(volumeId: String, treeUriString: String) {
    val json = try { JSONObject(prefs.getString(KEY_SAF_TREE_URIS, null) ?: "{}") } catch (_: Exception) { JSONObject() }
    json.put(volumeId, treeUriString)
    prefs.edit { putString(KEY_SAF_TREE_URIS, json.toString()) }
}
