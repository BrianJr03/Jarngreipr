package jr.brian.jarnfe.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import jr.brian.core.ui.components.JarnEmptyState
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnTabs
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.JarnRevealScrollTop
import jr.brian.core.ui.focus.LocalJarnTabsFocus
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.library.art.JarnLibraryData
import jr.brian.frontend.library.setup.GameFolder
import jr.brian.frontend.library.setup.GameFolderStatus
import jr.brian.frontend.library.setup.LibrarySetup
import jr.brian.jarnfe.R
import jr.brian.jarnfe.backup.JarnDataHost
import jr.brian.jarnfe.backup.JarnDataSection
import jr.brian.jarnfe.library.JarnFolderPicker
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.jarnLibraryDataOrNull
import jr.brian.jarnfe.library.jarnLibrarySetupOrNull
import jr.brian.jarnfe.scraper.JarnScraperSetupHost
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.SettingsSection
import jr.brian.jarnfe.session.VisibleSettingsSections
import jr.brian.jarnfe.shell.JarnNotices
import jr.brian.jarnfe.systems.JarnSystems
import jr.brian.jarnfe.systems.JarnSystemsSection
import jr.brian.jarnfe.tools.JarnInputToolsPanel
import jr.brian.jarnfe.utilities.JarnUtilitiesPanel
import jr.brian.jarnfe.wallpaper.jarnDisabledRowBacking

private val SideBySideMinWidth = 600.dp

/** With one screen, controls and preview share it, and a narrow window keeps only the controls. */
@Composable
internal fun JarnSettingsSingleScreen(section: SettingsSection) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth < SideBySideMinWidth) {
            JarnSettingsControls(section, JarnTheme.space.stageMargin)
        } else {
            Row(Modifier.fillMaxSize()) {
                JarnSettingsPreview(Modifier.weight(1f).fillMaxHeight())
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    JarnSettingsControls(section, JarnTheme.space.md)
                }
            }
        }
    }
    JarnSettingsScraperHost()
}

@Composable
internal fun JarnSettingsControls(section: SettingsSection, horizontalMargin: Dp) {
    val context = LocalContext.current
    val data = remember(context) { context.jarnLibraryDataOrNull() }
    val setup = remember(context) { context.jarnLibrarySetupOrNull() }
    if (data == null || setup == null) {
        JarnSettingsUnavailable()
        return
    }
    var toolsOpen by rememberSaveable { mutableStateOf(false) }
    if (toolsOpen) {
        JarnInputToolsPanel(onClose = { toolsOpen = false })
        return
    }
    var utilitiesOpen by rememberSaveable { mutableStateOf(false) }
    if (utilitiesOpen) {
        JarnUtilitiesPanel(onClose = { utilitiesOpen = false })
        return
    }
    Column(Modifier.fillMaxSize()) {
        SettingsSectionTabs(section, horizontalMargin)
        key(section) {
            SettingsSectionPage(horizontalMargin, Modifier.weight(1f).fillMaxWidth()) {
                SettingsSectionContent(
                    section = section,
                    data = data,
                    setup = setup,
                    onOpenTools = { toolsOpen = true },
                    onOpenUtilities = { utilitiesOpen = true },
                )
            }
        }
    }
}

/** Sits outside the panes so its sheet can use the whole window. */
@Composable
internal fun JarnSettingsScraperHost() {
    val context = LocalContext.current
    JarnDataHost()
    val data = remember(context) { context.jarnLibraryDataOrNull() } ?: return
    JarnScraperSetupHost(data)
}

@Composable
private fun JarnSettingsUnavailable() {
    JarnEmptyState(
        title = stringResource(R.string.jarnfe_empty_settings_title),
        message = stringResource(R.string.jarnfe_empty_settings_message),
        actionLabel = stringResource(R.string.jarnfe_back_to_library),
        onAction = { JarnSession.update { back() } },
    )
}

@StringRes
private fun SettingsSection.labelRes(): Int = when (this) {
    SettingsSection.Library -> R.string.jarnfe_settings_section_library
    SettingsSection.Art -> R.string.jarnfe_art_title
    SettingsSection.Look -> R.string.jarnfe_look_title
    SettingsSection.Display -> R.string.jarnfe_settings_section_display
    SettingsSection.Data -> R.string.jarnfe_settings_section_data
}

@Composable
private fun SettingsSectionTabs(selected: SettingsSection, horizontalMargin: Dp) {
    JarnTabs(
        labels = VisibleSettingsSections.map { stringResource(it.labelRes()) },
        selectedIndex = VisibleSettingsSections.indexOf(selected),
        onSelect = { index ->
            JarnSession.update { copy(settingsSection = VisibleSettingsSections[index]) }
        },
        modifier = Modifier.padding(horizontal = horizontalMargin),
    )
}

@Composable
private fun SettingsSectionPage(
    horizontalMargin: Dp,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val firstRow = remember { FocusRequester() }
    val tabsFocus = LocalJarnTabsFocus.current
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (!tabsFocus.hasFocus) firstRow.requestFocus()
    }
    val scroll = rememberScrollState()
    JarnRevealScrollTop(scroll) {
        Column(
            modifier = modifier
                .focusRequester(firstRow)
                .focusGroup()
                .verticalScroll(scroll)
                .padding(horizontal = horizontalMargin, vertical = JarnTheme.space.focusEdge),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        ) { content() }
    }
}

@Composable
private fun SettingsSectionContent(
    section: SettingsSection,
    data: JarnLibraryData,
    setup: LibrarySetup,
    onOpenTools: () -> Unit,
    onOpenUtilities: () -> Unit,
) {
    when (section) {
        SettingsSection.Library -> LibrarySettings(setup)
        SettingsSection.Art -> JarnArtSection(data)
        SettingsSection.Look -> {
            JarnLookSection()
            JarnWallpaperSection()
        }
        SettingsSection.Display -> {
            JarnDisplaySection()
            InputToolsRow(onOpenTools)
            UtilitiesRow(onOpenUtilities)
        }
        SettingsSection.Data -> JarnDataSection()
    }
}

@Composable
private fun UtilitiesRow(onOpen: () -> Unit) {
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_utilities_title),
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_utilities_settings_hint),
    )
}

@Composable
private fun InputToolsRow(onOpen: () -> Unit) {
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_tools_title),
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_tools_settings_hint),
    )
}

@Composable
private fun LibrarySettings(setup: LibrarySetup) {
    GameFoldersSection(setup)
    JarnSystemsSection(onOpen = ::openSystemManager)
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_settings_refresh),
        onClick = ::refreshLibrary,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_settings_refresh_hint),
    )
}

private fun refreshLibrary() {
    JarnLibrary.retry()
    JarnNotices.show(R.string.jarnfe_settings_refresh_started)
}

private fun openSystemManager() {
    JarnSystems.openAt(null)
    JarnSession.update { openSystems() }
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
private fun GameFoldersSection(setup: LibrarySetup) {
    val folders by setup.folders.collectAsState()
    SettingsSectionTitle(R.string.jarnfe_folders_title)
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_setup_add_folder),
        onClick = { JarnFolderPicker.open?.invoke(null) },
        modifier = Modifier.fillMaxWidth(),
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
        modifier = Modifier.fillMaxWidth().jarnDisabledRowBacking(needsReconnect),
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
