package jr.brian.jarnfe.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import jr.brian.core.model.ArtSource
import jr.brian.core.model.SystemArtSource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.frontend.library.art.ArtSourceStatus
import jr.brian.frontend.library.art.JarnLibraryData
import jr.brian.frontend.library.art.ScrapeMissingArtResult
import jr.brian.frontend.scrape.ScrapeState
import jr.brian.jarnfe.R
import jr.brian.jarnfe.scraper.JarnScraperFlow
import jr.brian.jarnfe.shell.JarnNotices

@StringRes
internal fun ArtSource.labelRes(): Int = when (this) {
    ArtSource.EsDe -> R.string.jarnfe_art_source_esde
    ArtSource.RetroHrai -> R.string.jarnfe_art_source_retrohrai
    ArtSource.Scraped -> R.string.jarnfe_art_source_scraped
}

/** Why the scrape row cannot run right now, or null when it can. */
@StringRes
internal fun scrapeBlockedReason(canScrape: Boolean, state: ScrapeState): Int? = when {
    state is ScrapeState.Running -> R.string.jarnfe_art_scrape_running
    !canScrape -> R.string.jarnfe_art_scrape_no_credentials
    else -> null
}

@Composable
internal fun JarnArtSection(data: JarnLibraryData) {
    val selected by data.artSource.collectAsState()
    val revision by data.artRevision.collectAsState()
    val scrape by data.scrapeState.collectAsState()
    val statuses by produceState<List<ArtSourceStatus>?>(null, revision) {
        value = data.artSourceStatus()
    }
    SettingsSectionTitle(R.string.jarnfe_art_title)
    ArtSource.entries.forEach { source ->
        ArtSourceRow(
            source = source,
            isSelected = source == selected,
            status = statuses?.firstOrNull { it.source == source },
            onSelect = { data.setArtSource(source) },
        )
    }
    ScraperSetupRow()
    ScrapeMissingArtRow(data, scrape)
    SystemArtSection(data)
}

@Composable
private fun SystemArtSection(data: JarnLibraryData) {
    val selected by data.systemArtSource.collectAsState()
    SettingsSectionTitle(R.string.jarnfe_system_art_title)
    SystemArtSource.entries.forEach { source ->
        JarnValueRow(
            label = stringResource(source.labelRes()),
            value = stringResource(if (source == selected) R.string.jarnfe_art_in_use else R.string.jarnfe_art_use),
            onClick = { data.setSystemArtSource(source) },
            modifier = Modifier.fillMaxWidth(),
            supporting = stringResource(source.hintRes()),
        )
    }
}

@StringRes
private fun SystemArtSource.labelRes(): Int = when (this) {
    SystemArtSource.RetroHrai -> R.string.jarnfe_art_source_retrohrai
    SystemArtSource.Bundled -> R.string.jarnfe_system_art_bundled
}

@StringRes
private fun SystemArtSource.hintRes(): Int = when (this) {
    SystemArtSource.RetroHrai -> R.string.jarnfe_system_art_retrohrai_hint
    SystemArtSource.Bundled -> R.string.jarnfe_system_art_bundled_hint
}

@Composable
private fun ArtSourceRow(
    source: ArtSource,
    isSelected: Boolean,
    status: ArtSourceStatus?,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    JarnValueRow(
        label = stringResource(source.labelRes()),
        value = stringResource(if (isSelected) R.string.jarnfe_art_in_use else R.string.jarnfe_art_use),
        onClick = onSelect,
        modifier = modifier.fillMaxWidth(),
        supporting = artAvailabilityText(status),
    )
}

@Composable
private fun artAvailabilityText(status: ArtSourceStatus?): String = when {
    status == null -> stringResource(R.string.jarnfe_art_checking)
    status.isAvailable -> pluralStringResource(
        R.plurals.jarnfe_art_file_count,
        status.fileCount,
        status.fileCount,
    )
    else -> stringResource(R.string.jarnfe_art_none_found)
}

@Composable
private fun ScraperSetupRow() {
    val focus = remember { FocusRequester() }
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_scraper_title),
        onClick = { JarnScraperFlow.open(focus) },
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
        supporting = stringResource(R.string.jarnfe_scraper_row_hint),
    )
}

/** A blocked scrape opens setup or progress, so this row always has something to do. */
@Composable
private fun ScrapeMissingArtRow(data: JarnLibraryData, scrape: ScrapeState) {
    val focus = remember { FocusRequester() }
    val canScrape = remember(scrape, JarnScraperFlow.isOpen) { data.canScrape() }
    val blocked = scrapeBlockedReason(canScrape, scrape)
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_art_scrape_missing),
        onClick = { if (blocked == null) startScrape(data) else JarnScraperFlow.open(focus) },
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
        supporting = stringResource(blocked ?: scrapeIdleText(scrape)),
    )
}

@StringRes
private fun scrapeIdleText(state: ScrapeState): Int = when (state) {
    is ScrapeState.Finished -> R.string.jarnfe_art_scrape_finished
    ScrapeState.Cancelled -> R.string.jarnfe_art_scrape_cancelled
    else -> R.string.jarnfe_art_scrape_hint
}

private fun startScrape(data: JarnLibraryData) {
    JarnNotices.show(
        when (data.scrapeMissingArt()) {
            ScrapeMissingArtResult.Started -> R.string.jarnfe_art_scrape_started
            ScrapeMissingArtResult.NotConfigured -> R.string.jarnfe_art_scrape_no_credentials
        },
    )
}
