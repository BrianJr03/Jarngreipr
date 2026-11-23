package jr.brian.jarnfe.canvas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.focus.JarnRevealScrollTop
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R

/** Picks apps, then a name. Going back from the name keeps the picks. */
@Composable
internal fun CanvasFolderCreateFlow(
    data: JarnCanvasData,
    defaultName: String,
    onCreate: (String, List<String>) -> Unit,
    onClose: () -> Unit,
) {
    var apps by remember { mutableStateOf<List<JarnCanvasApp>?>(null) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var naming by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { apps = runCatching { data.installedApps() }.getOrDefault(emptyList()) }
    if (naming) {
        CanvasKeyboardSheet(
            label = stringResource(R.string.jarnfe_folder_name_label),
            onDone = { text -> onCreate(jarnFolderNameOrNull(text) ?: defaultName, selected.toList()) },
            onCancel = { naming = false },
        )
    } else {
        CanvasFolderAppsPicker(
            title = stringResource(R.string.jarnfe_canvas_pick_folder_title),
            apps = apps,
            selected = selected,
            onSelected = { selected = it },
            confirmLabel = stringResource(R.string.jarnfe_folder_next, selected.size),
            canConfirmEmpty = false,
            onConfirm = { naming = true },
            onClose = onClose,
        )
    }
}

/** Saving with nothing picked asks first, because that deletes the folder. */
@Composable
internal fun CanvasFolderEditApps(
    data: JarnCanvasData,
    folder: JarnCanvasFolder,
    onSave: (added: Set<String>, removed: Set<String>) -> Unit,
    onDeleteFolder: () -> Unit,
    onClose: () -> Unit,
) {
    val current = remember(folder.id) { folder.apps.map { it.packageName }.toSet() }
    var apps by remember { mutableStateOf<List<JarnCanvasApp>?>(null) }
    var selected by remember(folder.id) { mutableStateOf(current) }
    var confirmEmpty by remember { mutableStateOf(false) }
    LaunchedEffect(folder.id) {
        val installed = runCatching { data.installedApps() }.getOrDefault(emptyList())
        apps = jarnFolderPickerApps(installed, folder.apps)
    }
    if (confirmEmpty) {
        CanvasFolderConfirmDialog(
            messageRes = R.string.jarnfe_folder_empty_message,
            onConfirm = onDeleteFolder,
            onDismiss = { confirmEmpty = false },
        )
    } else {
        CanvasFolderAppsPicker(
            title = stringResource(R.string.jarnfe_folder_edit_title),
            apps = apps,
            selected = selected,
            onSelected = { selected = it },
            confirmLabel = stringResource(R.string.jarnfe_folder_save, selected.size),
            canConfirmEmpty = true,
            onConfirm = {
                if (selected.isEmpty()) {
                    confirmEmpty = true
                } else {
                    onSave(selected - current, current - selected)
                }
            },
            onClose = onClose,
        )
    }
}

@Composable
private fun CanvasFolderAppsPicker(
    title: String,
    apps: List<JarnCanvasApp>?,
    selected: Set<String>,
    onSelected: (Set<String>) -> Unit,
    confirmLabel: String,
    canConfirmEmpty: Boolean,
    onConfirm: () -> Unit,
    onClose: () -> Unit,
) {
    if (apps.isNullOrEmpty()) {
        CanvasModalSheet(title, null, onClose) {
            JarnText(
                text = stringResource(
                    if (apps == null) R.string.jarnfe_canvas_pick_loading else R.string.jarnfe_canvas_pick_empty,
                ),
                style = JarnTheme.type.body,
                color = JarnTheme.colors.textMid,
            )
        }
        return
    }
    val first = remember { FocusRequester() }
    val scroll = rememberScrollState()
    CanvasModalSheet(title, first, onClose) {
        JarnRevealScrollTop(scroll) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = CanvasSheetListMaxHeight)
                    .verticalScroll(scroll)
                    .padding(vertical = JarnTheme.space.focusEdge),
                verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
            ) {
                JarnNavigateRow(
                    label = stringResource(R.string.jarnfe_folder_show_all),
                    onClick = { onSelected(apps.map { it.packageName }.toSet()) },
                    modifier = Modifier.fillMaxWidth().focusRequester(first),
                )
                JarnNavigateRow(
                    label = stringResource(R.string.jarnfe_folder_hide_all),
                    onClick = { onSelected(emptySet()) },
                    modifier = Modifier.fillMaxWidth(),
                )
                apps.forEach { app ->
                    JarnToggleRow(
                        label = app.label,
                        checked = app.packageName in selected,
                        onCheckedChange = { on ->
                            onSelected(if (on) selected + app.packageName else selected - app.packageName)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        JarnButton(
            label = confirmLabel,
            onClick = onConfirm,
            enabled = canConfirmEmpty || selected.isNotEmpty(),
            focusableWhenDisabled = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
