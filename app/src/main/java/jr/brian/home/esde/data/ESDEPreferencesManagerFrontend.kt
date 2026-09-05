package jr.brian.home.esde.data

import androidx.core.content.edit
import jr.brian.home.esde.model.FRONTEND_TILE_SCALE_MAX
import jr.brian.home.esde.model.FRONTEND_TILE_SCALE_MIN
import jr.brian.home.esde.model.FRONTEND_TRANSITION_MS_MAX
import jr.brian.home.esde.model.FRONTEND_TRANSITION_MS_MIN
import jr.brian.home.esde.model.FrontendLayout
import jr.brian.home.esde.model.FrontendRowAlignment
import jr.brian.home.esde.model.FrontendTransition
import jr.brian.home.esde.model.SystemCustomization
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_CANVAS_CONTINUOUS_SPIN_ROMS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_FLOAT_INTENSITY
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_FOCUS_BACKGROUND_DIM_GAMES
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_FOCUS_BACKGROUND_DIM_SYSTEMS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_FOCUS_BACKGROUND_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_FOCUS_BACKGROUND_GAMES
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_FOCUS_BACKGROUND_SYSTEMS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_GAME_ROW_ALIGNMENT
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_GAME_TILE_SCALE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_HINTS_VISIBLE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_SYSTEM_ROW_ALIGNMENT
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_SYSTEM_TILE_SCALE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_TRANSITION
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_TRANSITION_MS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_LAYOUT
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SECONDARY_MEDIA_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_CUSTOMIZATIONS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_FOCUS_HAPTIC_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_LAYOUT
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_ORDER
import kotlinx.serialization.encodeToString
import org.json.JSONArray

fun ESDEPreferencesManager.setFrontendEnabled(enabled: Boolean) {
    _state.value = _state.value.copy(frontendEnabled = enabled)
    prefs.edit { putBoolean(KEY_FRONTEND_ENABLED, enabled) }
}

fun ESDEPreferencesManager.setSecondaryMediaEnabled(enabled: Boolean) {
    _state.value = _state.value.copy(secondaryMediaEnabled = enabled)
    prefs.edit { putBoolean(KEY_SECONDARY_MEDIA_ENABLED, enabled) }
}

fun ESDEPreferencesManager.setSystemLayout(layout: FrontendLayout) {
    _state.value = _state.value.copy(systemLayout = layout)
    prefs.edit { putString(KEY_SYSTEM_LAYOUT, layout.name) }
}

fun ESDEPreferencesManager.setGameLayout(layout: FrontendLayout) {
    _state.value = _state.value.copy(gameLayout = layout)
    prefs.edit { putString(KEY_GAME_LAYOUT, layout.name) }
}

fun ESDEPreferencesManager.setSystemCustomization(systemName: String, customization: SystemCustomization) {
    val updated = _state.value.systemCustomizations + (systemName to customization)
    persistSystemCustomizations(updated)
}

fun ESDEPreferencesManager.clearSystemCustomization(systemName: String) {
    val updated = _state.value.systemCustomizations - systemName
    persistSystemCustomizations(updated)
}

fun ESDEPreferencesManager.setAllSystemCustomizations(map: Map<String, SystemCustomization>) {
    persistSystemCustomizations(map)
}

private fun ESDEPreferencesManager.persistSystemCustomizations(map: Map<String, SystemCustomization>) {
    _state.value = _state.value.copy(systemCustomizations = map)
    if (map.isEmpty()) {
        prefs.edit { remove(KEY_SYSTEM_CUSTOMIZATIONS) }
    } else {
        prefs.edit { putString(KEY_SYSTEM_CUSTOMIZATIONS, customizationJson.encodeToString(map)) }
    }
}

fun ESDEPreferencesManager.setSystemOrder(order: List<String>) {
    _state.value = _state.value.copy(systemOrder = order)
    if (order.isEmpty()) {
        prefs.edit { remove(KEY_SYSTEM_ORDER) }
    } else {
        prefs.edit { putString(KEY_SYSTEM_ORDER, customizationJson.encodeToString(order)) }
    }
}

fun ESDEPreferencesManager.setFrontendHintsVisible(visible: Boolean) {
    _state.value = _state.value.copy(frontendHintsVisible = visible)
    prefs.edit { putBoolean(KEY_FRONTEND_HINTS_VISIBLE, visible) }
}

fun ESDEPreferencesManager.setFrontendFloatIntensity(intensity: Float) {
    val coerced = intensity.coerceIn(0f, 3f)
    _state.value = _state.value.copy(frontendFloatIntensity = coerced)
    prefs.edit { putFloat(KEY_FRONTEND_FLOAT_INTENSITY, coerced) }
}

fun ESDEPreferencesManager.setFrontendFocusHapticEnabled(enabled: Boolean) {
    _state.value = _state.value.copy(frontendFocusHapticEnabled = enabled)
    prefs.edit { putBoolean(KEY_FRONTEND_FOCUS_HAPTIC_ENABLED, enabled) }
}

