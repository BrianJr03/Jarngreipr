package jr.brian.home.data

import jr.brian.core.model.ApplicationScope
import jr.brian.frontend.library.LibraryRepository
import jr.brian.frontend.library.LibraryState
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.data.SetupPreferences
import jr.brian.home.esde.scraper.ScraperCoordinator
import jr.brian.home.esde.util.mediaRoots
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mirrors the Room-backed library into legacy's [RomSearchStateHolder]:
 * the [LibraryGameInfoAdapter] game list, [LibraryState] scan progress,
 * and the media roots [LibraryRepository]'s scanner resolves art against.
 *
 * ### Lifecycle
 *
 * [start] is idempotent and called from every `RomSearchViewModel` init.
 * These collectors used to be launched from the ViewModel itself on the
 * application scope, so every new ViewModel instance (there are ~10
 * `hiltViewModel()` call sites) leaked another permanent set. Each leaked
 * set re-queried all of `games` + `media` on every write, and during a
 * scrape that piled up until the heap OOM'd. Owning them here keeps exactly
 * one set alive. They stay on the application scope so a game launch
 * finishing the frontend activity doesn't cancel them mid-scan.
 */
@Singleton
class LibraryStoreSync @Inject constructor(
    private val store: RomSearchStateHolder,
    private val libraryAdapter: LibraryGameInfoAdapter,
    private val libraryRepository: LibraryRepository,
    private val esdePreferencesManager: ESDEPreferencesManager,
    private val setupPreferences: SetupPreferences,
    private val scraperCoordinator: ScraperCoordinator,
    @param:ApplicationScope private val scope: CoroutineScope,
) {

    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return

        // The Scanning gate keeps intermediate emissions from flashing an
        // empty list between a scan starting and the first system completing.
        scope.launch {
            libraryAdapter.allGameInfos.collect { games ->
                if (libraryRepository.state.value !is LibraryState.Scanning) {
                    store.allGames.value = games
                }
            }
        }

        // Mirror LibraryState to the legacy loading / progress flags
        // the UI already renders. Scanning → true + counts, else false.
        scope.launch {
            libraryRepository.state.collect { state ->
                when (state) {
                    is LibraryState.Scanning -> {
                        store.isLoading.value = true
                        store.systemScanLoaded.value = state.systemsDone
                        store.systemScanTotal.value = state.systemsTotal
                    }
                    is LibraryState.Idle, is LibraryState.Error -> {
                        store.isLoading.value = false
                    }
                }
            }
        }

        // Push media roots into the repository whenever the user's
        // ES-DE / scraped-media prefs change. Without this the Phase 2
        // scanner's MediaIndex returns Resolved.EMPTY for every game,
        // which writeSystem interprets as "delete this game's media
        // row" — wiping scraped art the migration importer had just
        // written.
        scope.launch {
            esdePreferencesManager.state.collect {
                libraryRepository.setMediaRoots(currentMediaRoots())
            }
        }
    }

    private fun currentMediaRoots(): List<File> {
        val state = esdePreferencesManager.state.value
        val primary = state.customMediaPath ?: setupPreferences.mediaPath
        val secondary = if (state.secondaryMediaEnabled)
            SetupPreferences.RETRO_HRAI_PATH else null
        val tertiary = if (state.jarngreiprMediaEnabled)
            scraperCoordinator.scrapedRoot() else null
        return mediaRoots(primary, secondary, tertiary)
            .filter { it.isNotBlank() }
            .map { File(it) }
    }
}
