package jr.brian.home.esde.data

import androidx.core.content.edit
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_HIDDEN_SYSTEMS
import org.json.JSONArray

fun ESDEPreferencesManager.hideSystem(systemName: String) {
    val updated = _state.value.hiddenSystems + systemName
    _state.value = _state.value.copy(hiddenSystems = updated)
    prefs.edit { putString(KEY_HIDDEN_SYSTEMS, JSONArray(updated.toList()).toString()) }
}

fun ESDEPreferencesManager.unhideSystem(systemName: String) {
    val updated = _state.value.hiddenSystems - systemName
    _state.value = _state.value.copy(hiddenSystems = updated)
    if (updated.isEmpty()) {
        prefs.edit { remove(KEY_HIDDEN_SYSTEMS) }
    } else {
        prefs.edit { putString(KEY_HIDDEN_SYSTEMS, JSONArray(updated.toList()).toString()) }
    }
}

fun ESDEPreferencesManager.setSystemHidden(systemName: String, hidden: Boolean) {
    if (hidden) hideSystem(systemName) else unhideSystem(systemName)
}

fun ESDEPreferencesManager.isSystemHidden(systemName: String): Boolean {
    return state.value.hiddenSystems.contains(systemName)
}

fun ESDEPreferencesManager.setHiddenSystems(systemNames: Collection<String>) {
    val updated = systemNames.toSet()
    _state.value = _state.value.copy(hiddenSystems = updated)
    if (updated.isEmpty()) {
        prefs.edit { remove(KEY_HIDDEN_SYSTEMS) }
    } else {
        prefs.edit { putString(KEY_HIDDEN_SYSTEMS, JSONArray(updated.toList()).toString()) }
    }
}
