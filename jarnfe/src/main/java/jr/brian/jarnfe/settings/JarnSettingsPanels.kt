package jr.brian.jarnfe.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.focus.JarnRevealLazyItems
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.apps.JarnAppsData
import jr.brian.jarnfe.apps.JarnIconPackListSheet
import jr.brian.jarnfe.apps.jarnAppsDataOrNull
import jr.brian.jarnfe.audio.JarnAudioPanel
import jr.brian.jarnfe.dock.JarnDockApp
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.rss.JarnRssClearPositionsDialog
import jr.brian.jarnfe.rss.jarnRssDataOrNull
import jr.brian.jarnfe.shell.JarnNotices
import jr.brian.jarnfe.tools.JarnInputToolsPanel
import jr.brian.jarnfe.tools.ToolsInitialFocus
import jr.brian.jarnfe.utilities.JarnUtilitiesPanel
import jr.brian.jarnfe.utilities.UtilityLayer
import jr.brian.jarnfe.utilities.UtilityNote
import jr.brian.jarnfe.utilities.UtilityReader
import jr.brian.jarnfe.utilities.UtilityTitle
import kotlinx.coroutines.launch

internal enum class JarnSettingsPanel { Tools, Utilities, Audio, DockApps, WhatsNew }

internal enum class JarnSettingsOverlay { IconPacks, ClearRssPositions }

internal class JarnSettingsActions(
    val openPanel: (JarnSettingsPanel) -> Unit,
    val openOverlay: (JarnSettingsOverlay) -> Unit,
    val overlayReturn: FocusRequester,
)

@Composable
internal fun JarnSettingsPanelHost(panel: JarnSettingsPanel, onClose: () -> Unit) {
    when (panel) {
        JarnSettingsPanel.Tools -> JarnInputToolsPanel(onClose = onClose)
        JarnSettingsPanel.Utilities -> JarnUtilitiesPanel(onClose = onClose)
        JarnSettingsPanel.Audio -> JarnAudioPanel(onClose = onClose)
        JarnSettingsPanel.DockApps -> JarnDockAppsPanel(onClose)
        JarnSettingsPanel.WhatsNew -> JarnWhatsNewPanel(onClose)
    }
}

/** Modals sit outside the scrolling page, and hand focus back to the row that opened them. */
@Composable
internal fun JarnSettingsOverlayHost(
    overlay: JarnSettingsOverlay?,
    returnFocus: FocusRequester,
    onClose: () -> Unit,
) {
    var wasOpen by remember { mutableStateOf(false) }
    LaunchedEffect(overlay) {
        if (overlay != null) {
            wasOpen = true
        } else if (wasOpen) {
            wasOpen = false
            withFrameNanos { }
            runCatching { returnFocus.requestFocus() }
        }
    }
    when (overlay) {
        JarnSettingsOverlay.IconPacks -> IconPackOverlay(onClose)
        JarnSettingsOverlay.ClearRssPositions -> ClearRssOverlay(onClose)
        null -> Unit
    }
}

@Composable
private fun IconPackOverlay(onClose: () -> Unit) {
    val context = LocalContext.current
    val data = remember(context) { context.jarnAppsDataOrNull() } ?: return
    val scope = rememberCoroutineScope()
    JarnIconPackListSheet(
        data = data,
        offerNone = true,
        onPick = { pack ->
            scope.launch { data.setIconPack(pack?.packageName) }
            onClose()
        },
        onClose = onClose,
    )
}

@Composable
private fun ClearRssOverlay(onClose: () -> Unit) {
    val context = LocalContext.current
    val rss = remember(context) { context.jarnRssDataOrNull() } ?: return
    JarnRssClearPositionsDialog(rss, onClose)
}

@Composable
private fun JarnDockAppsPanel(onClose: () -> Unit) {
    val context = LocalContext.current
    val settings = remember(context) { context.jarnLauncherSettingsOrNull() }
    val data = remember(context) { context.jarnAppsDataOrNull() }
    UtilityLayer(onBack = onClose, bindings = { hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm) }) {
        Column(
            modifier = Modifier.fillMaxSize().padding(JarnTheme.space.lg),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        ) {
            UtilityTitle(stringResource(R.string.jarnfe_launcher_dock_apps))
            if (settings == null || data == null) {
                UtilityNote(stringResource(R.string.jarnfe_utilities_unavailable))
            } else {
                DockAppsContent(settings, data)
            }
        }
    }
}

@Composable
private fun DockAppsContent(settings: JarnLauncherSettings, data: JarnAppsData) {
    val prefs = rememberJarnLauncherPrefs()
    val snapshot by remember(data) { data.snapshot }.collectAsState(initial = null)
    UtilityNote(stringResource(R.string.jarnfe_launcher_dock_count, prefs.dockCount, prefs.maxDockApps))
    val apps = remember(snapshot) {
        snapshot?.apps?.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }
    when {
        apps == null -> UtilityNote(stringResource(R.string.jarnfe_utilities_loading))
        apps.isEmpty() -> UtilityNote(stringResource(R.string.jarnfe_launcher_dock_no_apps))
        else -> DockAppList(apps, prefs.dockApps) { packageName, inDock ->
            if (!settings.setDockApp(packageName, inDock)) JarnNotices.show(R.string.jarnfe_launcher_dock_full)
        }
    }
}

@Composable
private fun DockAppList(
    apps: List<JarnDockApp>,
    docked: List<String>,
    onToggle: (String, Boolean) -> Unit,
) {
    val first = remember { FocusRequester() }
    val listState = rememberLazyListState()
    ToolsInitialFocus(first)
    JarnRevealLazyItems(listState) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        ) {
            itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                JarnToggleRow(
                    label = app.label,
                    checked = app.packageName in docked,
                    onCheckedChange = { onToggle(app.packageName, it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (index == 0) Modifier.focusRequester(first) else Modifier),
                )
            }
        }
    }
}

@Composable
private fun JarnWhatsNewPanel(onClose: () -> Unit) {
    val context = LocalContext.current
    val settings = remember(context) { context.jarnLauncherSettingsOrNull() }
    val notes by produceState<String?>(null, settings) {
        value = settings?.whatsNew()?.let(::jarnPlainPatchNotes).orEmpty()
    }
    val title = stringResource(R.string.jarnfe_launcher_whats_new)
    val loaded = notes
    if (loaded == null) {
        UtilityLayer(onBack = onClose) {
            Column(Modifier.fillMaxSize().padding(JarnTheme.space.lg)) {
                UtilityTitle(title)
                UtilityNote(stringResource(R.string.jarnfe_utilities_loading))
            }
        }
    } else {
        UtilityReader(title = title, body = loaded, onBack = onClose)
    }
}
