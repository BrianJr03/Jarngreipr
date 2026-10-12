package jr.brian.home.esde.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.model.ScraperMediaKind
import jr.brian.home.esde.scraper.ScraperCoordinator
import jr.brian.home.esde.scraper.ScreenScraperClient
import jr.brian.home.esde.scraper.SteamGridDbClient
import jr.brian.home.esde.scraper.model.ScrapeRequest
import jr.brian.home.esde.scraper.model.ScrapeState
import jr.brian.home.esde.scraper.model.ScraperTestResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI-facing façade over the app-scoped [ScraperCoordinator]. Exposes:
 *
 *  - scraping state (Idle/Running/Finished/Cancelled) — driven straight
 *    from the singleton so entering the screen after backgrounding
 *    surfaces the in-flight scrape,
 *  - one-shot test-credentials calls (StateFlow so the result stays on
 *    screen until dismissed),
 *  - `startScrape(...)` / `cancel()` / `dismissFinished()`.
 */
@HiltViewModel
class ScraperViewModel @Inject constructor(
    private val prefs: ESDEPreferencesManager,
    private val store: RomSearchStateHolder,
    private val coordinator: ScraperCoordinator,
    private val screenScraper: ScreenScraperClient,
    private val steamGridDb: SteamGridDbClient
) : ViewModel() {

    val state: StateFlow<ScrapeState> = coordinator.state

    private val _screenScraperTest = MutableStateFlow<ScraperTestResult?>(null)
    val screenScraperTest: StateFlow<ScraperTestResult?> = _screenScraperTest.asStateFlow()

    private val _steamGridDbTest = MutableStateFlow<ScraperTestResult?>(null)
    val steamGridDbTest: StateFlow<ScraperTestResult?> = _steamGridDbTest.asStateFlow()

    private val _testInFlight = MutableStateFlow(false)
    val testInFlight: StateFlow<Boolean> = _testInFlight.asStateFlow()

    fun testScreenScraper() {
        if (_testInFlight.value) return
        val s = prefs.state.value
        val creds = ScreenScraperClient.Credentials(
            devId = s.screenScraperDevId,
            devPassword = s.screenScraperDevPassword,
            userId = s.screenScraperUserId,
            userPassword = s.screenScraperUserPassword
        )
        _testInFlight.value = true
        viewModelScope.launch {
            _screenScraperTest.value = screenScraper.testCredentials(creds)
            _testInFlight.value = false
        }
    }

    fun testSteamGridDb() {
        if (_testInFlight.value) return
        val key = prefs.state.value.steamGridDbApiKey
        _testInFlight.value = true
        viewModelScope.launch {
            _steamGridDbTest.value = steamGridDb.testApiKey(key)
            _testInFlight.value = false
        }
    }

    fun startScrape(onRefresh: () -> Unit) {
        val hidden = prefs.state.value.hiddenSystems
        val eligible = store.allGames.value.filter { it.systemName !in hidden }
        startScrapeFor(eligible, onRefresh)
    }

    fun scrapeSystem(systemName: String, onRefresh: () -> Unit) {
        val eligible = store.allGames.value.filter {
            it.systemName.equals(systemName, ignoreCase = true)
        }
        startScrapeFor(eligible, onRefresh)
    }

    fun scrapeGame(game: GameInfo, onRefresh: () -> Unit) {
        startScrapeFor(listOf(game), onRefresh)
    }

    private fun startScrapeFor(games: List<GameInfo>, onRefresh: () -> Unit) {
        if (games.isEmpty()) return
        val s = prefs.state.value
        val ssKinds = s.screenScraperMediaTypes.mapNotNull(ScraperMediaKind::fromName).toSet()
        val sgKinds = s.steamGridDbMediaTypes.mapNotNull(ScraperMediaKind::fromName).toSet()
        val request = ScrapeRequest(
            games = games,
            mediaTypesScreenScraper = ssKinds,
            mediaTypesSteamGridDb = sgKinds,
            screenScraperEnabled = s.screenScraperEnabled,
            steamGridDbEnabled = s.steamGridDbEnabled
        )
        val creds = if (s.screenScraperEnabled) {
            ScreenScraperClient.Credentials(
                devId = s.screenScraperDevId,
                devPassword = s.screenScraperDevPassword,
                userId = s.screenScraperUserId,
                userPassword = s.screenScraperUserPassword
            )
        } else null
        val apiKey = s.steamGridDbApiKey.takeIf { s.steamGridDbEnabled }
        coordinator.start(
            request = request,
            screenScraperCredentials = creds,
            steamGridDbApiKey = apiKey,
            onFinished = onRefresh
        )
    }

    fun hasEnabledScraper(): Boolean {
        val s = prefs.state.value
        return s.screenScraperEnabled || s.steamGridDbEnabled
    }

    fun cancel() {
        viewModelScope.launch { coordinator.cancel() }
    }

    fun dismissResult() {
        coordinator.clearFinished()
        _screenScraperTest.value = null
        _steamGridDbTest.value = null
    }

    @Suppress("unused") // hooks for future explicit game filtering
    fun eligibleGames(): List<GameInfo> {
        val hidden = prefs.state.value.hiddenSystems
        return store.allGames.value.filter { it.systemName !in hidden }
    }
}
