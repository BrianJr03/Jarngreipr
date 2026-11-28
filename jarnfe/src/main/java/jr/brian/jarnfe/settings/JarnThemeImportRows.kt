package jr.brian.jarnfe.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import jr.brian.core.model.theme.ThemeColor
import jr.brian.core.model.theme.ThemeColors
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.JarnLooks
import jr.brian.frontend.look.JarnLookStore
import jr.brian.frontend.look.esde.EsdeSystemRef
import jr.brian.frontend.look.esde.EsdeTranslateOptions
import jr.brian.frontend.look.themes.EsdeThemeScan
import jr.brian.frontend.look.themes.EsdeThemeUpdate
import jr.brian.frontend.look.themes.ThemeImportResult
import jr.brian.jarnfe.R
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.LIBRARY_HEADER_BAND
import jr.brian.jarnfe.look.jarnLookStoreOrNull
import jr.brian.jarnfe.shell.JarnNotices
import jr.brian.jarnfe.themekit.rememberThemeFilePicker
import jr.brian.jarnfe.themekit.themeImportMessage
import jr.brian.jarnfe.themes.EsdeImportStep
import jr.brian.jarnfe.themes.JarnEsdeThemeImportFlow
import jr.brian.jarnfe.themes.JarnEsdeThemeUpdatePending
import jr.brian.jarnfe.themes.JarnImportedThemes
import jr.brian.jarnfe.themes.JarnThemeChoice
import jr.brian.jarnfe.themes.applyThemeChoice
import jr.brian.jarnfe.themes.jarnEsdeThemeImport
import jr.brian.jarnfe.themes.jarnThemeImportOrNull
import jr.brian.jarnfe.themes.jarnThemesRoot
import kotlinx.coroutines.launch

/** Which import the panel runs: the theme list, or a re-import of an updated theme. */
internal sealed interface EsdeImportMode {
    data object Browse : EsdeImportMode
    data class Reimport(val update: EsdeThemeUpdate) : EsdeImportMode
}

/**
 * The Layout section's import entries: an update offer for the active ES-DE theme, Import from
 * ES-DE (opens in place) and Load a theme file.
 */
@Composable
internal fun JarnThemeImportRows(store: JarnLookStore, saved: JarnThemeChoice) {
    var mode by remember { mutableStateOf<EsdeImportMode?>(null) }
    val entryFocus = remember { FocusRequester() }
    PendingUpdateRows(saved) { mode = EsdeImportMode.Reimport(it) }
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_esde_import_entry),
        onClick = { mode = if (mode == null) EsdeImportMode.Browse else null },
        modifier = Modifier.fillMaxWidth().focusRequester(entryFocus),
        supporting = stringResource(R.string.jarnfe_esde_import_entry_hint),
    )
    mode?.let { current ->
        val close = {
            runCatching { entryFocus.requestFocus() }
            mode = null
        }
        EsdeImportPanel(current, onUse = { id -> useImported(store, saved, id); close() }, onClose = close)
    }
    ThemeFileRow()
}

@Composable
private fun PendingUpdateRows(saved: JarnThemeChoice, onReimport: (EsdeThemeUpdate) -> Unit) {
    val update = JarnEsdeThemeUpdatePending.update?.takeIf { it.themeId == saved.layout.id } ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    JarnValueRow(
        label = stringResource(R.string.jarnfe_esde_theme_updated),
        value = stringResource(R.string.jarnfe_esde_theme_reimport),
        onClick = { onReimport(update) },
        modifier = Modifier.fillMaxWidth(),
    )
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_esde_theme_not_now),
        onClick = {
            JarnEsdeThemeUpdatePending.update = null
            scope.launch { context.jarnThemeImportOrNull()?.jarnEsdeThemeUpdates?.dismiss(update) }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EsdeImportPanel(mode: EsdeImportMode, onUse: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val flow = remember(mode) { esdeImportFlow(context) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(flow) {
        when (mode) {
            EsdeImportMode.Browse -> flow.start()
            is EsdeImportMode.Reimport -> flow.reimport(mode.update)
        }
    }
    val step = flow.step
    LaunchedEffect(step) {
        if (step is EsdeImportStep.Done) JarnImportedThemes.refresh(context.jarnThemesRoot())
    }
    JarnEsdeImportSteps(
        step = step,
        onPick = { theme, settings -> scope.launch { flow.import(theme, settings) } },
        onRescan = { scope.launch { flow.start() } },
        onAllowAccess = { openAllFilesAccess(context) },
        onUse = { id ->
            if (JarnEsdeThemeUpdatePending.update?.themeId == id) JarnEsdeThemeUpdatePending.update = null
            onUse(id)
        },
        onClose = onClose,
        modifier = Modifier.fillMaxWidth().focusGroup(),
    )
}

@Composable
private fun ThemeFileRow() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pick = rememberThemeFilePicker { uri ->
        val importer = context.jarnThemeImportOrNull()?.jarnThemeFileImporter ?: return@rememberThemeFilePicker
        scope.launch {
            val result = importer.import(uri)
            if (result is ThemeImportResult.Installed) JarnImportedThemes.refresh(context.jarnThemesRoot())
            JarnNotices.show(themeImportMessage(result) ?: R.string.jarnfe_theme_file_added)
        }
    }
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_theme_file_entry),
        onClick = pick,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_theme_file_entry_hint),
    )
}

/** Each import reads with the saved Look as the colors' floor, and the player's systems from the library. */
private fun esdeImportFlow(context: Context): JarnEsdeThemeImportFlow {
    val locator = context.jarnThemeImportOrNull()?.jarnEsdeThemeLocator
    return JarnEsdeThemeImportFlow(
        scan = { locator?.scan() ?: EsdeThemeScan.NotFound(null) },
        runImport = { source, selection ->
            val look = JarnLooks.fromId(context.jarnLookStoreOrNull()?.lookId)
            val systems = JarnLibrary.state.systems.items.map { EsdeSystemRef(name = it.id, fullName = it.title) }
            val options = EsdeTranslateOptions(look.colors.toThemeColors(), source.name, LIBRARY_HEADER_BAND)
            context.jarnEsdeThemeImport(look).run(source, systems, selection, options)
        },
    )
}

private fun useImported(store: JarnLookStore, saved: JarnThemeChoice, id: String) {
    val entry = JarnImportedThemes.entries.firstOrNull { it.id == id } ?: return
    store.applyThemeChoice(saved.copy(layout = entry))
}

private fun openAllFilesAccess(context: Context) {
    val host = context as? Activity ?: return
    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, "package:${host.packageName}".toUri())
    runCatching { host.startActivity(intent) }
}

internal fun JarnColors.toThemeColors() = ThemeColors(
    bg = bg.theme(), surface = surface.theme(), raised = raised.theme(), textHigh = textHigh.theme(),
    textMid = textMid.theme(), textLow = textLow.theme(), accent = accent.theme(), danger = danger.theme(),
    success = success.theme(), line = line.theme(),
)

private fun Color.theme() = ThemeColor(toArgb())
