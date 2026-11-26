package jr.brian.jarnfe.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnChip
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.core.ui.focus.LocalJarnTabsFocus
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.apps.jarnAppsDataOrNull
import jr.brian.jarnfe.dock.JarnDockApp
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.session.JarnSession
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

/** Edits the two back button slots. The values are the same ones Classic reads and backs up. */
@Composable
internal fun JarnBackShortcutsPanel(
    tools: JarnInputTools,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shortcuts by tools.backShortcuts.collectAsState(JarnBackShortcuts())
    val apps by rememberPickableApps()
    var slot by remember { mutableStateOf(JarnBackSlot.Primary) }
    var pickingApp by remember { mutableStateOf(false) }
    var returnToAppRow by remember { mutableStateOf(false) }
    if (pickingApp) {
        BackShortcutAppPicker(
            apps = apps,
            onPick = { packageName ->
                tools.setBackShortcutApp(slot, packageName)
                tools.setBackShortcut(slot, JarnBackShortcut.APP)
                returnToAppRow = true
                pickingApp = false
            },
            onClose = {
                returnToAppRow = true
                pickingApp = false
            },
            modifier = modifier,
        )
    } else {
        BackShortcutEditor(
            shortcuts = shortcuts,
            apps = apps,
            slot = slot,
            focusAppRow = returnToAppRow,
            onSlot = { slot = it },
            onChoose = { choice ->
                if (choice == JarnBackShortcut.APP) pickingApp = true else tools.setBackShortcut(slot, choice)
            },
            onEnabled = tools::setBackShortcutsEnabled,
            onClose = onClose,
            modifier = modifier,
        )
    }
}

@Composable
private fun rememberPickableApps(): androidx.compose.runtime.State<List<JarnDockApp>> {
    val context = LocalContext.current
    val flow = remember(context) {
        context.jarnAppsDataOrNull()?.snapshot
            ?.map { snapshot -> snapshot.apps.sortedBy { it.label.lowercase() } }
            ?: emptyFlow()
    }
    return flow.collectAsState(emptyList())
}

@Composable
private fun BackShortcutEditor(
    shortcuts: JarnBackShortcuts,
    apps: List<JarnDockApp>,
    slot: JarnBackSlot,
    focusAppRow: Boolean,
    onSlot: (JarnBackSlot) -> Unit,
    onChoose: (JarnBackShortcut) -> Unit,
    onEnabled: (Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toggle = remember { FocusRequester() }
    val appRow = remember { FocusRequester() }
    ToolsInitialFocus(if (focusAppRow && shortcuts.enabled) appRow else toggle)
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
            on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = onClose)
            on(JarnAction.SwitchScreen, R.string.jarnfe_hint_switch_screen) {
                JarnSession.update { switchActiveScreen() }
            }
            if (shortcuts.enabled) {
                on(JarnAction.TabPrevious) { onSlot(JarnBackSlot.Primary) }
                on(JarnAction.TabNext) { onSlot(JarnBackSlot.Secondary) }
            }
        },
    ) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(JarnTheme.space.lg),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        ) {
            item(key = "enabled") {
                JarnToggleRow(
                    label = stringResource(R.string.jarnfe_tools_back_enabled),
                    checked = shortcuts.enabled,
                    onCheckedChange = onEnabled,
                    modifier = Modifier.focusRequester(toggle),
                )
            }
            if (shortcuts.enabled) {
                item(key = "slots") { BackSlotChips(slot, onSlot) }
                items(jarnBackShortcutOptions(slot), key = { it.name }) { option ->
                    BackShortcutRow(
                        option = option,
                        selected = shortcuts.shortcut(slot) == option,
                        appLabel = apps.firstOrNull { it.packageName == shortcuts.app(slot) }?.label,
                        onClick = { onChoose(option) },
                        modifier = if (option == JarnBackShortcut.APP) Modifier.focusRequester(appRow) else Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun BackSlotChips(slot: JarnBackSlot, onSlot: (JarnBackSlot) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.xs)) {
        JarnChip(
            label = stringResource(R.string.jarnfe_tools_slot_primary),
            selected = slot == JarnBackSlot.Primary,
            onClick = { onSlot(JarnBackSlot.Primary) },
        )
        JarnChip(
            label = stringResource(R.string.jarnfe_tools_slot_secondary),
            selected = slot == JarnBackSlot.Secondary,
            onClick = { onSlot(JarnBackSlot.Secondary) },
        )
    }
}

@Composable
private fun BackShortcutRow(
    option: JarnBackShortcut,
    selected: Boolean,
    appLabel: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val supporting = when {
        option != JarnBackShortcut.APP || !selected -> null
        appLabel != null -> stringResource(R.string.jarnfe_tools_selected_app, appLabel)
        else -> stringResource(R.string.jarnfe_tools_no_app)
    }
    JarnValueRow(
        label = stringResource(option.labelRes),
        value = if (selected) stringResource(R.string.jarnfe_tools_selected) else "",
        onClick = onClick,
        modifier = modifier,
        supporting = supporting,
    )
}

@Composable
private fun BackShortcutAppPicker(
    apps: List<JarnDockApp>,
    onPick: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(apps.isEmpty()) {
        if (apps.isNotEmpty()) {
            withFrameNanos { }
            runCatching { first.requestFocus() }
        }
    }
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
            on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = onClose)
        },
    ) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(JarnTheme.space.lg),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        ) {
            item(key = "title") {
                JarnText(
                    text = stringResource(R.string.jarnfe_tools_pick_app),
                    style = JarnTheme.type.label,
                    color = JarnTheme.colors.textHigh,
                )
            }
            items(apps, key = { it.packageName }) { app ->
                JarnNavigateRow(
                    label = app.label,
                    onClick = { onPick(app.packageName) },
                    modifier = if (app == apps.first()) Modifier.focusRequester(first) else Modifier,
                )
            }
        }
    }
}

/** Waits a frame so lazy rows exist, and leaves focus alone while a tab row holds it. */
@Composable
internal fun ToolsInitialFocus(target: FocusRequester) {
    val tabsFocus = LocalJarnTabsFocus.current
    LaunchedEffect(tabsFocus.handoffs) {
        withFrameNanos { }
        if (!tabsFocus.hasFocus) runCatching { target.requestFocus() }
    }
}
