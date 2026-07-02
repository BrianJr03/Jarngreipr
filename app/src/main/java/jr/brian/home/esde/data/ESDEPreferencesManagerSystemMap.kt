package jr.brian.home.esde.data

import androidx.core.content.edit
import jr.brian.home.esde.model.SystemLaunchTrigger
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_APP_MAP
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_LAUNCH_TRIGGER_MAP
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_TOP_SCREEN
import org.json.JSONArray
import org.json.JSONObject

fun ESDEPreferencesManager.setSystemAppMap(map: Map<String, String?>) {
    _state.value = _state.value.copy(systemAppMap = map)
    if (map.isEmpty()) {
        prefs.edit { remove(KEY_SYSTEM_APP_MAP) }
    } else {
        val json = JSONObject()
        map.forEach { (key, value) ->
            if (value != null) json.put(key, value) else json.put(key, JSONObject.NULL)
        }
        prefs.edit { putString(KEY_SYSTEM_APP_MAP, json.toString()) }
    }
}

fun ESDEPreferencesManager.getSystemAppForSystem(systemName: String): String? {
    return state.value.systemAppMap[systemName]
}

fun ESDEPreferencesManager.setSystemLaunchTrigger(systemFolderName: String, trigger: SystemLaunchTrigger) {
    val current = _state.value.systemLaunchTriggerMap.toMutableMap()
    if (trigger == SystemLaunchTrigger.NoAction) {
        current.remove(systemFolderName)
    } else {
        current[systemFolderName] = trigger
    }
    _state.value = _state.value.copy(systemLaunchTriggerMap = current)
    if (current.isEmpty()) {
        prefs.edit { remove(KEY_SYSTEM_LAUNCH_TRIGGER_MAP) }
    } else {
        val json = JSONObject()
        current.forEach { (key, value) -> json.put(key, value.name) }
        prefs.edit { putString(KEY_SYSTEM_LAUNCH_TRIGGER_MAP, json.toString()) }
    }
}

fun ESDEPreferencesManager.getSystemLaunchTrigger(systemName: String): SystemLaunchTrigger {
    return state.value.systemLaunchTriggerMap[systemName] ?: SystemLaunchTrigger.NoAction
}

fun ESDEPreferencesManager.toggleSystemTopScreen(systemFolderName: String) {
    val current = _state.value.systemTopScreenSet
    val updated = if (current.contains(systemFolderName)) {
        current - systemFolderName
    } else {
        current + systemFolderName
    }
    _state.value = _state.value.copy(systemTopScreenSet = updated)
    if (updated.isEmpty()) {
        prefs.edit { remove(KEY_SYSTEM_TOP_SCREEN) }
    } else {
        prefs.edit { putString(KEY_SYSTEM_TOP_SCREEN, JSONArray(updated.toList()).toString()) }
    }
}

fun ESDEPreferencesManager.isSystemBottomScreen(systemName: String): Boolean {
    return !state.value.systemTopScreenSet.contains(systemName)
}
