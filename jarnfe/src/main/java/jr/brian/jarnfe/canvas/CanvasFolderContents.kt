package jr.brian.jarnfe.canvas

import androidx.annotation.StringRes
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.JarnRevealScrollTop
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction

private const val FOLDER_GRID_COLUMNS = 4
private val FolderCellIconSize = 48.dp

@Composable
internal fun CanvasFolderContentsSheet(
    folder: JarnCanvasFolder,
    restoreFocusTo: String?,
    onOpenApp: (JarnCanvasApp) -> Unit,
    onAppOptions: (JarnCanvasApp) -> Unit,
    onRename: () -> Unit,
    onBackground: () -> Unit,
    onEditApps: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    var focusedApp by remember { mutableStateOf<JarnCanvasApp?>(null) }
    val actionFocus = remember { FocusRequester() }
    val appFocus = remember { FocusRequester() }
    val focusPackage = folder.apps.firstOrNull { it.packageName == restoreFocusTo }?.packageName
        ?: folder.apps.firstOrNull()?.packageName
    CanvasModalSheet(
        title = folder.name,
        initialFocus = if (focusPackage != null) appFocus else actionFocus,
        onClose = onClose,
        extraBindings = {
            focusedApp?.let { app ->
                on(JarnAction.Context, R.string.jarnfe_hint_context) { onAppOptions(app) }
            }
        },
    ) {
        JarnText(
            text = pluralStringResource(R.plurals.jarnfe_folder_app_count, folder.apps.size, folder.apps.size),
            style = JarnTheme.type.caption,
            color = JarnTheme.colors.textMid,
        )
        FolderActionRow(
            firstFocus = actionFocus,
            onFocused = { focusedApp = null },
            onRename = onRename,
            onBackground = onBackground,
            onEditApps = onEditApps,
            onDelete = onDelete,
        )
        if (folder.apps.isEmpty()) {
            JarnText(
                text = stringResource(R.string.jarnfe_canvas_folder_empty),
                style = JarnTheme.type.body,
                color = JarnTheme.colors.textMid,
            )
        } else {
            FolderAppGrid(
                apps = folder.apps,
                focusPackage = focusPackage,
                focusRequester = appFocus,
                onFocused = { focusedApp = it },
                onOpen = onOpenApp,
                onOptions = onAppOptions,
            )
        }
    }
}

@Composable
private fun FolderActionRow(
    firstFocus: FocusRequester,
    onFocused: () -> Unit,
    onRename: () -> Unit,
    onBackground: () -> Unit,
    onEditApps: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.hasFocus) onFocused() },
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
    ) {
        FolderActionButton(R.string.jarnfe_folder_edit_apps, onEditApps, Modifier.focusRequester(firstFocus))
        FolderActionButton(R.string.jarnfe_folder_rename, onRename)
        FolderActionButton(R.string.jarnfe_folder_background, onBackground)
        FolderActionButton(R.string.jarnfe_folder_delete, onDelete)
    }
}

@Composable
private fun RowScope.FolderActionButton(
    @StringRes labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    JarnButton(
        label = stringResource(labelRes),
        onClick = onClick,
        variant = JarnButtonVariant.Secondary,
        modifier = Modifier.weight(1f).then(modifier),
    )
}

@Composable
private fun FolderAppGrid(
    apps: List<JarnCanvasApp>,
    focusPackage: String?,
    focusRequester: FocusRequester,
    onFocused: (JarnCanvasApp) -> Unit,
    onOpen: (JarnCanvasApp) -> Unit,
    onOptions: (JarnCanvasApp) -> Unit,
) {
    val scroll = rememberScrollState()
    JarnRevealScrollTop(scroll) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = CanvasSheetListMaxHeight)
                .verticalScroll(scroll)
                .padding(vertical = JarnTheme.space.focusEdge),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        ) {
            apps.chunked(FOLDER_GRID_COLUMNS).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm)) {
                    row.forEach { app ->
                        val focus = if (app.packageName == focusPackage) {
                            Modifier.focusRequester(focusRequester)
                        } else {
                            Modifier
                        }
                        FolderAppCell(
                            app = app,
                            onFocused = { onFocused(app) },
                            onOpen = { onOpen(app) },
                            onOptions = { onOptions(app) },
                            modifier = Modifier.weight(1f).then(focus),
                        )
                    }
                    repeat(FOLDER_GRID_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** A opens and X gives options through the sheet's layer. Touch gets tap and long-press here. */
@Composable
private fun FolderAppCell(
    app: JarnCanvasApp,
    onFocused: () -> Unit,
    onOpen: () -> Unit,
    onOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val latestOpen by rememberUpdatedState(onOpen)
    val latestOptions by rememberUpdatedState(onOptions)
    Column(
        modifier = modifier
            .onFocusChanged { if (it.hasFocus) onFocused() }
            .jarnInteractive(JarnTheme.radius.mdShape, source, onClick = onOpen)
            .pointerInput(app.packageName) {
                detectTapGestures(onTap = { latestOpen() }, onLongPress = { latestOptions() })
            }
            .padding(JarnTheme.space.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
    ) {
        CanvasAppIcon(app, Modifier.size(FolderCellIconSize))
        JarnText(
            text = app.label,
            style = JarnTheme.type.caption,
            color = JarnTheme.colors.textHigh,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}
