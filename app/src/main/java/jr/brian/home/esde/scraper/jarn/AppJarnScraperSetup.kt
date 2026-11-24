package jr.brian.home.esde.scraper.jarn

import jr.brian.frontend.scrape.ScraperConnectionResult
import jr.brian.frontend.scrape.ScraperConnectionTester
import jr.brian.frontend.scrape.ScraperMediaKind
import jr.brian.frontend.scrape.ScreenScraperConfig
import jr.brian.frontend.scrape.UserScraperCredentials
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.setScreenScraperEnabled
import jr.brian.home.esde.data.setScreenScraperMediaTypes
import jr.brian.home.esde.data.setScreenScraperUserId
import jr.brian.home.esde.data.setScreenScraperUserPassword
import jr.brian.home.esde.data.setSteamGridDbApiKey
import jr.brian.home.esde.data.setSteamGridDbEnabled
import jr.brian.home.esde.data.setSteamGridDbMediaTypes
import jr.brian.home.esde.model.ESDEPrefsState
import jr.brian.jarnfe.scraper.JarnScraperDraft
import jr.brian.jarnfe.scraper.JarnScraperPlan
import jr.brian.jarnfe.scraper.JarnScraperSetup
import jr.brian.jarnfe.scraper.JarnScraperSource
import jr.brian.jarnfe.scraper.JarnScraperStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/** Saves through Classic's setters, which also keep the scrape engine's store in sync. */
@Singleton
class AppJarnScraperSetup @Inject constructor(
    private val prefs: ESDEPreferencesManager,
    private val credentials: UserScraperCredentials,
    private val config: ScreenScraperConfig,
    private val tester: ScraperConnectionTester,
) : JarnScraperSetup {

    override val status: Flow<JarnScraperStatus> =
        combine(prefs.state, credentials.state) { state, saved -> state.toStatus(saved) }
            .distinctUntilChanged()

    override fun currentStatus(): JarnScraperStatus =
        prefs.state.value.toStatus(credentials.state.value)

    override suspend fun test(
        source: JarnScraperSource,
        draft: JarnScraperDraft,
    ): ScraperConnectionResult = when (source) {
        JarnScraperSource.ScreenScraper -> tester.testScreenScraper(draft.userId, draft.password)
        JarnScraperSource.SteamGridDb -> tester.testSteamGridDb(draft.apiKey)
    }

    override fun save(plan: JarnScraperPlan) {
        plan.draft.userId?.let(prefs::setScreenScraperUserId)
        plan.draft.password?.let(prefs::setScreenScraperUserPassword)
        plan.draft.apiKey?.let(prefs::setSteamGridDbApiKey)
        if (plan.useScreenScraper) {
            prefs.setScreenScraperMediaTypes(plan.screenScraperKinds.names())
        }
        if (plan.useSteamGridDb) {
            prefs.setSteamGridDbMediaTypes(plan.steamGridDbKinds.names())
        }
        prefs.setScreenScraperEnabled(plan.useScreenScraper)
        prefs.setSteamGridDbEnabled(plan.useSteamGridDb)
    }

    override fun setEnabled(source: JarnScraperSource, enabled: Boolean) = when (source) {
        JarnScraperSource.ScreenScraper -> prefs.setScreenScraperEnabled(enabled)
        JarnScraperSource.SteamGridDb -> prefs.setSteamGridDbEnabled(enabled)
    }

    private fun ESDEPrefsState.toStatus(saved: UserScraperCredentials.Snapshot) = JarnScraperStatus(
        screenScraperAvailable = config.hasDevCredentials,
        screenScraperSaved = saved.hasScreenScraperUser,
        screenScraperEnabled = screenScraperEnabled,
        screenScraperKinds = screenScraperMediaTypes.toKinds(),
        steamGridDbSaved = saved.hasSteamGridDbKey,
        steamGridDbEnabled = steamGridDbEnabled,
        steamGridDbKinds = steamGridDbMediaTypes.toKinds(),
    )

    private fun Set<String>.toKinds(): Set<ScraperMediaKind> =
        ScraperMediaKind.entries.filter { it.name in this }.toSet()

    private fun Set<ScraperMediaKind>.names(): Set<String> = mapTo(LinkedHashSet()) { it.name }
}
