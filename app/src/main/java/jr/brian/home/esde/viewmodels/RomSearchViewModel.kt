package jr.brian.home.esde.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.RomIndexCache
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.data.RootAvailability
import jr.brian.home.esde.data.SetupPreferences
import jr.brian.home.esde.data.SystemCacheEntry
import jr.brian.home.esde.data.SystemStamp
import jr.brian.home.esde.data.computeRootAvailability
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.scraper.sidecar.UserMetadataOverride
import jr.brian.home.esde.scraper.sidecar.UserMetadataOverrideStore
import jr.brian.home.esde.util.CompositeMetadataSource
import jr.brian.home.esde.util.GamelistMetadataSource
import jr.brian.home.esde.util.MediaFallbackMetadataSource
import jr.brian.home.esde.util.NoOpMetadataSource
import jr.brian.home.esde.util.UserOverrideMetadataSource
import jr.brian.home.esde.util.RomIndexBuilder
import jr.brian.home.esde.util.RomMetadataSource
import jr.brian.home.esde.util.RomScanner
import jr.brian.home.esde.util.SidecarMetadataSource
import jr.brian.home.esde.util.mediaRoots
import jr.brian.home.esde.scraper.ScraperCoordinator
import jr.brian.core.model.ApplicationScope
import jr.brian.home.model.rom.PinnedRomInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class RomSearchViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val esdePreferencesManager: ESDEPreferencesManager,
    private val setupPreferences: SetupPreferences,
    private val store: RomSearchStateHolder,
    private val scraperCoordinator: ScraperCoordinator,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
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
    val systemScanLoaded: StateFlow<Int> = store.systemScanLoaded.asStateFlow()
    val systemScanTotal: StateFlow<Int> = store.systemScanTotal.asStateFlow()
    val focusedGame: StateFlow<GameInfo?> = store.focusedGame.asStateFlow()
    val screenDismissSignal: SharedFlow<Unit> = store.screenDismissSignal.asSharedFlow()
    val openEsdeSettingsSignal: SharedFlow<Unit> = store.openEsdeSettingsSignal.asSharedFlow()
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
            val tertiary = if (state.jarngreiprMediaEnabled)
                scraperCoordinator.scrapedRoot() else null
            return mediaRoots(primary, secondary, tertiary)
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
        // Scan on the application-scoped supervisor, not viewModelScope: the
        // activity hosting the frontend gets finish()'d by RomGameLauncher
        // every time a game launches, which cancels viewModelScope mid-parse
        // and used to leave the @Singleton isLoading flag stuck at true for
        // the life of the process. `invokeOnCompletion` is used instead of
        // try/finally so the reset runs even if the launch is cancelled
        // before its body ever starts (synchronous .value = true above is
        // paired with the completion handler below).
        val job = applicationScope.launch {
            parseAndStore(rootPath, forceRescan = false)
        }
        job.invokeOnCompletion { store.isLoading.value = false }
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
        var games = 0
        var systems = 0
        val job = applicationScope.launch {
            val result = parseAndStore(rootPath, forceRescan = true)
            games = result.first
            systems = result.second
        }
        job.invokeOnCompletion { cause ->
            store.isLoading.value = false
            if (cause == null) onComplete(games, systems)
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
        var count = 0
        val job = applicationScope.launch {
            count = rebuildSystemIntoCache(systemName, rootPath)
        }
        job.invokeOnCompletion { cause ->
            store.isLoading.value = false
            if (cause == null) onComplete(count)
        }
    }

    /**
     * Returns the override currently persisted for [game], or `null` when the
     * user has never written one. Called off the main thread from the Edit
     * Info sheet to prefill the form.
     */
    fun readUserOverride(game: GameInfo): UserMetadataOverride? {
        val basename = File(game.path).nameWithoutExtension.ifBlank { return null }
        return UserMetadataOverrideStore.read(
            scrapedRoot = scraperCoordinator.scrapedRoot(),
            systemName = game.systemName,
            basename = basename,
        )
    }

    /**
     * Persists an override for [game] and triggers a system refresh so the
     * UI sees the new metadata. Writing an [override] whose fields are all
     * null is equivalent to [clearUserOverride].
     */
    fun writeUserOverride(game: GameInfo, override: UserMetadataOverride, onComplete: () -> Unit = {}) {
        val basename = File(game.path).nameWithoutExtension.ifBlank {
            onComplete()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            UserMetadataOverrideStore.write(
                scrapedRoot = scraperCoordinator.scrapedRoot(),
                systemName = game.systemName,
                basename = basename,
                override = override,
            )
            refreshSystem(game.systemName) { onComplete() }
        }
    }

    /** Deletes the override + any copied images for [game], then refreshes. */
    fun clearUserOverride(game: GameInfo, onComplete: () -> Unit = {}) {
        val basename = File(game.path).nameWithoutExtension.ifBlank {
            onComplete()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            UserMetadataOverrideStore.delete(
                scrapedRoot = scraperCoordinator.scrapedRoot(),
                systemName = game.systemName,
                basename = basename,
            )
            refreshSystem(game.systemName) { onComplete() }
        }
    }

    /**
     * Copies a user-picked image into the override image directory and returns
     * its absolute path, suitable for storing on a [UserMetadataOverride].
     * Returns `null` on IO failure.
     */
    fun copyUserOverrideImage(game: GameInfo, slot: String, sourceUri: Uri): String? {
        val basename = File(game.path).nameWithoutExtension.ifBlank { return null }
        return UserMetadataOverrideStore.copyImage(
            context = context,
            scrapedRoot = scraperCoordinator.scrapedRoot(),
            systemName = game.systemName,
            basename = basename,
            slot = slot,
            sourceUri = sourceUri,
        )
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
    /**
     * Composes the metadata layer the index builder sees. User overrides
     * (via [UserOverrideMetadataSource], written through the Edit Info sheet)
     * sit at the top and win on any field they set. Below them ES-DE's
     * gamelist.xml stays authoritative (via [GamelistMetadataSource]);
     * [SidecarMetadataSource] fills text nulls with scraper-written JSON
     * under `scrapedRoot()`; [MediaFallbackMetadataSource] fills image-path
     * nulls for scans no gamelist entry decorates (PC shortcuts the user
     * never scraped in ES-DE are the canonical case). When gamelist
     * decoration is off we drop all four — the user explicitly opted out
     * of decoration, and layering anything on would re-enable it through
     * the back door.
     */
    private fun buildMetadataSource(
        decorationEnabled: Boolean,
        rootPath: String,
        mediaPaths: List<String>,
    ): RomMetadataSource {
        if (!decorationEnabled) return NoOpMetadataSource
        val scrapedRoot = scraperCoordinator.scrapedRoot()
        val userOverrides = UserOverrideMetadataSource(scrapedRoot = scrapedRoot)
        val gamelist = GamelistMetadataSource(
            esdeRootPath = rootPath,
            mediaPaths = mediaPaths,
            parseCacheDir = context.filesDir,
        )
        val sidecar = SidecarMetadataSource(scrapedRoot = scrapedRoot)
        val mediaFallback = MediaFallbackMetadataSource(mediaPaths = mediaPaths)
        return CompositeMetadataSource(
            primary = userOverrides,
            fallback = CompositeMetadataSource(
                primary = CompositeMetadataSource(primary = gamelist, fallback = sidecar),
                fallback = mediaFallback,
            ),
        )
    }

    private suspend fun rebuildSystemIntoCache(systemName: String, rootPath: String): Int {
        val prefsState = esdePreferencesManager.state.value
        val decorationEnabled = prefsState.gamelistDecorationEnabled
        val esSystemsFile = File(rootPath, "custom_systems/es_systems.xml")
        val metadataSource: RomMetadataSource = buildMetadataSource(
            decorationEnabled = decorationEnabled,
            rootPath = rootPath,
            mediaPaths = mediaPaths,
        )
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
    ): Pair<Int, Int> = coroutineScope {
        // Reset the scan progress counters up front so the frontend's loading
        // badge only reflects *this* scan — a prior degraded scan's totals
        // would otherwise linger and show stale progress.
        store.systemScanLoaded.value = 0
        store.systemScanTotal.value = 0

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
        val metadataSource: RomMetadataSource = buildMetadataSource(
            decorationEnabled = decorationEnabled,
            rootPath = rootPath,
            mediaPaths = mediaPaths,
        )

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
        // Skip systems the user has hidden from the frontend — reconciling them
        // just to have the UI filter them out downstream is pure overhead on
        // activity start. Their cached entries ride through untouched via the
        // `nextCache` seed, so unhiding later still restores whatever was last
        // scanned until the next natural reconcile replaces it.
        val hiddenSystems = prefsState.hiddenSystems
        val candidateSystems = (rootCandidates + mappingCandidates + cached.keys)
            .distinct()
            .filter { it !in hiddenSystems }
        store.systemScanTotal.value = candidateSystems.size

        val perSystemExtensions = esSystemsFile
            .takeIf { it.exists() }
            ?.let(RomScanner::parseSystemExtensions)
            .orEmpty()

        // Seed `nextCache` with the cached snapshot so incremental publishes
        // never regress below the painted cache. Each system's rebuild then
        // either replaces its entry (fresh games), removes it (confirmed
        // gone), or leaves the cached entry in place (source unavailable).
        // The UI reads `allGames` and renders whatever's there — the user
        // sees cached games instantly and watches rebuilt systems replace
        // them live instead of staring at a spinner for the full reconcile.
        val nextCache = ConcurrentHashMap<String, SystemCacheEntry>().apply { putAll(cached) }
        val publishLock = Mutex()
        val semaphore = Semaphore(RECONCILE_PARALLELISM)

        suspend fun publishIncremental() {
            publishLock.withLock {
                store.allGames.value = nextCache.values
                    .flatMap { it.games }
                    .sortedWith(compareBy({ it.name.lowercase() }, { it.systemName.trim() }))
            }
        }

        // Prime the UI with the seeded snapshot before any worker fires. The
        // spinner gate in `SystemGrid`/`RomResultsGrid` is now `isLoading &&
        // items.isEmpty()`, so a cached paint flips the view off the spinner
        // immediately.
        if (cached.isNotEmpty()) publishIncremental()

        val jobs = candidateSystems.map { systemName ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    runCatching {
                        reconcileSystem(
                            systemName = systemName,
                            cached = cached,
                            nextCache = nextCache,
                            availability = availability,
                            prefsState = prefsState,
                            rootPath = rootPath,
                            esSystemsFile = esSystemsFile,
                            metadataSource = metadataSource,
                            perSystemExtensions = perSystemExtensions,
                            decorationEnabled = decorationEnabled,
                        )
                    }.onFailure { Log.w(TAG, "Reconcile failed for $systemName", it) }
                    store.systemScanLoaded.update { it + 1 }
                    publishIncremental()
                }
            }
        }
        jobs.awaitAll()

        cache.saveAll(nextCache.toMap())

        val sorted = nextCache.values
            .flatMap { it.games }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.systemName.trim() }))
        store.allGames.value = sorted
        val systemCount = sorted.mapTo(mutableSetOf()) { it.systemName }.size
        sorted.size to systemCount
    }

    /**
     * Reconcile one system against the live filesystem, mutating [nextCache]
     * in place. [nextCache] starts seeded with the cached snapshot, so a
     * `remove` here is how we signal a confirmed drop and an untouched entry
     * is how we carry the cache forward (SD card unmounted, no live stamp to
     * compare against).
     */
    private suspend fun reconcileSystem(
        systemName: String,
        cached: Map<String, SystemCacheEntry>,
        nextCache: ConcurrentHashMap<String, SystemCacheEntry>,
        availability: RootAvailability,
        prefsState: jr.brian.home.esde.model.ESDEPrefsState,
        rootPath: String,
        esSystemsFile: File,
        metadataSource: RomMetadataSource,
        perSystemExtensions: Map<String, Set<String>>,
        decorationEnabled: Boolean,
    ) {
        val resolvedExtensions = RomScanner.extensionsFor(systemName, perSystemExtensions)
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
            // now, carry the entry forward (which it already is via seed).
            val existing = cached[systemName]
            if (existing != null && !availability.cachedEntryIsFromUnavailableSource(existing)) {
                nextCache.remove(systemName)
            }
            return
        }
        val existing = cached[systemName]
        if (existing != null && existing.stamp.matches(liveStamp)) {
            // Unchanged; nothing to do — the seeded entry is still valid.
            return
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
            return
        }
        // Non-null stamp with zero games is ambiguous: it's the "user deleted
        // every ROM" case AND the mapped-system-on-unmounted-SAF case (the
        // mapping fallback returns a non-null stamp with mtime 0 / count 0
        // rather than null). Use the same availability veto as the null-stamp
        // branch so the second case never destroys the index.
        if (existing != null && !availability.cachedEntryIsFromUnavailableSource(existing)) {
            nextCache.remove(systemName)
        }
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
        val job = applicationScope.launch {
            parseAndStore(rootPath, forceRescan = false)
        }
        job.invokeOnCompletion { store.isLoading.value = false }
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

        /**
         * Max concurrent per-system reconciles. Bounded because each worker
         * walks SAF / filesystem and parses XML; 4 keeps IO busy without
         * thrashing. `Dispatchers.IO`'s pool is much larger, so this is the
         * only backpressure stopping ~50 systems from scanning in parallel.
         */
        private const val RECONCILE_PARALLELISM = 4
    }
}
