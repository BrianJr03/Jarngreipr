package jr.brian.home.esde.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.core.model.ApplicationScope
import jr.brian.frontend.library.LibraryRepository
import jr.brian.frontend.library.LibraryState
import jr.brian.frontend.library.migrate.LibraryMigration
import jr.brian.home.data.FavoritesGateway
import jr.brian.home.data.LibraryGameInfoAdapter
import jr.brian.home.data.LibraryStoreSync
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.scraper.ScraperCoordinator
import jr.brian.home.esde.scraper.sidecar.UserMetadataOverride
import jr.brian.home.esde.scraper.sidecar.UserMetadataOverrideStore
import jr.brian.home.model.rom.PinnedRomInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Phase 2.5 ViewModel: no longer owns a scanner. All ROM discovery
 * goes through [LibraryRepository.scanAll] / [scanSystem] which
 * writes directly to Room. `allGames` is pulled from Room via
 * [LibraryGameInfoAdapter]; the [ESDEPreferencesManager]'s
 * `gamelistDecorationEnabled` toggle gates a post-scan
 * `LibraryMigration.reimportGamelists()` run so ES-DE's gamelist.xml
 * metadata still decorates the entries. The old
 * `parseAndStore` / `rebuildSystemIntoCache` cache pipeline is gone;
 * so is the `rom_index/<system>.json` write path.
 */
@HiltViewModel
class RomSearchViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val esdePreferencesManager: ESDEPreferencesManager,
    private val store: RomSearchStateHolder,
    private val scraperCoordinator: ScraperCoordinator,
    private val libraryAdapter: LibraryGameInfoAdapter,
    private val libraryRepository: LibraryRepository,
    private val libraryMigration: LibraryMigration,
    private val librarySync: LibraryStoreSync,
    private val favoritesGateway: FavoritesGateway,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    init {
        librarySync.start()
    }

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

    /**
     * Initial browse open. If Room already has games (the usual case
     * after the one-shot [LibraryMigration]) the adapter has already
     * published them to `store.allGames` and this is a no-op. First-run
     * users with an empty DB fall through to a full scan.
     */
    fun loadGames() {
        if (libraryRepository.state.value is LibraryState.Scanning) return
        if (store.allGames.value.isNotEmpty()) return
        libraryRepository.scanAll()
        decorateAfterScan { _, _ -> }
    }

    /**
     * Explicit refresh triggered from Settings. Phase 2's scanner
     * handles unreachable sources by leaving their existing rows
     * untouched (returns an empty list → `writeSystem` never runs for
     * that source), so an SD card that isn't mounted doesn't drop its
     * cached games.
     */
    fun refreshGames(onComplete: (games: Int, systems: Int) -> Unit = { _, _ -> }) {
        if (libraryRepository.state.value is LibraryState.Scanning) return
        libraryRepository.scanAll()
        decorateAfterScan(onComplete)
    }

    fun refreshSystem(systemName: String, onComplete: (games: Int) -> Unit = {}) {
        if (libraryRepository.state.value is LibraryState.Scanning) return
        libraryRepository.scanSystem(systemName)
        applicationScope.launch {
            awaitIdle()
            maybeReimportGamelists()
            // Same reason as decorateAfterScan: LibraryStoreSync drops
            // emissions while LibraryState is Scanning, so without this
            // forced pull the collector never republishes the post-scan
            // Room state and the UI keeps rendering the stale list.
            store.allGames.value = libraryAdapter.snapshot()
            val count = store.allGames.value.count { it.systemName == systemName }
            onComplete(count)
        }
    }

    /**
     * Pulled by the `ACTION_MEDIA_MOUNTED` receiver in
     * [jr.brian.home.esde.ui.FrontEndActivity]. The Phase 2 scanner
     * doesn't maintain a "degraded" flag — it never destroys data for
     * unreachable sources — so the retry collapses to "trigger a
     * scan if nothing else is in flight."
     */
    fun retryIfLastScanWasDegraded() {
        if (libraryRepository.state.value is LibraryState.Scanning) return
        libraryRepository.scanAll()
        decorateAfterScan { _, _ -> }
    }

    private fun decorateAfterScan(onComplete: (games: Int, systems: Int) -> Unit) {
        applicationScope.launch {
            awaitIdle()
            maybeReimportGamelists()
            // Force one pull from the adapter so `store.allGames` reflects
            // post-scan Room state. The init block's collector drops
            // emissions while LibraryState is Scanning (preventing mid-scan
            // blips during `replaceSystemGames`' delete+insert), so without
            // this read the last emission the collector saw gets stuck as
            // the published value — showing "No systems found" even though
            // the scan actually populated games.
            store.allGames.value = libraryAdapter.snapshot()
            val games = store.allGames.value.size
            val systems = store.allGames.value.map { it.systemName }.distinct().size
            onComplete(games, systems)
        }
    }

    private suspend fun awaitIdle() {
        libraryRepository.state.filter { it !is LibraryState.Scanning }.first()
    }

    private suspend fun maybeReimportGamelists() {
        if (!esdePreferencesManager.state.value.gamelistDecorationEnabled) return
        try {
            libraryMigration.reimportGamelists()
        } catch (e: Exception) {
            Log.w(TAG, "Gamelist decoration after scan failed", e)
        }
    }

    /**
     * Flip the favorite flag for [game] in Room. The
     * [LibraryGameInfoAdapter] collector picks up the new GameEntity
     * row and republishes `store.allGames`, so the UI's
     * `game.isFavorite` flips without an explicit refresh.
     */
    fun toggleFavorite(game: GameInfo) {
        applicationScope.launch {
            favoritesGateway.toggleFavorite(game)
        }
    }

    /**
     * Returns the override currently persisted for [game], or `null`
     * when the user has never written one. Called off the main thread
     * from the Edit Info sheet to prefill the form.
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
     * Persists an override for [game] and triggers a system refresh
     * so the UI sees the new metadata. Writing an [override] whose
     * fields are all null is equivalent to [clearUserOverride].
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
     * Copies a user-picked image into the override image directory
     * and returns its absolute path, suitable for storing on a
     * [UserMetadataOverride]. Returns `null` on IO failure.
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

    private companion object {
        private const val TAG = "RomSearchViewModel"
    }
}
