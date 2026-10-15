package jr.brian.frontend.scrape

import jr.brian.frontend.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Static ScreenScraper developer credentials, read from BuildConfig
 * fields injected at build time from the SCREENSCRAPER_DEV_ID /
 * SCREENSCRAPER_DEV_PASSWORD CI secrets.
 *
 * [hasDevCredentials] is the only guard scraping callers need to
 * check — a build without the secrets compiles and runs, but the
 * scrape surfaces return a "scraper not configured" status rather
 * than hitting the API with empty strings and failing on auth.
 *
 * Deliberately NOT reading from ESDEPreferencesManager — users
 * shouldn't be asked for dev credentials; those ship with the app.
 * Phase 0 §5 already stripped scraper secrets from config exports.
 */
@Singleton
class ScreenScraperConfig @Inject constructor() {

    val devId: String get() = BuildConfig.SCREENSCRAPER_DEV_ID
    val devPassword: String get() = BuildConfig.SCREENSCRAPER_DEV_PASSWORD

    val hasDevCredentials: Boolean
        get() = devId.isNotBlank() && devPassword.isNotBlank()
}
