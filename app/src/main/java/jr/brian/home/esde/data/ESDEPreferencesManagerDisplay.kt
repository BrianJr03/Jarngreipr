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
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_FOLDER_MAPPINGS
import jr.brian.home.esde.model.SystemFolderMapping
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

/**
 * Returns the mapping for [systemName] if one has been declared, or null.
 * When a mapping exists it is the authoritative source of the folder we scan
 * and launch through — callers reading a SAF tree URI for [systemName]
 * should already see the mapping's URI via [setSafTreeUri] on add.
 */
fun ESDEPreferencesManager.getSystemFolderMapping(systemName: String): SystemFolderMapping? =
    state.value.systemFolderMappings.firstOrNull {
        it.systemName.equals(systemName, ignoreCase = true)
    }

fun ESDEPreferencesManager.addSystemFolderMapping(mapping: SystemFolderMapping) {
    val existing = _state.value.systemFolderMappings
    val filtered = existing.filterNot {
        it.systemName.equals(mapping.systemName, ignoreCase = true) &&
                it.treeUri == mapping.treeUri
    }
    val updated = filtered + mapping
    _state.value = _state.value.copy(systemFolderMappings = updated)
    prefs.edit {
        putString(KEY_SYSTEM_FOLDER_MAPPINGS, customizationJson.encodeToString(updated))
    }
}

/**
 * Replace the entire mapping list. Used by import/export to restore a
 * previously saved set atomically without triggering per-item state churn.
 */
fun ESDEPreferencesManager.setAllSystemFolderMappings(mappings: List<SystemFolderMapping>) {
    _state.value = _state.value.copy(systemFolderMappings = mappings)
    if (mappings.isEmpty()) {
        prefs.edit { remove(KEY_SYSTEM_FOLDER_MAPPINGS) }
    } else {
        prefs.edit {
            putString(KEY_SYSTEM_FOLDER_MAPPINGS, customizationJson.encodeToString(mappings))
        }
    }
}

fun ESDEPreferencesManager.removeSystemFolderMapping(mapping: SystemFolderMapping) {
    val existing = _state.value.systemFolderMappings
    val updated = existing.filterNot {
        it.systemName.equals(mapping.systemName, ignoreCase = true) &&
                it.treeUri == mapping.treeUri
    }
    if (updated.size == existing.size) return
    _state.value = _state.value.copy(systemFolderMappings = updated)
    if (updated.isEmpty()) {
        prefs.edit { remove(KEY_SYSTEM_FOLDER_MAPPINGS) }
    } else {
        prefs.edit {
            putString(KEY_SYSTEM_FOLDER_MAPPINGS, customizationJson.encodeToString(updated))
        }
    }
}

/** Returns the persisted SAF tree URI string for [systemName], or null. */
fun ESDEPreferencesManager.getSafTreeUri(systemName: String): String? {
    val json = prefs.getString(KEY_SAF_TREE_URIS, null) ?: return null
    return try { JSONObject(json).optString(systemName, null) } catch (_: Exception) { null }
}

/** Persists a SAF tree URI for [systemName]. */
fun ESDEPreferencesManager.setSafTreeUri(systemName: String, treeUriString: String) {
    val json = try { JSONObject(prefs.getString(KEY_SAF_TREE_URIS, null) ?: "{}") } catch (_: Exception) { JSONObject() }
    json.put(systemName, treeUriString)
    prefs.edit { putString(KEY_SAF_TREE_URIS, json.toString()) }
}

/**
 * Toggle whether ES-DE gamelist.xml decorates the filesystem scan. Off means
 * the library shows exactly the ROMs on disk, titled from their filenames, and
 * gamelist.xml is not read. Callers must trigger a library rebuild after
 * flipping this — the flag changes what the ROM index looks like on the next
 * scan, not what is already in memory.
 */
fun ESDEPreferencesManager.setGamelistDecorationEnabled(enabled: Boolean) {
    _state.value = _state.value.copy(gamelistDecorationEnabled = enabled)
    prefs.edit { putBoolean(jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAMELIST_DECORATION_ENABLED, enabled) }
}

/** Removes the persisted SAF tree URI for [systemName], if any. */
fun ESDEPreferencesManager.clearSafTreeUri(systemName: String) {
    val existing = prefs.getString(KEY_SAF_TREE_URIS, null) ?: return
    val json = try { JSONObject(existing) } catch (_: Exception) { return }
    json.remove(systemName)
    prefs.edit { putString(KEY_SAF_TREE_URIS, json.toString()) }
}
