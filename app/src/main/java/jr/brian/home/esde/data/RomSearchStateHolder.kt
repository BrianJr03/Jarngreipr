package jr.brian.home.esde.data

import jr.brian.home.esde.model.FrontendRoute
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.model.rom.PinnedRomInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RomSearchStateHolder @Inject constructor() {
    val query = MutableStateFlow("")
    val allGames = MutableStateFlow<List<GameInfo>>(emptyList())
    val isLoading = MutableStateFlow(false)
    val dismissSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val screenDismissSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val gameLaunchSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val showSearchKeyboardSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openedFromFrontend = MutableStateFlow(false)
    val focusedGame = MutableStateFlow<GameInfo?>(null)
    val hintAndKbVisible = MutableStateFlow(true)

    val isSelectMode = MutableStateFlow(false)
    val pendingSelectPageIndex = MutableStateFlow(-1)
    val pendingRomForPin = MutableStateFlow<Pair<Int, GameInfo>?>(null)
    val pendingRomToLaunch = MutableStateFlow<PinnedRomInfo?>(null)

    val currentRoute = MutableStateFlow<FrontendRoute>(FrontendRoute.Systems)

    /**
     * Last system the user focused on the System screen, by name. Lives at @Singleton scope
     * so it survives the route switch to Games and back — without it, returning to Systems
     * would reset focus to index 0 (the first tile alphabetically).
     */
    val lastFocusedSystem = MutableStateFlow<String?>(null)

    /**
     * Per-system last-focused game (keyed by system name → game.path). Singleton scope so
     * it survives RomSearchResultsActivity.finish() on launch — when the user returns to
     * Games(system), focus restores to the game they launched rather than tile 0.
     */
    val lastFocusedGameBySystem = MutableStateFlow<Map<String, String>>(emptyMap())

    /**
     * Lookup by (system name, ES-DE-style path or filename) against the built
     * index. Used by callers that historically re-parsed gamelist.xml on
     * demand — the ROM index already carries scraped titles, descriptions,
     * and art, so consulting it costs nothing and avoids repeated XML parses.
     *
     * Matches on `game.path` first (may be `./Foo.iso` or `./Sub/Foo.iso`), then
     * on filename basename. Returns null when the index has no entry for the
     * filename, which is the same signal the old regex-based reader gave.
     */
    fun findGame(systemName: String, filename: String): GameInfo? {
        val games = allGames.value.filter { it.systemName.equals(systemName, ignoreCase = true) }
        if (games.isEmpty()) return null
        val normalized = filename.removePrefix("./").trim()
        games.firstOrNull { it.path.removePrefix("./") == normalized }?.let { return it }
        val basename = File(normalized).name
        return games.firstOrNull { File(it.path).name == basename }
    }
}
