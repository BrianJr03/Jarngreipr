package jr.brian.jarnfe.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.LocalJarnScreen
import jr.brian.jarnfe.library.JarnLibrary

/** Settings, Library: "ES-DE", only for players who have ES-DE. Re-import first, then the toggle. */
@Composable
internal fun JarnEsdeSettingsGroup() {
    val importer = rememberJarnEsdeImporter() ?: return
    LaunchedEffect(importer) { importer.refreshDetection() }
    if (importer.detection?.found != true) return
    WatchJarnEsdeImport(importer, wantsNotice = true)
    SettingsGroup(R.string.jarnfe_esde_import_settings_group) {
        EsdeReimportRow(importer)
        ReadGamelistRow()
    }
}

/** No confirm: a re-import only fills gaps. The consent dialog shows only when access is missing. */
@Composable
private fun EsdeReimportRow(importer: JarnEsdeImporter) {
    val screen = LocalJarnScreen.current
    val focus = remember { FocusRequester() }
    RefocusAfterFileAccess(importer, focus)
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_esde_import_reimport),
        onClick = { importer.request(screen, returnFocus = focus) },
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
        supporting = esdeReimportHint(importer),
    )
}

@Composable
private fun esdeReimportHint(importer: JarnEsdeImporter): String {
    val last by importer.lastImport.collectAsState()
    val state = importer.state
    esdeImportProgressText(state)?.let { return it }
    return when {
        state is JarnEsdeImportState.Done -> esdeLastImportText(state.summary)
        state == JarnEsdeImportState.NeedsAccess -> stringResource(R.string.jarnfe_esde_import_no_access)
        state == JarnEsdeImportState.Failed -> stringResource(R.string.jarnfe_esde_import_failed)
        else -> last?.let { esdeLastImportText(it) } ?: stringResource(R.string.jarnfe_esde_import_reimport_hint)
    }
}

/** Classic's setting, so both front ends agree. Turning it on brings the details in right away. */
@Composable
private fun ReadGamelistRow() {
    val context = LocalContext.current
    val setting = remember(context) { context.jarnGamelistSettingOrNull() } ?: return
    val read by setting.readGamelist.collectAsState(setting.isReadingGamelist)
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_esde_import_read_gamelist),
        checked = read,
        onCheckedChange = { on ->
            setting.setReadGamelist(on)
            if (on) JarnLibrary.retry()
        },
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_esde_import_read_gamelist_hint),
    )
}
