package jr.brian.jarnfe.systems

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnEmptyState
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.JarnRevealScrollTop
import jr.brian.core.ui.focus.LocalJarnTabsFocus
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.settings.SettingsGroup
import jr.brian.jarnfe.settings.SettingsSectionTitle
import jr.brian.jarnfe.shell.JarnNotices

@Composable
internal fun JarnSystemsRoute(manager: JarnSystemManager, onExit: () -> Unit) {
    val systems by manager.systems.collectAsState(initial = null)
    val folders by manager.addedFolders.collectAsState(initial = emptyList())
    var nav by remember { mutableStateOf(SystemsNav.opening(JarnSystems.takeTarget())) }
    var quickToggleId by remember { mutableStateOf<String?>(null) }
    val goBack = { nav.back()?.let { nav = it } ?: onExit() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) nav = nav.copy(page = SystemsPage.AddChoose(uri.toString()))
    }
    val openPicker = {
        runCatching { picker.launch(null) }
            .onFailure { JarnNotices.show(R.string.jarnfe_launch_folder_unavailable) }
        Unit
    }
    val current = nav.page
    val editedId = current.systemId()
    val edited = editedId?.let { id -> systems?.firstOrNull { it.id == id } }
    LaunchedEffect(editedId, systems) {
        if (editedId != null && systems != null && edited == null) nav = nav.copy(page = SystemsPage.List)
    }
    val toggleTarget = edited?.takeIf { current is SystemsPage.Editor }
        ?: quickToggleId?.takeIf { current == SystemsPage.List }
            ?.let { id -> systems?.firstOrNull { it.id == id } }
    JarnInputLayer(
        bindings = {
            systemsBindings(
                nav = nav,
                toggleTarget = toggleTarget,
                manager = manager,
                onBack = goBack,
                onNav = { nav = it },
            )
        },
    ) {
        when (current) {
            SystemsPage.List -> SystemListPage(
                systems = systems,
                folders = folders,
                nav = nav,
                manager = manager,
                onOpen = { nav = nav.openEditor(it) },
                onAdd = openPicker,
                onFocusSystem = { quickToggleId = it },
            )
            is SystemsPage.Editor -> edited?.let {
                SystemEditorPage(it, current.section, manager) { target -> nav = nav.copy(page = target) }
            }
            is SystemsPage.Emulators -> edited?.let {
                SystemEmulatorPage(it, current.allApps, manager, onBack = goBack) {
                    nav = nav.copy(page = current.copy(allApps = true))
                }
            }
            is SystemsPage.HiddenGames -> edited?.let { SystemHiddenGamesPage(it, manager, onBack = goBack) }
            is SystemsPage.AddChoose -> AddSystemChoosePage(
                treeUri = Uri.parse(current.treeUri),
                manager = manager,
                onNoGames = { id -> nav = nav.copy(page = SystemsPage.AddNoGames(current.treeUri, id)) },
                onDone = { nav = nav.copy(page = SystemsPage.List) },
            )
            is SystemsPage.AddNoGames -> AddSystemNoGamesPage(
                treeUri = Uri.parse(current.treeUri),
                systemId = current.systemId,
                manager = manager,
                onChooseAnother = {
                    nav = nav.copy(page = SystemsPage.List)
                    openPicker()
                },
                onDone = { nav = nav.copy(page = SystemsPage.List) },
            )
        }
    }
}

/** The system a page edits, so the flow leaves it when that system goes away. */
private fun SystemsPage.systemId(): String? = when (this) {
    is SystemsPage.Editor -> systemId
    is SystemsPage.Emulators -> systemId
    is SystemsPage.HiddenGames -> systemId
    else -> null
}

private fun JarnBindingsBuilder.systemsBindings(
    nav: SystemsNav,
    toggleTarget: JarnManagedSystem?,
    manager: JarnSystemManager,
    onBack: () -> Unit,
    onNav: (SystemsNav) -> Unit,
) {
    hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
    on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = onBack)
    toggleTarget?.let { system ->
        val label = if (system.isHidden) R.string.jarnfe_systems_show else R.string.jarnfe_systems_hide
        on(JarnAction.Context, label) { manager.setHidden(system.id, !system.isHidden) }
    }
    if (nav.page == SystemsPage.List) {
        on(JarnAction.Search, nav.filter.next().labelRes) { onNav(nav.cycleFilter()) }
    }
    if (nav.page is SystemsPage.Editor) {
        on(JarnAction.TabPrevious, R.string.jarnfe_hint_previous_tab) { onNav(nav.stepSection(-1)) }
        on(JarnAction.TabNext, R.string.jarnfe_hint_next_tab) { onNav(nav.stepSection(1)) }
    }
}

/** A scrolling page that keeps its heading in view and focused rows off the edges. */
@Composable
internal fun SystemsPageColumn(content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    JarnRevealScrollTop(scroll) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = JarnTheme.space.stageMargin, vertical = JarnTheme.space.focusEdge),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
            content = content,
        )
    }
}

/** Focuses [requester] once the page's first row has been laid out. */
@Composable
internal fun SystemsPageFocus(requester: FocusRequester, key: Any?) {
    val tabsFocus = LocalJarnTabsFocus.current
    LaunchedEffect(key, tabsFocus.handoffs) {
        withFrameNanos { }
        if (!tabsFocus.hasFocus) runCatching { requester.requestFocus() }
    }
}

