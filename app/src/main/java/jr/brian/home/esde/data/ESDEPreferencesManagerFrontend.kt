package jr.brian.home.esde.data

import androidx.core.content.edit
import jr.brian.home.esde.model.FrontendLayout
import jr.brian.home.esde.model.SystemCustomization
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_CANVAS_CONTINUOUS_SPIN_ROMS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_FLOAT_INTENSITY
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_HINTS_VISIBLE
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
