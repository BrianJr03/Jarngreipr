package jr.brian.frontend.scrape

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User-side credential storage. ScreenScraper user account (needed
 * for per-user quota) and SteamGridDB API key.
 *
 * Phase 4a lands plain SharedPreferences storage; the plan's §3.7
 * calls for Android Keystore-wrapped values in DataStore. That's a
 * strictly-safer substitute, slotted for a follow-up (not blocking
 * the scraper's end-to-end flow). The values are **never** written
 * to a config export — Phase 0 §5 enforces that at the
 * ImportExportManager level.
 */
@Singleton
class UserScraperCredentials @Inject constructor(
    @param:ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<Snapshot> = _state.asStateFlow()

    fun setScreenScraperUser(userId: String, password: String) {
        _state.value = _state.value.copy(ssUserId = userId, ssUserPassword = password)
        prefs.edit {
            putString(KEY_SS_USER_ID, userId)
            putString(KEY_SS_USER_PASSWORD, password)
        }
    }

    fun setSteamGridDbKey(key: String) {
        _state.value = _state.value.copy(steamGridDbApiKey = key)
        prefs.edit { putString(KEY_SGDB_KEY, key) }
    }

    fun clearScreenScraperUser() = setScreenScraperUser("", "")
    fun clearSteamGridDbKey() = setSteamGridDbKey("")

    private fun load(): Snapshot = Snapshot(
        ssUserId = prefs.getString(KEY_SS_USER_ID, "").orEmpty(),
        ssUserPassword = prefs.getString(KEY_SS_USER_PASSWORD, "").orEmpty(),
        steamGridDbApiKey = prefs.getString(KEY_SGDB_KEY, "").orEmpty(),
    )

    data class Snapshot(
        val ssUserId: String,
        val ssUserPassword: String,
        val steamGridDbApiKey: String,
    ) {
        val hasScreenScraperUser: Boolean get() = ssUserId.isNotBlank() && ssUserPassword.isNotBlank()
        val hasSteamGridDbKey: Boolean get() = steamGridDbApiKey.isNotBlank()
    }

    private companion object {
        const val PREFS_NAME = "frontend_scraper_user_creds"
        const val KEY_SS_USER_ID = "ss_user_id"
        const val KEY_SS_USER_PASSWORD = "ss_user_password"
        const val KEY_SGDB_KEY = "sgdb_key"
    }
}