fun ESDEPreferencesManager.setFrontendFocusBackgroundEnabled(enabled: Boolean) {
    _state.value = _state.value.copy(frontendFocusBackgroundEnabled = enabled)
    prefs.edit { putBoolean(KEY_FRONTEND_FOCUS_BACKGROUND_ENABLED, enabled) }
}

fun ESDEPreferencesManager.setFrontendFocusBackgroundSystems(enabled: Boolean) {
    _state.value = _state.value.copy(frontendFocusBackgroundSystems = enabled)
    prefs.edit { putBoolean(KEY_FRONTEND_FOCUS_BACKGROUND_SYSTEMS, enabled) }
}

fun ESDEPreferencesManager.setFrontendFocusBackgroundGames(enabled: Boolean) {
    _state.value = _state.value.copy(frontendFocusBackgroundGames = enabled)
    prefs.edit { putBoolean(KEY_FRONTEND_FOCUS_BACKGROUND_GAMES, enabled) }
}

fun ESDEPreferencesManager.setFrontendFocusBackgroundDimSystems(dim: Float) {
    val coerced = dim.coerceIn(0f, 1f)
    _state.value = _state.value.copy(frontendFocusBackgroundDimSystems = coerced)
    prefs.edit { putFloat(KEY_FRONTEND_FOCUS_BACKGROUND_DIM_SYSTEMS, coerced) }
}

fun ESDEPreferencesManager.setFrontendFocusBackgroundDimGames(dim: Float) {
    val coerced = dim.coerceIn(0f, 1f)
    _state.value = _state.value.copy(frontendFocusBackgroundDimGames = coerced)
    prefs.edit { putFloat(KEY_FRONTEND_FOCUS_BACKGROUND_DIM_GAMES, coerced) }
}

fun ESDEPreferencesManager.setFrontendTransition(transition: FrontendTransition) {
    _state.value = _state.value.copy(frontendTransition = transition)
    prefs.edit { putString(KEY_FRONTEND_TRANSITION, transition.name) }
}

fun ESDEPreferencesManager.setFrontendTransitionMs(durationMs: Int) {
    val coerced = durationMs.coerceIn(FRONTEND_TRANSITION_MS_MIN, FRONTEND_TRANSITION_MS_MAX)
    _state.value = _state.value.copy(frontendTransitionMs = coerced)
    prefs.edit { putInt(KEY_FRONTEND_TRANSITION_MS, coerced) }
}

fun ESDEPreferencesManager.setFrontendSystemRowAlignment(alignment: FrontendRowAlignment) {
    _state.value = _state.value.copy(frontendSystemRowAlignment = alignment)
    prefs.edit { putString(KEY_FRONTEND_SYSTEM_ROW_ALIGNMENT, alignment.name) }
}

fun ESDEPreferencesManager.setFrontendGameRowAlignment(alignment: FrontendRowAlignment) {
    _state.value = _state.value.copy(frontendGameRowAlignment = alignment)
    prefs.edit { putString(KEY_FRONTEND_GAME_ROW_ALIGNMENT, alignment.name) }
}

fun ESDEPreferencesManager.setFrontendSystemTileScale(scale: Float) {
    val coerced = scale.coerceIn(FRONTEND_TILE_SCALE_MIN, FRONTEND_TILE_SCALE_MAX)
    _state.value = _state.value.copy(frontendSystemTileScale = coerced)
    prefs.edit { putFloat(KEY_FRONTEND_SYSTEM_TILE_SCALE, coerced) }
}

fun ESDEPreferencesManager.setFrontendGameTileScale(scale: Float) {
    val coerced = scale.coerceIn(FRONTEND_TILE_SCALE_MIN, FRONTEND_TILE_SCALE_MAX)
    _state.value = _state.value.copy(frontendGameTileScale = coerced)
    prefs.edit { putFloat(KEY_FRONTEND_GAME_TILE_SCALE, coerced) }
}

fun ESDEPreferencesManager.setCanvasContinuousSpin(romKey: String, enabled: Boolean) {
    val current = _state.value.canvasContinuousSpinRoms
    val updated = if (enabled) current + romKey else current - romKey
    persistCanvasContinuousSpin(updated)
}

fun ESDEPreferencesManager.setAllCanvasContinuousSpin(roms: Set<String>) {
    persistCanvasContinuousSpin(roms)
}

private fun ESDEPreferencesManager.persistCanvasContinuousSpin(roms: Set<String>) {
    _state.value = _state.value.copy(canvasContinuousSpinRoms = roms)
    if (roms.isEmpty()) {
        prefs.edit { remove(KEY_CANVAS_CONTINUOUS_SPIN_ROMS) }
    } else {
        prefs.edit { putString(KEY_CANVAS_CONTINUOUS_SPIN_ROMS, JSONArray(roms.toList()).toString()) }
    }
}
