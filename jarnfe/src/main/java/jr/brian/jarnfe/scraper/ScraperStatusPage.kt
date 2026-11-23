package jr.brian.jarnfe.scraper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.scrape.ScrapeState
import jr.brian.frontend.scrape.ScraperConnectionResult
import jr.brian.frontend.scrape.ScraperMediaKind
import jr.brian.jarnfe.R

private val ProgressBarHeight = 6.dp

internal fun JarnScraperStatus.isSaved(source: JarnScraperSource): Boolean = when (source) {
    JarnScraperSource.ScreenScraper -> screenScraperSaved && screenScraperAvailable
    JarnScraperSource.SteamGridDb -> steamGridDbSaved
}

internal fun JarnScraperStatus.isEnabled(source: JarnScraperSource): Boolean = when (source) {
    JarnScraperSource.ScreenScraper -> screenScraperEnabled
    JarnScraperSource.SteamGridDb -> steamGridDbEnabled
}

internal fun JarnScraperStatus.kinds(source: JarnScraperSource): Set<ScraperMediaKind> = when (source) {
    JarnScraperSource.ScreenScraper -> screenScraperKinds
    JarnScraperSource.SteamGridDb -> steamGridDbKinds
}

@Composable
internal fun ScraperStatusPage(
    status: JarnScraperStatus,
    scrape: ScrapeState,
    setup: JarnScraperSetup,
    onScrape: () -> Unit,
    onCancelScrape: () -> Unit,
    onEditSetup: () -> Unit,
    onDone: () -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocus() }
    var results by remember { mutableStateOf<Map<JarnScraperSource, ScraperConnectionResult>>(emptyMap()) }
    var testing by remember { mutableStateOf(false) }
    val enabled = JarnScraperSource.entries.filter { status.isEnabled(it) && status.isSaved(it) }
    if (testing) {
        LaunchedEffect(Unit) {
            results = enabled.associateWith { setup.test(it, JarnScraperDraft()) }
            testing = false
        }
    }
    ScrapeProgress(scrape, onCancelScrape)
    JarnScraperSource.entries.forEachIndexed { index, source ->
        SourceStatusRow(
            source = source,
            status = status,
            onToggle = { setup.setEnabled(source, it) },
            modifier = if (index == 0) Modifier.fillMaxWidth().focusRequester(first) else Modifier.fillMaxWidth(),
        )
        if (source in enabled && (testing || source in results)) {
            ScraperResultLine(source, results[source], testing)
        }
    }
    JarnButton(
        label = stringResource(
            if (scrape is ScrapeState.Finished || scrape == ScrapeState.Cancelled) {
                R.string.jarnfe_scraper_scrape_again
            } else {
                R.string.jarnfe_scraper_scrape_now
            },
        ),
        onClick = onScrape,
        modifier = Modifier.fillMaxWidth(),
        enabled = status.isConfigured && scrape !is ScrapeState.Running,
        focusableWhenDisabled = true,
    )
    SecondaryAction(R.string.jarnfe_scraper_test, enabled.isNotEmpty() && !testing) {
        results = emptyMap()
        testing = true
    }
    SecondaryAction(R.string.jarnfe_scraper_edit, enabled = true, onEditSetup)
    SecondaryAction(R.string.jarnfe_scraper_done, enabled = true, onDone)
}

@Composable
private fun SecondaryAction(labelRes: Int, enabled: Boolean, onClick: () -> Unit) {
    JarnButton(
        label = stringResource(labelRes),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        variant = JarnButtonVariant.Secondary,
        enabled = enabled,
        focusableWhenDisabled = true,
    )
}

@Composable
private fun SourceStatusRow(
    source: JarnScraperSource,
    status: JarnScraperStatus,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier,
) {
    val saved = status.isSaved(source)
    val kinds = status.kinds(source).map { stringResource(it.labelRes()) }
    JarnToggleRow(
        label = stringResource(source.labelRes()),
        checked = status.isEnabled(source) && saved,
        onCheckedChange = onToggle,
        modifier = modifier,
        supporting = if (saved) {
            kinds.joinToString(stringResource(R.string.jarnfe_scraper_kind_separator))
        } else {
            stringResource(R.string.jarnfe_scraper_no_account)
        },
        enabled = saved,
        focusableWhenDisabled = true,
    )
}

@Composable
private fun ScrapeProgress(scrape: ScrapeState, onCancel: () -> Unit) {
    when (scrape) {
        ScrapeState.Idle -> Unit
        is ScrapeState.Running -> RunningProgress(scrape, onCancel)
        is ScrapeState.Finished -> ProgressMessage(
            if (scrape.processed == 0) {
                stringResource(R.string.jarnfe_scraper_nothing_missing)
            } else {
                stringResource(R.string.jarnfe_scraper_summary, scrape.processed, scrape.downloads, scrape.errors)
            },
        )
        ScrapeState.Cancelled -> ProgressMessage(stringResource(R.string.jarnfe_scraper_stopped))
    }
}

@Composable
private fun ProgressMessage(text: String) {
    JarnText(text, JarnTheme.type.body, JarnTheme.colors.textHigh)
}

@Composable
private fun RunningProgress(state: ScrapeState.Running, onCancel: () -> Unit) {
    ProgressMessage(stringResource(R.string.jarnfe_scraper_progress, state.done, state.total, state.downloads))
    state.currentTitle?.let {
        JarnText(it, JarnTheme.type.caption, JarnTheme.colors.textMid, maxLines = 1)
    }
    ProgressBar(scrapeFraction(state))
    JarnButton(
        label = stringResource(R.string.jarnfe_scraper_cancel),
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth(),
        variant = JarnButtonVariant.Danger,
    )
}

internal fun scrapeFraction(state: ScrapeState.Running): Float =
    if (state.total <= 0) 0f else (state.done.toFloat() / state.total).coerceIn(0f, 1f)

@Composable
private fun ProgressBar(fraction: Float) {
    val shape = JarnTheme.radius.smShape
    Box(
        Modifier
            .fillMaxWidth()
            .height(ProgressBarHeight)
            .background(JarnTheme.colors.raised, shape),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(JarnTheme.colors.accent, shape),
        )
    }
}
