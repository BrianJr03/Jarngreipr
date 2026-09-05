package jr.brian.home.esde.viewmodels

import android.content.Context
import android.util.Log
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
import jr.brian.home.esde.data.computeRootAvailability
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.util.GamelistMetadataSource
import jr.brian.home.esde.util.NoOpMetadataSource
import jr.brian.home.esde.util.RomIndexBuilder
import jr.brian.home.esde.util.RomMetadataSource
import jr.brian.home.esde.util.RomScanner
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

    /**
     * True when the most recent scan observed a configured root or mapping
     * whose backing storage was unreachable. Read by [retryIfLastScanWasDegraded]
     * so a later `ACTION_MEDIA_MOUNTED` broadcast can re-scan without racing
     * a normal steady-state load.
     */
    @Volatile
    private var lastScanWasDegraded: Boolean = false
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
     * Unconditionally rebuild a single system's cache entry, bypassing the
     * stamp check. Sibling to [refreshGames] for when the user has scraped or
     * added ROMs for one platform and the whole-library refresh would be
     * needlessly expensive — the stamp is precisely what fails to notice an
     * external scrape, so we skip it entirely here.
     *
     * Shares the [store]'s `isLoading` guard with [refreshGames] and [loadGames]
     * so the three can't race on `store.allGames`. If the rebuild finds no
     * games the entry is dropped from the cache — otherwise a platform whose
     * ROMs were deleted keeps its tile in the grid until the next full refresh.
     *
     * [onComplete] receives the game count for the rebuilt system (0 if empty
     * or unbuildable).
     */
    fun refreshSystem(systemName: String, onComplete: (games: Int) -> Unit = {}) {
        if (store.isLoading.value) return
        val rootPath = esdeRootPath ?: return
        store.isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val count = try {
                rebuildSystemIntoCache(systemName, rootPath)
            } finally {
                store.isLoading.value = false
            }
            onComplete(count)
        }
    }

    /**
     * Runs one system through the same scan → stamp → cache-merge pipeline
     * [parseAndStore] uses per system, but without the stamp-matches short
     * circuit. Returns the resulting game count.
     *
     * The merged cache map is the source of truth for the store: `allGames` is
     * rederived from it after the write so a partial rebuild can't leave the
     * two out of sync.
     */
    private suspend fun rebuildSystemIntoCache(systemName: String, rootPath: String): Int {
        val prefsState = esdePreferencesManager.state.value
        val decorationEnabled = prefsState.gamelistDecorationEnabled
        val esSystemsFile = File(rootPath, "custom_systems/es_systems.xml")
        val metadataSource: RomMetadataSource =
            if (decorationEnabled) {
                GamelistMetadataSource(esdeRootPath = rootPath, mediaPaths = mediaPaths)
            } else {
                NoOpMetadataSource
            }
        val perSystemExtensions = esSystemsFile
            .takeIf { it.exists() }
            ?.let(RomScanner::parseSystemExtensions)
            .orEmpty()
        val resolvedExtensions = RomScanner.extensionsFor(systemName, perSystemExtensions)

        val games = RomIndexBuilder.buildForSystem(
            systemName = systemName,
            romsPaths = prefsState.romsPaths,
            esSystemsFile = esSystemsFile.takeIf { it.exists() },
            metadataSource = metadataSource,
            systemFolderMappings = prefsState.systemFolderMappings,
            context = context,
            emulatorPackage = prefsState.systemAppMap[systemName],
        )
        val liveStamp = RomIndexBuilder.stamp(
            systemName = systemName,
            romsPaths = prefsState.romsPaths,
            esdeRootPath = rootPath,
            decorationEnabled = decorationEnabled,
            systemFolderMappings = prefsState.systemFolderMappings,
            context = context,
            resolvedExtensions = resolvedExtensions,
        )

        val merged = cache.loadAll().toMutableMap()
        if (games.isEmpty() || liveStamp == null) {
            // Drop the entry entirely — leaving the stale list in place would
            // keep a deleted platform's tile in the grid until the next full
            // refresh.
            merged.remove(systemName)
        } else {
            merged[systemName] = SystemCacheEntry(stamp = liveStamp, games = games)
        }
        cache.saveAll(merged)

        store.allGames.value = merged.values
            .flatMap { it.games }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.systemName.trim() }))
        return games.size
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
     *
     * When a configured root or mapping is unreachable at scan time (SD card
     * not yet mounted after boot is the canonical case), its cached systems
     * are carried forward instead of pruned — the cache's save-set is a
     * deletion signal, so treating a temporary unmount as "these systems no
     * longer exist" would destroy the persisted index. See [RootAvailability].
     */
    internal suspend fun parseAndStore(
        rootPath: String,
        forceRescan: Boolean,
    ): Pair<Int, Int> {
        val prefsState = esdePreferencesManager.state.value
        val decorationEnabled = prefsState.gamelistDecorationEnabled
        val invalidationKey = buildInvalidationKey(
            romsPaths = prefsState.romsPaths,
            decorationEnabled = decorationEnabled,
            mappings = prefsState.systemFolderMappings,
        )

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

        val availability = computeRootAvailability(
            romsPaths = prefsState.romsPaths,
            mappings = prefsState.systemFolderMappings,
        )
        if (availability.anyMissing) {
            // Silent `.filter { it.exists() }` inside the scanner is precisely
            // what let this fail invisibly before — a WARN here makes the
            // degraded scan observable in logcat.
            Log.w(
                TAG,
                "Degraded scan: unreachable roots=${availability.unavailableRoots}, " +
                    "unreachable mappings=${availability.unavailableMappingUris}",
            )
        }
        lastScanWasDegraded = availability.anyMissing

        val esSystemsFile = File(rootPath, "custom_systems/es_systems.xml")
        val metadataSource: RomMetadataSource =
            if (decorationEnabled) {
                GamelistMetadataSource(esdeRootPath = rootPath, mediaPaths = mediaPaths)
            } else {
                NoOpMetadataSource
            }

        // Discover every candidate system by walking each root's immediate
        // children — cheap, one-level listFiles per root, no recursion — plus
        // every explicit mapping the user has declared. Cached keys are also
        // seeded so a system whose only root is currently unavailable still
        // enters the loop and can be carried forward from the cache; without
        // this, an unmounted SD card's systems would never even be visited.
        val rootCandidates = prefsState.romsPaths.flatMap { root ->
            File(root).listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
        }
        val mappingCandidates = prefsState.systemFolderMappings.map { it.systemName }
        val candidateSystems = (rootCandidates + mappingCandidates + cached.keys).distinct()

        val perSystemExtensions = esSystemsFile
            .takeIf { it.exists() }
            ?.let(RomScanner::parseSystemExtensions)
            .orEmpty()

        val nextCache = HashMap<String, SystemCacheEntry>()
        for (systemName in candidateSystems) {
            val resolvedExtensions =
                RomScanner.extensionsFor(systemName, perSystemExtensions)
            val liveStamp = RomIndexBuilder.stamp(
                systemName = systemName,
                romsPaths = prefsState.romsPaths,
                esdeRootPath = rootPath,
                decorationEnabled = decorationEnabled,
                systemFolderMappings = prefsState.systemFolderMappings,
                context = context,
                resolvedExtensions = resolvedExtensions,
            )
            if (liveStamp == null) {
                // The scan couldn't observe this system anywhere. Only drop it
                // when we actually looked at every configured source — if the
                // source that produced the cached entry is unreachable right
                // now, carry the entry forward instead of destroying the index.
                cached[systemName]
                    ?.takeIf { availability.cachedEntryIsFromUnavailableSource(it) }
                    ?.let { nextCache[systemName] = it }
                continue
            }
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
                systemFolderMappings = prefsState.systemFolderMappings,
                context = context,
                emulatorPackage = prefsState.systemAppMap[systemName],
            )
            if (games.isNotEmpty()) {
                nextCache[systemName] = SystemCacheEntry(stamp = liveStamp, games = games)
            } else {
                // A non-null stamp with zero games is ambiguous: it's the
                // fully-mounted "user deleted every ROM" case AND the mapped-
                // system-on-unmounted-SAF case (the mapping fallback returns a
                // non-null stamp with mtime 0 / count 0 rather than null). Use
                // the same availability veto as the null-stamp branch so the
                // second case never destroys the index.
                cached[systemName]
                    ?.takeIf { availability.cachedEntryIsFromUnavailableSource(it) }
                    ?.let { nextCache[systemName] = it }
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
     * Re-run [parseAndStore] iff the previous scan was degraded. Called by the
     * `ACTION_MEDIA_MOUNTED` receiver in [jr.brian.home.esde.ui.FrontEndActivity]
     * so an SD card that finished mounting after boot can restore the
     * frontend without the user hitting Refresh Library.
     *
     * Deliberately does not touch the `allGames.isNotEmpty()` half of
     * [loadGames]'s guard — the cached list may already be painted, but the
     * scan itself must still run to reconcile against the now-mounted volume.
     * The `isLoading` half is honored so this cannot fight a manual refresh
     * that happens to overlap.
     */
    fun retryIfLastScanWasDegraded() {
        if (!lastScanWasDegraded) return
        if (store.isLoading.value) return
        val rootPath = esdeRootPath ?: return
        store.isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            parseAndStore(rootPath, forceRescan = false)
            store.isLoading.value = false
        }
    }

    /**
     * Fingerprint of the inputs that must invalidate every cache entry at once.
     * Per-system stamp checks handle within-system drift; this catches
     * across-system changes (roots reordered, decoration flipped, a mapping
     * added or removed — mapping list changes can shift which systems exist).
     */
    private fun buildInvalidationKey(
        romsPaths: List<String>,
        decorationEnabled: Boolean,
        mappings: List<jr.brian.home.esde.model.SystemFolderMapping>,
    ): String = buildString {
        append(romsPaths.joinToString("|"))
        append("||decoration=$decorationEnabled")
        append("||mappings=")
        append(
            mappings.sortedBy { it.systemName + it.treeUri }
                .joinToString(",") { "${it.systemName}=${it.treeUri}" }
        )
    }

    companion object {
        private const val TAG = "RomSearchViewModel"
    }
}
