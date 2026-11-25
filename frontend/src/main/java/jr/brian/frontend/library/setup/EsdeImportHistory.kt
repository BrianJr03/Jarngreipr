package jr.brian.frontend.library.setup

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.frontend.library.migrate.EsdeImportSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The last "Import from ES-DE" summary, for the Settings line. Display only, so it
 * stays out of backups (JarngreiprConfig): a restore just shows no last import
 * until the next one.
 */
@Singleton
class EsdeImportHistory @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _last = MutableStateFlow(read())

    val last: StateFlow<EsdeImportSummary?> = _last.asStateFlow()

    fun save(summary: EsdeImportSummary) {
        _last.value = summary
        prefs.edit {
            putBoolean(KEY_SAVED, true)
            putInt(KEY_GAMELISTS, summary.gamelistsFound)
            putInt(KEY_SYSTEMS, summary.systemsMatched)
            putStringSet(KEY_UNRECOGNISED, summary.unrecognisedSystems.toSet())
            putInt(KEY_GAMES, summary.gamesUpdated)
            putInt(KEY_FAVORITES, summary.favoritesAdded)
            putInt(KEY_PLAY_STATS, summary.playStatsUpdated)
            putInt(KEY_UNREADABLE, summary.unreadable)
            putInt(KEY_EMULATOR_SYSTEMS, summary.emulatorSystemsSet)
            putInt(KEY_EMULATOR_GAMES, summary.emulatorGamesSet)
            putInt(KEY_EMULATOR_KEPT, summary.emulatorKeptExisting)
            putInt(KEY_EMULATOR_SKIPPED, summary.emulatorSkipped)
        }
    }

    private fun read(): EsdeImportSummary? {
        if (!prefs.getBoolean(KEY_SAVED, false)) return null
        return EsdeImportSummary(
            gamelistsFound = prefs.getInt(KEY_GAMELISTS, 0),
            systemsMatched = prefs.getInt(KEY_SYSTEMS, 0),
            unrecognisedSystems = prefs.getStringSet(KEY_UNRECOGNISED, null).orEmpty().sorted(),
            gamesUpdated = prefs.getInt(KEY_GAMES, 0),
            favoritesAdded = prefs.getInt(KEY_FAVORITES, 0),
            playStatsUpdated = prefs.getInt(KEY_PLAY_STATS, 0),
            unreadable = prefs.getInt(KEY_UNREADABLE, 0),
            emulatorSystemsSet = prefs.getInt(KEY_EMULATOR_SYSTEMS, 0),
            emulatorGamesSet = prefs.getInt(KEY_EMULATOR_GAMES, 0),
            emulatorKeptExisting = prefs.getInt(KEY_EMULATOR_KEPT, 0),
            emulatorSkipped = prefs.getInt(KEY_EMULATOR_SKIPPED, 0),
        )
    }

    private companion object {
        const val PREFS_NAME = "jarn_esde_import"
        const val KEY_SAVED = "saved"
        const val KEY_GAMELISTS = "gamelists"
        const val KEY_SYSTEMS = "systems"
        const val KEY_UNRECOGNISED = "unrecognised"
        const val KEY_GAMES = "games"
        const val KEY_FAVORITES = "favorites"
        const val KEY_PLAY_STATS = "play_stats"
        const val KEY_UNREADABLE = "unreadable"
        const val KEY_EMULATOR_SYSTEMS = "emulator_systems"
        const val KEY_EMULATOR_GAMES = "emulator_games"
        const val KEY_EMULATOR_KEPT = "emulator_kept"
        const val KEY_EMULATOR_SKIPPED = "emulator_skipped"
    }
}
