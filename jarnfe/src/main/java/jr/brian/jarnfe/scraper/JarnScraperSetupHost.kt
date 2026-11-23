package jr.brian.jarnfe.scraper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnDialog
import jr.brian.core.ui.components.JarnSheet
import jr.brian.core.ui.focus.JarnRevealScrollTop
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.library.art.JarnLibraryData
import jr.brian.frontend.library.art.ScrapeMissingArtResult
import jr.brian.frontend.scrape.ScrapeState
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.shell.JarnNotices

/** Opens the scraper sheet from any row and returns focus to that row when it closes. */
internal object JarnScraperFlow {
    var isOpen by mutableStateOf(false)
        private set

    internal var returnFocus: FocusRequester? = null
        private set

    fun open(returnTo: FocusRequester) {
        returnFocus = returnTo
        isOpen = true
    }

    fun close() {
        isOpen = false
    }
}

@Composable
internal fun JarnScraperSetupHost(data: JarnLibraryData) {
    ScraperFocusReturn()
    DisposableEffect(Unit) { onDispose { JarnScraperFlow.close() } }
    if (!JarnScraperFlow.isOpen) return
    val context = LocalContext.current
    val setup = remember(context) { context.jarnScraperSetupOrNull() }
    if (setup == null) {
        LaunchedEffect(Unit) { JarnScraperFlow.close() }
    } else {
        ScraperSheet(setup, data)
    }
}

@Composable
private fun ScraperFocusReturn() {
    val isOpen = JarnScraperFlow.isOpen
    var wasOpen by remember { mutableStateOf(false) }
    LaunchedEffect(isOpen) {
        if (wasOpen && !isOpen) {
            withFrameNanos { }
            JarnScraperFlow.returnFocus?.requestFocus()
        }
        wasOpen = isOpen
    }
}

@Composable
private fun ScraperSheet(setup: JarnScraperSetup, data: JarnLibraryData) {
    val status by setup.status.collectAsState(setup.currentStatus())
    val scrape by data.scrapeState.collectAsState()
    var wizard by remember {
        val current = setup.currentStatus()
        val showStatus = current.isConfigured || data.scrapeState.value is ScrapeState.Running
        mutableStateOf(if (showStatus) null else ScraperWizard.from(current))
    }
    var editing by remember { mutableStateOf<ScraperField?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }

    fun leaveWizard() {
        wizard = null
        if (!setup.currentStatus().isConfigured) JarnScraperFlow.close()
    }

    fun onBack() {
        val current = wizard
        when {
            confirmDiscard -> confirmDiscard = false
            editing != null -> editing = null
            current == null -> JarnScraperFlow.close()
            current.step != ScraperStep.Source -> wizard = current.previous()
            current.isDirty -> confirmDiscard = true
            else -> leaveWizard()
        }
    }

    fun finish(scrapeNow: Boolean) {
        wizard?.let { setup.save(it.toPlan()) }
        wizard = null
        if (scrapeNow) {
            startMissingArtScrape(data)
        } else {
            JarnScraperFlow.close()
        }
    }

    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
            on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = ::onBack)
        },
    ) {
        JarnSheet(
            visible = true,
            onDismiss = ::onBack,
            title = stringResource(R.string.jarnfe_scraper_title),
        ) {
            val scroll = rememberScrollState()
            JarnRevealScrollTop(scroll) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(scroll)
                        .padding(JarnTheme.space.focusEdge),
                    verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
                ) {
                    val active = wizard
                    val field = editing
                    when {
                        active != null && field != null -> ScraperFieldKeyboard(
                            field = field,
                            onDone = { value ->
                                wizard = active.edit(field, value)
                                editing = null
                            },
                            onCancel = { editing = null },
                        )
                        active != null -> ScraperWizardContent(
                            wizard = active,
                            setup = setup,
                            refocus = !confirmDiscard,
                            onChange = { wizard = it },
                            onEdit = { editing = it },
                            onFinish = ::finish,
                        )
                        else -> ScraperStatusPage(
                            status = status,
                            scrape = scrape,
                            setup = setup,
                            onScrape = { startMissingArtScrape(data) },
                            onCancelScrape = data::cancelScrape,
                            onEditSetup = { wizard = ScraperWizard.from(setup.currentStatus()) },
                            onDone = JarnScraperFlow::close,
                        )
                    }
                }
            }
        }
        if (confirmDiscard) {
            ScraperDiscardDialog(
                onKeepEditing = { confirmDiscard = false },
                onDiscard = {
                    confirmDiscard = false
                    leaveWizard()
                },
            )
        }
    }
}

@Composable
private fun ScraperDiscardDialog(onKeepEditing: () -> Unit, onDiscard: () -> Unit) {
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
            on(JarnAction.Back, R.string.jarnfe_scraper_keep_editing, onPress = onKeepEditing)
        },
    ) {
        JarnDialog(
            visible = true,
            title = stringResource(R.string.jarnfe_scraper_discard_title),
            message = stringResource(R.string.jarnfe_scraper_discard_message),
            confirmLabel = stringResource(R.string.jarnfe_scraper_discard),
            dismissLabel = stringResource(R.string.jarnfe_scraper_keep_editing),
            onConfirm = onDiscard,
            onDismiss = onKeepEditing,
            destructive = true,
        )
    }
}

private fun startMissingArtScrape(data: JarnLibraryData) {
    JarnNotices.show(
        when (data.scrapeMissingArt()) {
            ScrapeMissingArtResult.Started -> R.string.jarnfe_art_scrape_started
            ScrapeMissingArtResult.NotConfigured -> R.string.jarnfe_art_scrape_no_credentials
        },
    )
}
