package jr.brian.jarnfe.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.library.art.JarnLibraryData
import jr.brian.frontend.library.setup.GameFolder
import jr.brian.frontend.library.setup.GameFolderStatus
import jr.brian.frontend.library.setup.LibrarySetup
import jr.brian.jarnfe.R
import jr.brian.core.ui.focus.JarnRevealScrollTop
import jr.brian.jarnfe.library.JarnFolderPicker

@Composable
internal fun JarnSettingsRoute(data: JarnLibraryData, setup: LibrarySetup) {
    val firstRow = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstRow.requestFocus() }
    val scroll = rememberScrollState()
    JarnRevealScrollTop(scroll) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = JarnTheme.space.stageMargin, vertical = JarnTheme.space.focusEdge),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        ) {
            GameFoldersSection(setup, Modifier.focusRequester(firstRow))
            JarnArtSection(data)
        }
    }
}

@Composable
internal fun SettingsSectionTitle(@StringRes titleRes: Int) {
    JarnText(
        text = stringResource(titleRes),
        style = JarnTheme.type.labelStrong,
        color = JarnTheme.colors.textMid,
        maxLines = 1,
    )
}

@Composable
private fun GameFoldersSection(setup: LibrarySetup, addRowModifier: Modifier) {
    val folders by setup.folders.collectAsState()
    SettingsSectionTitle(R.string.jarnfe_folders_title)
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_setup_add_folder),
        onClick = { JarnFolderPicker.open?.invoke(null) },
        modifier = addRowModifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_folders_add_hint),
    )
    folders.forEach { folder -> GameFolderRow(folder) }
}

/** Only a folder that lost access has an action. The rest stay focusable to read. */
@Composable
private fun GameFolderRow(folder: GameFolder) {
    val needsReconnect = folder.status == GameFolderStatus.NeedsReconnect
    JarnNavigateRow(
        label = folder.name,
        onClick = { JarnFolderPicker.open?.invoke(folder.reconnectHint()) },
        modifier = Modifier.fillMaxWidth(),
        supporting = gameFolderStatusText(folder),
        enabled = needsReconnect,
        focusableWhenDisabled = true,
    )
}

private fun GameFolder.reconnectHint() = treeUri.takeIf { it.startsWith("content://") }?.toUri()

@Composable
private fun gameFolderStatusText(folder: GameFolder): String = when (folder.status) {
    GameFolderStatus.NeedsReconnect -> stringResource(R.string.jarnfe_folders_reconnect)
    GameFolderStatus.InsideAnother -> stringResource(R.string.jarnfe_folders_inside)
    GameFolderStatus.Connected -> pluralStringResource(
        R.plurals.jarnfe_folders_connected,
        folder.gameCount,
        folder.gameCount,
    )
}
