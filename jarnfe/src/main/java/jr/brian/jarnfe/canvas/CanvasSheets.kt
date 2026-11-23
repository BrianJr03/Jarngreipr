package jr.brian.jarnfe.canvas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnSheet
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.shell.JarnNotices
import jr.brian.jarnfe.wallpaper.jarnWallpaperSourceOrNull

internal val CanvasSheetListMaxHeight = 360.dp

internal sealed interface CanvasListTarget {
    data class LastPlayed(val games: List<JarnCanvasRom>) : CanvasListTarget
}

/** A sheet that B closes. It leaves composition when closed, so focus returns to the grid at once. */
@Composable
internal fun CanvasModalSheet(
    title: String,
    initialFocus: FocusRequester?,
    onClose: () -> Unit,
    extraBindings: JarnBindingsBuilder.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
            extraBindings()
            on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = onClose)
        },
    ) {
        JarnSheet(
            visible = true,
            onDismiss = onClose,
            title = title,
            initialFocus = initialFocus,
            content = content,
        )
    }
}

private enum class CanvasOptionsView { List, Wallpaper, ConfirmTidy }

@Composable
internal fun CanvasOptionsSheet(
    onAdd: () -> Unit,
    onEdit: () -> Unit,
    onPages: () -> Unit,
    canEdit: Boolean,
    alreadyEditing: Boolean,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val data = remember(context) { context.jarnCanvasDataOrNull() }
    val hasWallpaper = remember(context) { context.jarnWallpaperSourceOrNull() != null }
    val page = JarnCanvas.snapshot?.pages?.getOrNull(JarnCanvas.selectedPage)?.takeIf { canEdit }
    var view by remember { mutableStateOf(CanvasOptionsView.List) }
    when (view) {
        CanvasOptionsView.Wallpaper -> CanvasWallpaperSheet(onBack = { view = CanvasOptionsView.List })
        CanvasOptionsView.ConfirmTidy -> CanvasTidyDialog(
            onConfirm = {
                if (data != null && page != null) tidyCanvasPage(JarnCanvas.undo, CanvasDataUndoWrites(data), page)
                onClose()
            },
            onDismiss = { view = CanvasOptionsView.List },
        )
        CanvasOptionsView.List -> CanvasOptionsList(
            onAdd = onAdd,
            onEdit = onEdit,
            onPages = onPages,
            canEdit = canEdit,
            alreadyEditing = alreadyEditing,
            onClose = onClose,
            onWallpaper = { view = CanvasOptionsView.Wallpaper }.takeIf { hasWallpaper },
            setup = { onAxisFocused ->
                if (data != null && page != null) {
                    CanvasSetupRows(
                        data = data,
                        page = page,
                        onTidy = {
                            if (canvasTidied(page.layout) == null) {
                                JarnNotices.show(R.string.jarnfe_canvas_notice_already_tidy)
                            } else {
                                view = CanvasOptionsView.ConfirmTidy
                            }
                        },
                        onAxisFocused = onAxisFocused,
                    )
                }
            },
        )
    }
}

@Composable
private fun CanvasOptionsList(
    onAdd: () -> Unit,
    onEdit: () -> Unit,
    onPages: () -> Unit,
    canEdit: Boolean,
    alreadyEditing: Boolean,
    onClose: () -> Unit,
    onWallpaper: (() -> Unit)?,
    setup: @Composable (onAxisFocused: (Boolean) -> Unit) -> Unit,
) {
    val first = remember { FocusRequester() }
    var axisFocused by remember { mutableStateOf(false) }
    CanvasModalSheet(
        title = stringResource(R.string.jarnfe_canvas_options_title),
        initialFocus = first,
        onClose = onClose,
        extraBindings = { if (axisFocused) hint(JarnAction.MoveLeft, R.string.jarnfe_canvas_hint_adjust) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = CanvasSheetListMaxHeight)
                .verticalScroll(rememberScrollState())
                .padding(vertical = JarnTheme.space.focusEdge),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        ) {
            JarnNavigateRow(
                label = stringResource(R.string.jarnfe_canvas_option_add),
                supporting = stringResource(R.string.jarnfe_canvas_option_add_detail),
                onClick = onAdd,
                enabled = canEdit,
                focusableWhenDisabled = true,
                modifier = Modifier.fillMaxWidth().focusRequester(first),
            )
            if (!alreadyEditing) {
                JarnNavigateRow(
                    label = stringResource(R.string.jarnfe_canvas_option_edit),
                    supporting = stringResource(R.string.jarnfe_canvas_option_edit_detail),
                    onClick = onEdit,
                    enabled = canEdit,
                    focusableWhenDisabled = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            setup { axisFocused = it }
            onWallpaper?.let {
                JarnNavigateRow(
                    label = stringResource(R.string.jarnfe_wallpaper_title),
                    supporting = stringResource(R.string.jarnfe_canvas_option_wallpaper_detail),
                    onClick = it,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            JarnNavigateRow(
                label = stringResource(R.string.jarnfe_canvas_option_pages),
                supporting = stringResource(R.string.jarnfe_canvas_option_pages_detail),
                onClick = onPages,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun CanvasListSheet(
    target: CanvasListTarget,
    onLaunchRom: (JarnCanvasRom) -> Unit,
    onClose: () -> Unit,
) {
    val first = remember(target) { FocusRequester() }
    when (target) {
        is CanvasListTarget.LastPlayed -> CanvasRowsSheet(
            title = stringResource(R.string.jarnfe_canvas_tile_last_played),
            emptyMessage = stringResource(R.string.jarnfe_canvas_last_played_empty),
            entries = target.games.map { game -> game.title to { onLaunchRom(game) } },
            first = first,
            onClose = onClose,
        )
    }
}

@Composable
private fun CanvasRowsSheet(
    title: String,
    emptyMessage: String,
    entries: List<Pair<String, () -> Unit>>,
    first: FocusRequester,
    onClose: () -> Unit,
) {
    CanvasModalSheet(title, first.takeIf { entries.isNotEmpty() }, onClose) {
        if (entries.isEmpty()) {
            JarnText(emptyMessage, JarnTheme.type.body, JarnTheme.colors.textMid)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = CanvasSheetListMaxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = JarnTheme.space.focusEdge),
                verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
            ) {
                entries.forEachIndexed { index, (label, onClick) ->
                    val focus = if (index == 0) Modifier.focusRequester(first) else Modifier
                    JarnNavigateRow(
                        label = label,
                        onClick = onClick,
                        modifier = Modifier.fillMaxWidth().then(focus),
                    )
                }
            }
        }
    }
}
