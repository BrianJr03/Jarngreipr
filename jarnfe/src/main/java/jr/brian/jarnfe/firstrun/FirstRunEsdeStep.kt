package jr.brian.jarnfe.firstrun

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.look.JarnLookStore
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.look.jarnLookStoreOrNull
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.settings.JarnEsdeImport
import jr.brian.jarnfe.settings.JarnEsdeImportState
import jr.brian.jarnfe.settings.JarnEsdeImportSummaryCard
import jr.brian.jarnfe.settings.JarnEsdeImporter
import jr.brian.jarnfe.settings.RefocusAfterFileAccess
import jr.brian.jarnfe.settings.WatchJarnEsdeImport
import jr.brian.jarnfe.settings.esdeFoundText
import jr.brian.jarnfe.settings.esdeImportProgressText
import jr.brian.jarnfe.settings.rememberJarnEsdeImporter
import jr.brian.jarnfe.shell.JarnNotices

/** The tour's actions, with the ES-DE step's primary button and the skip notice wired in. */
@Composable
internal fun rememberJarnFirstRunActions(controller: JarnFirstRunController): JarnFirstRunActions {
    val context = LocalContext.current
    val importer = rememberJarnEsdeImporter()
    val store = remember(context) { context.jarnLookStoreOrNull() }
    return remember(controller, importer, store) {
        JarnFirstRunActions(
            controller = controller,
            session = JarnSession,
            onSkipped = { importer?.let { noticeEsdeAfterSkip(it, store) } },
            esdePrimary = { importer?.let(::esdePrimary) ?: false },
        )
    }
}

/** Shows the ES-DE step only when ES-DE is there and the library has games. Runs on the Stage. */
@Composable
internal fun FirstRunEsdeOfferEffect(controller: JarnFirstRunController, twoScreens: Boolean) {
    val importer = rememberJarnEsdeImporter() ?: return
    val hasGames = JarnLibrary.state.systems.items.any { it.gameCount > 0 }
    LaunchedEffect(importer, hasGames, twoScreens) {
        val detection = importer.refreshDetection()
        controller.offerEsde(hasGames && detection.worthOffering, twoScreens)
    }
}

/** True when the press was the ES-DE step's own (import, retry); false lets the tour move on. */
private fun esdePrimary(importer: JarnEsdeImporter): Boolean {
    if (importer.state is JarnEsdeImportState.Done) return false
    val screen = if (JarnSession.state.settingsOnDeck) JarnScreen.Deck else JarnScreen.Stage
    importer.request(screen, returnFocus = FirstRunEsdeFocus.primary)
    return true
}

/** The ES-DE step's primary button while it's on screen, so the consent dialog can return focus to it. */
internal object FirstRunEsdeFocus {
    var primary: FocusRequester? = null
}

/** One notice, once ever, for someone who skipped the tour with ES-DE never imported. */
private fun noticeEsdeAfterSkip(importer: JarnEsdeImporter, store: JarnLookStore?) {
    if (store == null || store.esdeImportNoticeShown) return
    val found = importer.detection?.found == true
    val imported = importer.lastImport.value != null || importer.state is JarnEsdeImportState.Done
    if (!found || imported) return
    JarnNotices.show(R.string.jarnfe_esde_import_skip_notice)
    store.markEsdeImportNoticeShown()
}

@StringRes
internal fun esdePrimaryLabelRes(state: JarnEsdeImportState?): Int = when (state) {
    is JarnEsdeImportState.Done -> R.string.jarnfe_first_run_next
    JarnEsdeImportState.NeedsAccess, JarnEsdeImportState.Failed -> R.string.jarnfe_esde_import_retry
    else -> R.string.jarnfe_esde_import_bring
}

@Composable
internal fun FirstRunEsdeStep() {
    val importer = rememberJarnEsdeImporter()
    FirstRunEsdeTitle()
    if (importer == null) return
    WatchJarnEsdeImport(importer)
    when (val state = importer.state) {
        is JarnEsdeImportState.Done -> JarnEsdeImportSummaryCard(state.summary)
        JarnEsdeImportState.NeedsAccess -> FirstRunEsdeLine(R.string.jarnfe_esde_import_no_access)
        JarnEsdeImportState.Failed -> FirstRunEsdeLine(R.string.jarnfe_esde_import_failed)
        else -> FirstRunEsdeBody(esdeFoundText(importer.detection))
    }
}

@Composable
private fun FirstRunEsdeTitle() {
    JarnText(
        text = stringResource(R.string.jarnfe_esde_import_title),
        style = JarnTheme.type.heading,
        color = JarnTheme.colors.textHigh,
        maxLines = 2,
    )
}

@Composable
private fun FirstRunEsdeBody(text: String) {
    JarnText(text, JarnTheme.type.body, JarnTheme.colors.textMid)
}

@Composable
private fun FirstRunEsdeLine(@StringRes textRes: Int) {
    FirstRunEsdeBody(stringResource(textRes))
}

/** The ES-DE step's primary: Bring them over, a loading count while it runs, then Next. */
@Composable
internal fun FirstRunEsdePrimaryButton(
    actions: JarnFirstRunActions,
    focus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val importer = JarnEsdeImport.importer
    val state = importer?.state
    val running = state is JarnEsdeImportState.Running
    importer?.let { RefocusAfterFileAccess(it, focus) }
    DisposableEffect(focus) {
        FirstRunEsdeFocus.primary = focus
        onDispose { if (FirstRunEsdeFocus.primary === focus) FirstRunEsdeFocus.primary = null }
    }
    JarnButton(
        label = state?.let { esdeImportProgressText(it) } ?: stringResource(esdePrimaryLabelRes(state)),
        onClick = actions::primary,
        modifier = modifier.focusRequester(focus),
        loading = running,
    )
}

/** "Not now", under the buttons, until the import is done. Moving on never cancels a running import. */
@Composable
internal fun FirstRunEsdeNotNow(actions: JarnFirstRunActions) {
    if (JarnEsdeImport.importer?.state is JarnEsdeImportState.Done) return
    JarnButton(
        label = stringResource(R.string.jarnfe_esde_import_not_now),
        onClick = { actions.advanceFrom(JarnFirstRunStep.Esde) },
        modifier = Modifier.fillMaxWidth(),
        variant = JarnButtonVariant.Secondary,
    )
}
