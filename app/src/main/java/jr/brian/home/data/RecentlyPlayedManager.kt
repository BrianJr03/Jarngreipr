package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.model.rom.PinnedRomInfo
import jr.brian.home.model.rom.toPinnedRomInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class RecentlyPlayedEntry(
    val rom: PinnedRomInfo,
    val launchedAt: Long,
    val launchCount: Int = 1
)

/**
 * In-memory, SharedPreferences-backed store of the last N ROMs the user
 * launched through Jarngreipr's own [jr.brian.home.esde.ui.RomGameLauncher].
 *
 * Writers are the three [RomGameLauncher] success paths (regular launch,
 * explicit emulator-pick, saved ES-DE command); each calls [record] right
 * before `startActivity`. External ES-DE launches aren't recorded.
 *
 * Entries are kept newest-first and deduplicated by [PinnedRomInfo.key]
 * (`"<system>/<path>"`) — relaunching an existing entry moves it to the top
 * and bumps [RecentlyPlayedEntry.launchCount] instead of inserting a dupe.
 *
 * [MAX_ENTRIES] is the retention cap (older entries fall off); [DISPLAY_COUNT]
 * is the default number the Last Played widget surfaces. The two differ so
 * callers (Random picker, a future "see more" sheet) can draw from a deeper
 * pool than what fits on the tile.
 */
@Singleton
class RecentlyPlayedManager internal constructor(
    context: Context,
    private val timeProvider: () -> Long
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context, System::currentTimeMillis)

    private val json = Json { ignoreUnknownKeys = true }
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _entries = MutableStateFlow<List<RecentlyPlayedEntry>>(load())
    val entries: StateFlow<List<RecentlyPlayedEntry>> = _entries.asStateFlow()

    /**
     * Record a launch of [game]. Moves an existing entry with the same
     * [PinnedRomInfo.key] to the front and increments its [RecentlyPlayedEntry.launchCount];
     * otherwise prepends a fresh entry. Trims the list to [MAX_ENTRIES] from
     * the oldest end.
     */
    fun record(game: GameInfo) {
        val rom = game.toPinnedRomInfo()
        val now = timeProvider()
        val current = _entries.value
        val existing = current.firstOrNull { it.rom.key == rom.key }
        val updated = if (existing != null) {
            val moved = RecentlyPlayedEntry(
                rom = rom,
                launchedAt = now,
                launchCount = existing.launchCount + 1
            )
            listOf(moved) + current.filter { it.rom.key != rom.key }
        } else {
            listOf(RecentlyPlayedEntry(rom = rom, launchedAt = now)) + current
        }.take(MAX_ENTRIES)
        _entries.value = updated
        save(updated)
    }

    fun remove(key: String) {
        val updated = _entries.value.filter { it.rom.key != key }
        if (updated.size == _entries.value.size) return
        _entries.value = updated
        save(updated)
    }

    fun clear() {
        if (_entries.value.isEmpty()) return
        _entries.value = emptyList()
        save(emptyList())
    }

    /**
     * Replace the current list wholesale. Used by config restore; the caller
     * is responsible for the list already being newest-first and trimmed.
     */
    fun restore(entries: List<RecentlyPlayedEntry>) {
        val trimmed = entries.take(MAX_ENTRIES)
        _entries.value = trimmed
        save(trimmed)
    }

    private fun load(): List<RecentlyPlayedEntry> {
        val stored = prefs.getString(ENTRIES_KEY, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<RecentlyPlayedEntry>>(stored).take(MAX_ENTRIES)
        }.getOrDefault(emptyList())
    }

    private fun save(entries: List<RecentlyPlayedEntry>) {
        prefs.edit { putString(ENTRIES_KEY, json.encodeToString(entries)) }
    }

    companion object {
        const val MAX_ENTRIES = 20
        const val DISPLAY_COUNT = 5

        private const val PREFS_NAME = "recently_played_prefs"
        private const val ENTRIES_KEY = "recently_played_v1"
    }
}
