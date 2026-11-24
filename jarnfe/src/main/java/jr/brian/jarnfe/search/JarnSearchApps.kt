package jr.brian.jarnfe.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import jr.brian.jarnfe.R
import jr.brian.jarnfe.apps.JarnAppDisplayAsk
import jr.brian.jarnfe.apps.JarnAppOptionsFlow
import jr.brian.jarnfe.apps.JarnAppOptionsTarget
import jr.brian.jarnfe.apps.JarnAppTile
import jr.brian.jarnfe.apps.JarnAppsCommands
import jr.brian.jarnfe.apps.jarnAppsDataOrNull
import jr.brian.jarnfe.apps.launchOrAsk
import jr.brian.jarnfe.apps.rememberJarnAppsCommands
import jr.brian.jarnfe.dock.JarnDockApp
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.LibraryFocusGrid
import jr.brian.jarnfe.library.LibrarySelection
import jr.brian.jarnfe.look.rememberJarnDisplay
import jr.brian.jarnfe.settings.rememberJarnLauncherPrefs

private val SearchAppCellMinSize = 104.dp

/** App results: A opens the app like the Apps drawer, Select opens its options. */
@Composable
internal fun SearchAppGrid(apps: List<JarnDockApp>, search: JarnSearchStore = JarnSearch) {
    val context = LocalContext.current
    val data = remember(context) { context.jarnAppsDataOrNull() } ?: return
    val commands = rememberJarnAppsCommands(data)
    var menuKey by remember { mutableStateOf<String?>(null) }
    var askKey by remember { mutableStateOf<String?>(null) }
    var gridFocused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { JarnLibrary.update { focusSearchResult(LibrarySelection.None) } }
    SearchAppInput(
        focused = search.focusedApp.takeIf { gridFocused && menuKey == null && askKey == null },
        onOptions = { menuKey = it },
    )
    Box(Modifier.fillMaxSize()) {
        SearchAppTiles(apps, search, commands, Modifier.onFocusChanged { gridFocused = it.hasFocus }) { askKey = it }
        apps.firstOrNull { it.packageName == menuKey }?.let { app ->
            SearchAppOptions(app, commands) { menuKey = null }
        }
        askKey?.let { packageName -> JarnAppDisplayAsk(packageName, commands) { askKey = null } }
    }
}

@Composable
private fun SearchAppTiles(
    apps: List<JarnDockApp>,
    search: JarnSearchStore,
    commands: JarnAppsCommands,
    modifier: Modifier,
    onAsk: (String) -> Unit,
) {
    val labelSize = rememberJarnLauncherPrefs().appLabelSize
    val namesVisible = rememberJarnDisplay().appNamesVisible
    LibraryFocusGrid(
        items = apps,
        itemKey = JarnDockApp::packageName,
        focusedKey = search.focusedApp?.takeIf { key -> apps.any { it.packageName == key } },
        onFocused = { search.focusedApp = it },
        cellMinSize = SearchAppCellMinSize,
        modifier = modifier,
        refocusSignal = JarnLibrary.stageRefocus,
    ) { app, modifier ->
        JarnAppTile(
            app = app,
            labelSize = labelSize,
            nameVisible = namesVisible,
            modifier = modifier.fillMaxWidth(),
            onClick = { commands.launchOrAsk(app.packageName, onAsk) },
        )
    }
}

@Composable
private fun SearchAppInput(focused: String?, onOptions: (String) -> Unit) {
    JarnInputLayer(
        enabled = focused != null,
        bindings = {
            if (focused != null) {
                hint(JarnAction.Confirm, R.string.jarnfe_hint_open)
                on(JarnAction.Context, R.string.jarnfe_hint_context) { onOptions(focused) }
            }
        },
    )
}

@Composable
private fun SearchAppOptions(app: JarnDockApp, commands: JarnAppsCommands, onClose: () -> Unit) {
    JarnAppOptionsFlow(
        data = commands.options,
        target = JarnAppOptionsTarget(app.packageName, app.label, app.iconPath != null),
        extras = emptyList(),
        onClose = onClose,
        onLeave = onClose,
        onListChange = onClose,
    )
}