@Composable
private fun SystemListPage(
    systems: List<JarnManagedSystem>?,
    folders: List<JarnAddedFolder>,
    nav: SystemsNav,
    manager: JarnSystemManager,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onFocusSystem: (String?) -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    val visible = systems.orEmpty().filter(nav.filter::matches)
    val restoreId = nav.focusedSystemId?.takeIf { id -> visible.any { it.id == id } }
    SystemsPageFocus(firstFocus, nav.filter to (systems != null))
    SystemsPageColumn {
        JarnText(
            text = stringResource(R.string.jarnfe_systems_title),
            style = JarnTheme.type.heading,
            color = JarnTheme.colors.textHigh,
            maxLines = 1,
        )
        JarnText(
            text = stringResource(R.string.jarnfe_systems_filter_status, stringResource(nav.filter.labelRes)),
            style = JarnTheme.type.label,
            color = JarnTheme.colors.textMid,
            maxLines = 1,
        )
        JarnNavigateRow(
            label = stringResource(R.string.jarnfe_systems_add),
            supporting = stringResource(R.string.jarnfe_systems_add_hint),
            onClick = onAdd,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (restoreId == null) Modifier.focusRequester(firstFocus) else Modifier)
                .onFocusChanged { if (it.isFocused) onFocusSystem(null) },
        )
        BulkVisibilityRows(systems.orEmpty(), manager, onFocusSystem)
        when {
            systems == null -> SystemsMessage(R.string.jarnfe_systems_loading)
            systems.isEmpty() -> JarnEmptyState(
                title = stringResource(R.string.jarnfe_systems_empty_title),
                message = stringResource(R.string.jarnfe_systems_empty_message),
            )
            visible.isEmpty() -> SystemsMessage(R.string.jarnfe_systems_filter_empty)
        }
        visible.forEach { system ->
            SystemRow(
                system = system,
                onOpen = { onOpen(system.id) },
                onFocused = { onFocusSystem(system.id) },
                modifier = if (system.id == restoreId) Modifier.focusRequester(firstFocus) else Modifier,
            )
        }
        AddedFolderRows(folders, manager, onFocusSystem)
    }
}

@Composable
private fun BulkVisibilityRows(
    systems: List<JarnManagedSystem>,
    manager: JarnSystemManager,
    onFocusSystem: (String?) -> Unit,
) {
    if (systems.isEmpty()) return
    val clearFocus = Modifier
        .fillMaxWidth()
        .onFocusChanged { if (it.isFocused) onFocusSystem(null) }
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_systems_show_all),
        onClick = { manager.setAllHidden(systems.map { it.id }, false) },
        modifier = clearFocus,
        enabled = systems.any { it.isHidden },
        focusableWhenDisabled = true,
    )
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_systems_hide_all),
        onClick = { manager.setAllHidden(systems.map { it.id }, true) },
        modifier = clearFocus,
        enabled = systems.any { !it.isHidden },
        focusableWhenDisabled = true,
    )
}

@Composable
private fun SystemRow(
    system: JarnManagedSystem,
    onOpen: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = pluralStringResource(R.plurals.jarnfe_systems_game_count, system.gameCount, system.gameCount)
    val supporting = if (system.isHidden) {
        stringResource(R.string.jarnfe_systems_row_hidden, count)
    } else {
        count
    }
    JarnNavigateRow(
        label = system.title,
        supporting = supporting,
        onClick = onOpen,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) onFocused() },
    )
}

@Composable
private fun AddedFolderRows(
    folders: List<JarnAddedFolder>,
    manager: JarnSystemManager,
    onFocusSystem: (String?) -> Unit,
) {
    if (folders.isEmpty()) return
    var pendingRemoval by remember { mutableStateOf<JarnAddedFolder?>(null) }
    SettingsSectionTitle(R.string.jarnfe_systems_added_folders)
    folders.forEach { folder ->
        JarnNavigateRow(
            label = folder.systemName,
            supporting = folder.displayPath,
            onClick = { pendingRemoval = folder },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) onFocusSystem(null) },
        )
    }
    pendingRemoval?.let { folder ->
        SystemConfirmDialog(
            title = stringResource(R.string.jarnfe_systems_remove_folder_title),
            message = stringResource(R.string.jarnfe_systems_remove_folder_message, folder.displayPath),
            confirmLabel = stringResource(R.string.jarnfe_systems_remove),
            onConfirm = {
                manager.removeFolder(folder)
                pendingRemoval = null
            },
            onDismiss = { pendingRemoval = null },
        )
    }
}

@Composable
internal fun SystemsMessage(messageRes: Int) {
    JarnText(stringResource(messageRes), JarnTheme.type.body, JarnTheme.colors.textMid, maxLines = 3)
}

/** The Settings entry into the flow. */
@Composable
internal fun JarnSystemsSection(onOpen: () -> Unit) {
    if (JarnSystems.manager == null) return
    SettingsGroup(R.string.jarnfe_systems_title) {
        JarnNavigateRow(
            label = stringResource(R.string.jarnfe_systems_manage),
            supporting = stringResource(R.string.jarnfe_systems_manage_hint),
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
