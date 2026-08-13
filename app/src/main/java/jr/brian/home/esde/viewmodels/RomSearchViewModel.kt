package jr.brian.home.esde.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.RomIndexCache
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.data.SetupPreferences
import jr.brian.home.esde.data.SystemCacheEntry
import jr.brian.home.esde.data.SystemStamp
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.util.GamelistMetadataSource
import jr.brian.home.esde.util.NoOpMetadataSource
import jr.brian.home.esde.util.RomIndexBuilder
import jr.brian.home.esde.util.RomMetadataSource
import jr.brian.home.esde.util.mediaRoots
import jr.brian.home.model.rom.PinnedRomInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RomSearchViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val esdePreferencesManager: ESDEPreferencesManager,
    private val setupPreferences: SetupPreferences,
    private val store: RomSearchStateHolder,
) : ViewModel() {
    private val cache = RomIndexCache(context)
    /**
     * Snapshot of the last known set of roots + decoration flag. When any of
     * these change we hard-invalidate the cache — a per-system stamp check is
     * insufficient because a roots change alters which systems even exist to
     * scan.
     */
    private var lastInvalidationKey: String? = null
    val query: StateFlow<String> = store.query.asStateFlow()
    val isLoading: StateFlow<Boolean> = store.isLoading.asStateFlow()
    val focusedGame: StateFlow<GameInfo?> = store.focusedGame.asStateFlow()
    val screenDismissSignal: SharedFlow<Unit> = store.screenDismissSignal.asSharedFlow()
    val pendingRomForPin: StateFlow<Pair<Int, GameInfo>?> = store.pendingRomForPin.asStateFlow()
    val stateHolder: RomSearchStateHolder = store

    fun enterSelectMode(pageIndex: Int) {
        store.isSelectMode.value = true
        store.pendingSelectPageIndex.value = pageIndex
    }

    fun clearPendingRomForPin() {
        store.pendingRomForPin.value = null
    }

    fun requestRomLaunch(rom: PinnedRomInfo) {
        store.pendingRomToLaunch.value = rom
    }

    private val esdeRootPath: String?
        get() = File(setupPreferences.scriptsPath).parentFile?.absolutePath

    private val mediaPaths: List<String>
        get() {
            val state = esdePreferencesManager.state.value
            val primary = state.customMediaPath ?: setupPreferences.mediaPath
            val secondary = if (state.secondaryMediaEnabled)
                SetupPreferences.RETRO_HRAI_PATH else null
            return mediaRoots(primary, secondary)
        }

    fun updateQuery(q: String) {
        store.query.value = q
    }

    fun dismiss() {
        store.query.value = ""
        store.focusedGame.value = null
        store.dismissSignal.tryEmit(Unit)
    }

    fun clearState() {
        store.query.value = ""
        store.focusedGame.value = null
    }

    fun loadGames() {
        if (store.isLoading.value || store.allGames.value.isNotEmpty()) return
        val rootPath = esdeRootPath ?: return
        store.isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            parseAndStore(rootPath, forceRescan = false)
            store.isLoading.value = false
        }
    }

    /**
     * Force a full re-scan, bypassing the cache. Used by the settings refresh
     * action after the user has scraped or added ROMs externally. Still no-ops
     * when a parse is already in flight so a double-press cannot run two
     * concurrent parses over the same [RomSearchStateHolder.allGames].
     *
     * [onComplete] receives the resulting game and system counts.
     */
    fun refreshGames(onComplete: (games: Int, systems: Int) -> Unit = { _, _ -> }) {
        if (store.isLoading.value) return
        val rootPath = esdeRootPath ?: return
        store.isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val (games, systems) = parseAndStore(rootPath, forceRescan = true)
            store.isLoading.value = false
            onComplete(games, systems)
        }
    }

    /**
     * Two-phase load:
     *
     *  1. If a cache exists and the roots/decoration invalidation key matches,
     *     paint the store with the cached list immediately so the UI has
     *     something to render.
     *  2. Build a live stamp per system, reconcile against the cache, and
     *     rescan only the systems whose stamps changed. When [forceRescan] is
     *     true, step 1 still paints the cached list for continuity but the
     *     cache is invalidated afterwards and every system is rebuilt.
     */
    private suspend fun parseAndStore(
        rootPath: String,
        forceRescan: Boolean,
    ): Pair<Int, Int> {
        val prefsState = esdePreferencesManager.state.value
        val decorationEnabled = prefsState.gamelistDecorationEnabled
        val invalidationKey = buildInvalidationKey(prefsState.romsPaths, decorationEnabled)

        // Hard invalidate when the roots or decoration flag changed under us,
        // or when the caller asked for it. A per-system stamp check is not
        // enough — the roots set defines which systems even exist to scan.
        val hardInvalidate = forceRescan ||
            (lastInvalidationKey != null && lastInvalidationKey != invalidationKey)
        if (hardInvalidate) cache.invalidateAll()
        lastInvalidationKey = invalidationKey

        val cached = if (hardInvalidate) emptyMap() else cache.loadAll()

        // Paint the cached list first so the UI has something while we reconcile.
        if (cached.isNotEmpty()) {
            store.allGames.value = cached.values
                .flatMap { it.games }
                .sortedWith(compareBy({ it.name.lowercase() }, { it.systemName.trim() }))
        }

        val esSystemsFile = File(rootPath, "custom_systems/es_systems.xml")
        val metadataSource: RomMetadataSource =
            if (decorationEnabled) {
                GamelistMetadataSource(esdeRootPath = rootPath, mediaPaths = mediaPaths)
            } else {
                NoOpMetadataSource
            }

        // Discover every candidate system by walking each root's immediate
        // children — cheap, one-level listFiles per root, no recursion.
        val candidateSystems = prefsState.romsPaths.flatMap { root ->
            File(root).listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
        }.distinct()

        val nextCache = HashMap<String, SystemCacheEntry>()
        for (systemName in candidateSystems) {
            val liveStamp = RomIndexBuilder.stamp(
                systemName = systemName,
                romsPaths = prefsState.romsPaths,
                esdeRootPath = rootPath,
                decorationEnabled = decorationEnabled,
            ) ?: continue
            val existing = cached[systemName]
            if (existing != null && existing.stamp.matches(liveStamp)) {
                nextCache[systemName] = existing
                continue
            }
            val games = RomIndexBuilder.buildForSystem(
                systemName = systemName,
                romsPaths = prefsState.romsPaths,
                esSystemsFile = esSystemsFile.takeIf { it.exists() },
                metadataSource = metadataSource,
                emulatorPackage = prefsState.systemAppMap[systemName],
            )
            if (games.isNotEmpty()) {
                nextCache[systemName] = SystemCacheEntry(stamp = liveStamp, games = games)
            }
        }

        cache.saveAll(nextCache)

        val sorted = nextCache.values
            .flatMap { it.games }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.systemName.trim() }))
        store.allGames.value = sorted
        val systemCount = sorted.mapTo(mutableSetOf()) { it.systemName }.size
        return sorted.size to systemCount
    }

    /**
     * Fingerprint of the inputs that must invalidate every cache entry at once.
     * Per-system stamp checks handle within-system drift; this catches
     * across-system changes (roots reordered, decoration flipped).
     */
    private fun buildInvalidationKey(
        romsPaths: List<String>,
        decorationEnabled: Boolean,
    ): String = romsPaths.joinToString("|") + "||decoration=$decorationEnabled"
}
