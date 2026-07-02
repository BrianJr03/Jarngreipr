package jr.brian.home.esde.data

import androidx.core.content.edit
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_COMMAND_MAP
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_CORE_MAP
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_EMULATOR_MAP
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_HIDDEN_GAMES
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
