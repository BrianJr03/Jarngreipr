package jr.brian.jarnfe.firstrun

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.LocalJarnScreen
import jr.brian.jarnfe.settings.JarnEsdeImportState
import jr.brian.jarnfe.settings.JarnEsdeImportSummaryCard
import jr.brian.jarnfe.settings.JarnEsdeImporter
import jr.brian.jarnfe.settings.WatchJarnEsdeImport
import jr.brian.jarnfe.settings.esdeImportProgressText
import jr.brian.jarnfe.settings.rememberJarnEsdeImporter

internal const val READY_ROW_ESDE_IMPORT = "esde-import"

/** The Ready card's ES-DE row, for ES-DE players with games. Null hides it. */
@Composable
internal fun esdeImportReadyRow(card: JarnReadyCard): JarnReadyRow? {
    val importer = rememberJarnEsdeImporter() ?: return null
    if (!card.hasGames || importer.detection?.worthOffering != true) return null
    WatchJarnEsdeImport(importer)
    val state = importer.state
    val screen = LocalJarnScreen.current
    val focus = remember { FocusRequester() }
    return JarnReadyRow(
        key = READY_ROW_ESDE_IMPORT,
        text = stringResource(
            if (state == JarnEsdeImportState.NeedsAccess) {
                R.string.jarnfe_esde_import_no_access
            } else {
                R.string.jarnfe_esde_import_ready_row
            },
        ),
        isDone = state is JarnEsdeImportState.Done,
        actionLabel = esdeImportProgressText(state) ?: stringResource(esdePrimaryLabelRes(state)),
        onAction = { importer.request(screen, returnFocus = focus) },
        order = 15,
        focus = focus,
    )
}

/** The import's summary under the Ready card's rows, once it's done. */
@Composable
internal fun FirstRunReadyEsdeSummary() {
    val importer: JarnEsdeImporter = rememberJarnEsdeImporter() ?: return
    val done = importer.state as? JarnEsdeImportState.Done ?: return
    JarnEsdeImportSummaryCard(done.summary)
}
