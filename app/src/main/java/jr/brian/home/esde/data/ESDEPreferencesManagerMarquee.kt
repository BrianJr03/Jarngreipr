package jr.brian.home.esde.data

import androidx.core.content.edit
import jr.brian.home.esde.model.OverlayMediaType
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_DESCRIPTION_OVERLAY_PAGES
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_LOGO_ONLY_MODE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_HEIGHT
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_MIN_WIDTH_PERCENT
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_POSITION_LOCKED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_PRESS_SHORTCUT
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_PRESS_SHORTCUT_APP_PACKAGE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_VISIBLE_PAGES
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_WIDTH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_OVERLAY_MEDIA_TYPE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SHOW_MARQUEE_FOR_GAME
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SHOW_MARQUEE_FOR_SYSTEM
import jr.brian.home.model.Shortcut

fun ESDEPreferencesManager.setMarqueeWidth(width: Int) {
    val coercedWidth = width.coerceIn(40, 600)
    _state.value = _state.value.copy(marqueeWidth = coercedWidth)
    prefs.edit { putInt(KEY_MARQUEE_WIDTH, coercedWidth) }
}

fun ESDEPreferencesManager.setMarqueeHeight(height: Int) {
    val coercedHeight = height.coerceIn(40, 600)
    _state.value = _state.value.copy(marqueeHeight = coercedHeight)
    prefs.edit { putInt(KEY_MARQUEE_HEIGHT, coercedHeight) }
}

fun ESDEPreferencesManager.setMarqueePressShortcut(shortcut: Shortcut) {
    _state.value = _state.value.copy(marqueePressShortcut = shortcut)
    prefs.edit { putString(KEY_MARQUEE_PRESS_SHORTCUT, shortcut.name) }
}

fun ESDEPreferencesManager.setMarqueePressShortcutAppPackage(packageName: String?) {
    _state.value = _state.value.copy(marqueePressShortcutAppPackage = packageName)
    if (packageName != null) {
        prefs.edit { putString(KEY_MARQUEE_PRESS_SHORTCUT_APP_PACKAGE, packageName) }
    } else {
        prefs.edit { remove(KEY_MARQUEE_PRESS_SHORTCUT_APP_PACKAGE) }
    }
}

fun ESDEPreferencesManager.toggleMarqueePageVisibility(pageIndex: Int) {
    val hiddenPages = _state.value.marqueeHiddenPages
    val newPages = if (hiddenPages.contains(pageIndex)) {
        hiddenPages - pageIndex
    } else {
        hiddenPages + pageIndex
    }

    _state.value = _state.value.copy(marqueeHiddenPages = newPages)
    if (newPages.isEmpty()) {
        prefs.edit { remove(KEY_MARQUEE_VISIBLE_PAGES) }
    } else {
        prefs.edit { putString(KEY_MARQUEE_VISIBLE_PAGES, newPages.joinToString(",")) }
    }
}

fun ESDEPreferencesManager.toggleDescriptionOverlayPage(pageIndex: Int) {
    val enabledPages = _state.value.descriptionOverlayEnabledPages
    val newPages = if (enabledPages.contains(pageIndex)) {
        enabledPages - pageIndex
    } else {
        enabledPages + pageIndex
    }

    _state.value = _state.value.copy(descriptionOverlayEnabledPages = newPages)
    if (newPages.isEmpty()) {
        prefs.edit { remove(KEY_DESCRIPTION_OVERLAY_PAGES) }
    } else {
        prefs.edit { putString(KEY_DESCRIPTION_OVERLAY_PAGES, newPages.joinToString(",")) }
    }
}

fun ESDEPreferencesManager.setShowLogoForSystem(show: Boolean) {
    _state.value = _state.value.copy(showMarqueeForSystem = show)
    prefs.edit { putBoolean(KEY_SHOW_MARQUEE_FOR_SYSTEM, show) }
}

fun ESDEPreferencesManager.setShowLogoForGame(show: Boolean) {
    _state.value = _state.value.copy(showMarqueeForGame = show)
    prefs.edit { putBoolean(KEY_SHOW_MARQUEE_FOR_GAME, show) }
}

fun ESDEPreferencesManager.setLogoPositionLocked(locked: Boolean) {
    _state.value = _state.value.copy(marqueePositionLocked = locked)
    prefs.edit { putBoolean(KEY_MARQUEE_POSITION_LOCKED, locked) }
}

fun ESDEPreferencesManager.toggleLogoPositionLocked() {
    setLogoPositionLocked(!_state.value.marqueePositionLocked)
}

fun ESDEPreferencesManager.setMarqueeMinWidthPercent(percent: Float) {
    val coercedPercent = percent.coerceIn(0.3f, 1.0f)
    _state.value = _state.value.copy(marqueeMinWidthPercent = coercedPercent)
    prefs.edit { putFloat(KEY_MARQUEE_MIN_WIDTH_PERCENT, coercedPercent) }
}

fun ESDEPreferencesManager.setOverlayMediaType(type: OverlayMediaType) {
    _state.value = _state.value.copy(overlayMediaType = type)
    prefs.edit { putString(KEY_OVERLAY_MEDIA_TYPE, type.name) }
}

fun ESDEPreferencesManager.setLogoOnlyMode(enabled: Boolean) {
    _state.value = _state.value.copy(logoOnlyMode = enabled)
    prefs.edit { putBoolean(KEY_LOGO_ONLY_MODE, enabled) }
}
