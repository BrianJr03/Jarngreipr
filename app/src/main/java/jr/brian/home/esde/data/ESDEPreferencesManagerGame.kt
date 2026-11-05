package jr.brian.home.esde.data

import androidx.core.content.edit
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_COMMAND_MAP
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_COMPANION_MAP
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_CORE_MAP
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_EMULATOR_MAP
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_HIDDEN_GAMES
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_EMULATOR_MAP
import org.json.JSONArray
import org.json.JSONObject

fun ESDEPreferencesManager.setGameEmulator(gameKey: String, packageName: String) {
    val updated = _state.value.gameEmulatorMap.toMutableMap()
    updated[gameKey] = packageName
    _state.value = _state.value.copy(gameEmulatorMap = updated)
    val json = JSONObject()
    updated.forEach { (k, v) -> json.put(k, v) }
    prefs.edit { putString(KEY_GAME_EMULATOR_MAP, json.toString()) }
}

fun ESDEPreferencesManager.getGameEmulator(gameKey: String): String? {
    return state.value.gameEmulatorMap[gameKey]
}

fun ESDEPreferencesManager.setSystemEmulator(systemName: String, packageName: String) {
    val updated = _state.value.systemEmulatorMap.toMutableMap()
    updated[systemName] = packageName
    _state.value = _state.value.copy(systemEmulatorMap = updated)
    val json = JSONObject()
    updated.forEach { (k, v) -> json.put(k, v) }
    prefs.edit { putString(KEY_SYSTEM_EMULATOR_MAP, json.toString()) }
}

fun ESDEPreferencesManager.clearSystemEmulator(systemName: String) {
    val updated = _state.value.systemEmulatorMap.toMutableMap()
    if (updated.remove(systemName) == null) return
    _state.value = _state.value.copy(systemEmulatorMap = updated)
    if (updated.isEmpty()) {
        prefs.edit { remove(KEY_SYSTEM_EMULATOR_MAP) }
    } else {
        val json = JSONObject()
        updated.forEach { (k, v) -> json.put(k, v) }
        prefs.edit { putString(KEY_SYSTEM_EMULATOR_MAP, json.toString()) }
    }
}

fun ESDEPreferencesManager.getSystemEmulator(systemName: String): String? {
    return state.value.systemEmulatorMap[systemName]
}

fun ESDEPreferencesManager.setGameLaunchCommand(gameKey: String, command: String) {
    val updated = _state.value.gameCommandMap.toMutableMap()
    updated[gameKey] = command
    _state.value = _state.value.copy(gameCommandMap = updated)
    val json = JSONObject()
    updated.forEach { (k, v) -> json.put(k, v) }
    prefs.edit { putString(KEY_GAME_COMMAND_MAP, json.toString()) }
}

fun ESDEPreferencesManager.getGameLaunchCommand(gameKey: String): String? {
    return state.value.gameCommandMap[gameKey]
}

fun ESDEPreferencesManager.setGameCore(gameKey: String, corePath: String) {
    val updated = _state.value.gameCoreMap.toMutableMap()
    updated[gameKey] = corePath
    _state.value = _state.value.copy(gameCoreMap = updated)
    val json = JSONObject()
    updated.forEach { (k, v) -> json.put(k, v) }
    prefs.edit { putString(KEY_GAME_CORE_MAP, json.toString()) }
}

fun ESDEPreferencesManager.getGameCore(gameKey: String): String? {
    return state.value.gameCoreMap[gameKey]
}

private const val MAX_COMPANIONS_PER_GAME = 2

private fun ESDEPreferencesManager.persistGameCompanions(map: Map<String, List<String>>) {
    _state.value = _state.value.copy(gameCompanionsMap = map)
    if (map.isEmpty()) {
        prefs.edit { remove(KEY_GAME_COMPANION_MAP) }
        return
    }
    val json = JSONObject()
    map.forEach { (k, v) ->
        val arr = JSONArray()
        v.forEach { arr.put(it) }
        json.put(k, arr)
    }
    prefs.edit { putString(KEY_GAME_COMPANION_MAP, json.toString()) }
}

/**
 * Append [packageName] to the companion list for [gameKey]. If the game already has
 * [MAX_COMPANIONS_PER_GAME] companions, the list is replaced with a single-element
 * list containing the new pick — the detail screen's click-adds-cycles UX relies on
 * this: a tap when both slots are full starts the list over at one companion.
 */
fun ESDEPreferencesManager.addGameCompanion(gameKey: String, packageName: String) {
    val current = _state.value.gameCompanionsMap[gameKey].orEmpty()
    val updatedList = when {
        current.contains(packageName) -> current
        current.size >= MAX_COMPANIONS_PER_GAME -> listOf(packageName)
        else -> current + packageName
    }
    val updated = _state.value.gameCompanionsMap.toMutableMap()
    updated[gameKey] = updatedList
    persistGameCompanions(updated)
}

fun ESDEPreferencesManager.clearGameCompanions(gameKey: String) {
    val updated = _state.value.gameCompanionsMap.toMutableMap()
    if (updated.remove(gameKey) == null) return
    persistGameCompanions(updated)
}

fun ESDEPreferencesManager.getGameCompanions(gameKey: String): List<String> {
    return state.value.gameCompanionsMap[gameKey].orEmpty()
}

fun ESDEPreferencesManager.setAllGameCompanionsMap(map: Map<String, List<String>>) {
    persistGameCompanions(map.filterValues { it.isNotEmpty() })
}

fun ESDEPreferencesManager.hideGame(gameKey: String) {
    val updated = _state.value.hiddenGames + gameKey
    _state.value = _state.value.copy(hiddenGames = updated)
    prefs.edit { putString(KEY_HIDDEN_GAMES, JSONArray(updated.toList()).toString()) }
}

fun ESDEPreferencesManager.isGameHidden(gameKey: String): Boolean {
    return state.value.hiddenGames.contains(gameKey)
}

fun ESDEPreferencesManager.unhideGame(gameKey: String) {
    val updated = _state.value.hiddenGames - gameKey
    _state.value = _state.value.copy(hiddenGames = updated)
    prefs.edit { putString(KEY_HIDDEN_GAMES, JSONArray(updated.toList()).toString()) }
}

fun ESDEPreferencesManager.unhideAllGames(gameKeys: Collection<String>) {
    val updated = _state.value.hiddenGames - gameKeys.toSet()
    _state.value = _state.value.copy(hiddenGames = updated)
    if (updated.isEmpty()) {
        prefs.edit { remove(KEY_HIDDEN_GAMES) }
    } else {
        prefs.edit { putString(KEY_HIDDEN_GAMES, JSONArray(updated.toList()).toString()) }
    }
}
