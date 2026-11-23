package jr.brian.jarnfe.scraper

import android.content.Context
import androidx.compose.runtime.Immutable
import jr.brian.frontend.scrape.ScraperConnectionResult
import jr.brian.frontend.scrape.ScraperMediaKind
import kotlinx.coroutines.flow.Flow

enum class JarnScraperSource { ScreenScraper, SteamGridDb }

/** What the UI may know about saved accounts: whether they exist, never their values. */
@Immutable
data class JarnScraperStatus(
    val screenScraperAvailable: Boolean,
    val screenScraperSaved: Boolean,
    val screenScraperEnabled: Boolean,
    val screenScraperKinds: Set<ScraperMediaKind>,
    val steamGridDbSaved: Boolean,
    val steamGridDbEnabled: Boolean,
    val steamGridDbKinds: Set<ScraperMediaKind>,
) {
    val isConfigured: Boolean
        get() = (screenScraperEnabled && screenScraperSaved && screenScraperAvailable) ||
            (steamGridDbEnabled && steamGridDbSaved)
}

/** Unsaved account edits. A null value keeps what is saved. */
class JarnScraperDraft(
    val userId: String? = null,
    val password: String? = null,
    val apiKey: String? = null,
) {
    override fun toString(): String = "JarnScraperDraft(redacted)"
}

data class JarnScraperPlan(
    val useScreenScraper: Boolean,
    val useSteamGridDb: Boolean,
    val screenScraperKinds: Set<ScraperMediaKind>,
    val steamGridDbKinds: Set<ScraperMediaKind>,
    val draft: JarnScraperDraft,
)

/** Implemented in `:app`, over the same credential storage Classic uses. */
interface JarnScraperSetup {
    val status: Flow<JarnScraperStatus>
    fun currentStatus(): JarnScraperStatus
    suspend fun test(source: JarnScraperSource, draft: JarnScraperDraft): ScraperConnectionResult
    fun save(plan: JarnScraperPlan)
    fun setEnabled(source: JarnScraperSource, enabled: Boolean)
}

interface JarnScraperSetupProvider {
    val jarnScraperSetup: JarnScraperSetup
}

internal fun Context.jarnScraperSetupOrNull(): JarnScraperSetup? =
    (applicationContext as? JarnScraperSetupProvider)?.jarnScraperSetup
