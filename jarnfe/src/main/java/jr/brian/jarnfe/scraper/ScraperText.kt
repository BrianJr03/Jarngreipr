package jr.brian.jarnfe.scraper

import androidx.annotation.StringRes
import jr.brian.frontend.scrape.ScraperConnectionResult
import jr.brian.frontend.scrape.ScraperMediaKind
import jr.brian.jarnfe.R

@StringRes
internal fun JarnScraperSource.labelRes(): Int = when (this) {
    JarnScraperSource.ScreenScraper -> R.string.jarnfe_scraper_source_screenscraper
    JarnScraperSource.SteamGridDb -> R.string.jarnfe_scraper_source_steamgriddb
}

@StringRes
internal fun ScraperField.labelRes(): Int = when (this) {
    ScraperField.UserId -> R.string.jarnfe_scraper_field_user
    ScraperField.Password -> R.string.jarnfe_scraper_field_password
    ScraperField.ApiKey -> R.string.jarnfe_scraper_field_api_key
}

@StringRes
internal fun ScraperStep.titleRes(): Int = when (this) {
    ScraperStep.Source -> R.string.jarnfe_scraper_step_source
    ScraperStep.Account -> R.string.jarnfe_scraper_step_account
    ScraperStep.Test -> R.string.jarnfe_scraper_step_test
    ScraperStep.Art -> R.string.jarnfe_scraper_step_art
    ScraperStep.Finish -> R.string.jarnfe_scraper_step_finish
}

@StringRes
internal fun ScraperStep.messageRes(): Int = when (this) {
    ScraperStep.Source -> R.string.jarnfe_scraper_step_source_message
    ScraperStep.Account -> R.string.jarnfe_scraper_step_account_message
    ScraperStep.Test -> R.string.jarnfe_scraper_step_test_message
    ScraperStep.Art -> R.string.jarnfe_scraper_step_art_message
    ScraperStep.Finish -> R.string.jarnfe_scraper_step_finish_message
}

@StringRes
internal fun ScraperMediaKind.labelRes(): Int = when (this) {
    ScraperMediaKind.Covers -> R.string.jarnfe_scraper_kind_covers
    ScraperMediaKind.Screenshots -> R.string.jarnfe_scraper_kind_screenshots
    ScraperMediaKind.Marquees -> R.string.jarnfe_scraper_kind_marquees
    ScraperMediaKind.Fanart -> R.string.jarnfe_scraper_kind_fanart
    ScraperMediaKind.TitleScreens -> R.string.jarnfe_scraper_kind_titles
    ScraperMediaKind.MixImages -> R.string.jarnfe_scraper_kind_mix
    ScraperMediaKind.Videos -> R.string.jarnfe_scraper_kind_videos
}

/** Bad credentials name the exact field to fix for that source. */
@StringRes
internal fun ScraperConnectionResult.messageRes(source: JarnScraperSource): Int = when (this) {
    ScraperConnectionResult.Ok -> R.string.jarnfe_scraper_result_ok
    ScraperConnectionResult.MissingAccount -> R.string.jarnfe_scraper_result_missing
    ScraperConnectionResult.BadCredentials -> when (source) {
        JarnScraperSource.ScreenScraper -> R.string.jarnfe_scraper_result_bad_user
        JarnScraperSource.SteamGridDb -> R.string.jarnfe_scraper_result_bad_key
    }
    ScraperConnectionResult.RateLimited -> R.string.jarnfe_scraper_result_limited
    ScraperConnectionResult.Offline -> R.string.jarnfe_scraper_result_offline
    ScraperConnectionResult.ServiceProblem -> R.string.jarnfe_scraper_result_service
    ScraperConnectionResult.NotAvailable -> R.string.jarnfe_scraper_unavailable
}
