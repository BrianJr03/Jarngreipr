package jr.brian.home.esde.data

import androidx.core.content.edit
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SCRAPER_CONCURRENCY_OVERRIDE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SCREENSCRAPER_DEV_ID
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SCREENSCRAPER_DEV_PASSWORD
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SCREENSCRAPER_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SCREENSCRAPER_MEDIA_TYPES
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SCREENSCRAPER_USER_ID
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SCREENSCRAPER_USER_PASSWORD
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_STEAMGRIDDB_API_KEY
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_STEAMGRIDDB_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_STEAMGRIDDB_MEDIA_TYPES
import org.json.JSONArray

fun ESDEPreferencesManager.setScreenScraperEnabled(enabled: Boolean) {
    _state.value = _state.value.copy(screenScraperEnabled = enabled)
    prefs.edit { putBoolean(KEY_SCREENSCRAPER_ENABLED, enabled) }
}

fun ESDEPreferencesManager.setScreenScraperDevId(value: String) {
    _state.value = _state.value.copy(screenScraperDevId = value)
    prefs.edit { putString(KEY_SCREENSCRAPER_DEV_ID, value) }
}

fun ESDEPreferencesManager.setScreenScraperDevPassword(value: String) {
    _state.value = _state.value.copy(screenScraperDevPassword = value)
    prefs.edit { putString(KEY_SCREENSCRAPER_DEV_PASSWORD, value) }
}

fun ESDEPreferencesManager.setScreenScraperUserId(value: String) {
    _state.value = _state.value.copy(screenScraperUserId = value)
    prefs.edit { putString(KEY_SCREENSCRAPER_USER_ID, value) }
}

fun ESDEPreferencesManager.setScreenScraperUserPassword(value: String) {
    _state.value = _state.value.copy(screenScraperUserPassword = value)
    prefs.edit { putString(KEY_SCREENSCRAPER_USER_PASSWORD, value) }
}

fun ESDEPreferencesManager.setScreenScraperMediaTypes(types: Set<String>) {
    _state.value = _state.value.copy(screenScraperMediaTypes = types)
    persistMediaTypes(KEY_SCREENSCRAPER_MEDIA_TYPES, types)
}

fun ESDEPreferencesManager.setSteamGridDbEnabled(enabled: Boolean) {
    _state.value = _state.value.copy(steamGridDbEnabled = enabled)
    prefs.edit { putBoolean(KEY_STEAMGRIDDB_ENABLED, enabled) }
}

fun ESDEPreferencesManager.setSteamGridDbApiKey(value: String) {
    _state.value = _state.value.copy(steamGridDbApiKey = value)
    prefs.edit { putString(KEY_STEAMGRIDDB_API_KEY, value) }
}

fun ESDEPreferencesManager.setSteamGridDbMediaTypes(types: Set<String>) {
    _state.value = _state.value.copy(steamGridDbMediaTypes = types)
    persistMediaTypes(KEY_STEAMGRIDDB_MEDIA_TYPES, types)
}

fun ESDEPreferencesManager.setScraperConcurrencyOverride(value: Int) {
    val coerced = value.coerceIn(0, 16)
    _state.value = _state.value.copy(scraperConcurrencyOverride = coerced)
    prefs.edit { putInt(KEY_SCRAPER_CONCURRENCY_OVERRIDE, coerced) }
}

private fun ESDEPreferencesManager.persistMediaTypes(key: String, types: Set<String>) {
    if (types.isEmpty()) {
        prefs.edit { remove(key) }
    } else {
        prefs.edit { putString(key, JSONArray(types.toList()).toString()) }
    }
}
