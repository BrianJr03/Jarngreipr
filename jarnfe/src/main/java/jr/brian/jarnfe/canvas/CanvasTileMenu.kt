package jr.brian.jarnfe.canvas

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnDialog
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.EsdeContentScale
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode

internal enum class CanvasTileAction(@param:StringRes val label: Int) {
    Open(R.string.jarnfe_canvas_action_open),
    UseWidget(R.string.jarnfe_canvas_action_use_widget),
    ChooseWidget(R.string.jarnfe_canvas_action_choose_widget),
    ChoosePhoto(R.string.jarnfe_canvas_action_choose_photo),
    PlayRandom(R.string.jarnfe_canvas_action_random),
    Games(R.string.jarnfe_canvas_action_games),
    Scale(R.string.jarnfe_canvas_action_scale),
    AppInfo(R.string.jarnfe_canvas_action_app_info),
    Move(R.string.jarnfe_canvas_action_move),
    Remove(R.string.jarnfe_canvas_action_remove),
}

/** Only actions that work right now, so the menu never offers a dead row. */
internal fun canvasTileActions(
    item: CanvasItem,
    resolved: Boolean,
    widgetHealth: JarnWidgetHealth?,
    hasRecent: Boolean,
): List<CanvasTileAction> {
    val specific = when (item) {
        is CanvasItem.AppItem -> if (resolved) {
            listOf(CanvasTileAction.Open, CanvasTileAction.AppInfo)
        } else {
            emptyList()
        }
        is CanvasItem.FolderItem -> if (resolved) listOf(CanvasTileAction.Open) else emptyList()
        is CanvasItem.RomItem -> if (resolved) {
            listOf(CanvasTileAction.Open, CanvasTileAction.Scale)
        } else {
            emptyList()
        }
        is CanvasItem.WidgetItem -> when (widgetHealth) {
            JarnWidgetHealth.Live -> listOf(CanvasTileAction.UseWidget)
            JarnWidgetHealth.Unbound -> listOf(CanvasTileAction.ChooseWidget)
            JarnWidgetHealth.Gone, null -> emptyList()
        }
        is CanvasItem.PhotoContainer -> listOf(CanvasTileAction.ChoosePhoto)
        is CanvasItem.EsdeArtItem -> listOf(CanvasTileAction.Open, CanvasTileAction.Scale)
        is CanvasItem.LastPlayedItem -> buildList {
            add(CanvasTileAction.Open)
            if (hasRecent) add(CanvasTileAction.PlayRandom)
            add(CanvasTileAction.Games)
            add(CanvasTileAction.Scale)
        }
        is CanvasItem.RssLauncherItem,
        is CanvasItem.RssMusicItem -> listOf(CanvasTileAction.Open)
    }
    return specific + CanvasTileAction.Move + CanvasTileAction.Remove
}

private fun EsdeContentScale.flipped(): EsdeContentScale =
    if (this == EsdeContentScale.FIT) EsdeContentScale.CROP else EsdeContentScale.FIT

/** The same tile with Fit and Crop swapped. Tiles without a scale come back unchanged. */
internal fun CanvasItem.withFlippedScale(): CanvasItem = when (this) {
    is CanvasItem.RomItem -> copy(contentScale = resolvedContentScale.flipped())
    is CanvasItem.EsdeArtItem -> copy(contentScale = resolvedContentScale.flipped())
    is CanvasItem.LastPlayedItem -> copy(contentScale = resolvedContentScale.flipped())
    else -> this
}

internal fun CanvasItem.LastPlayedItem.withNextCount(): CanvasItem.LastPlayedItem {
    val next = resolvedCount + 1
    return copy(
        count = if (next > CanvasItem.LastPlayedItem.MAX_DISPLAY_COUNT) {
            CanvasItem.LastPlayedItem.MIN_DISPLAY_COUNT
        } else {
            next
        },
    )
}

private fun CanvasItem.scaleOrNull(): EsdeContentScale? = when (this) {
    is CanvasItem.RomItem -> resolvedContentScale
    is CanvasItem.EsdeArtItem -> resolvedContentScale
    is CanvasItem.LastPlayedItem -> resolvedContentScale
    else -> null
}

@Composable
private fun actionDetail(action: CanvasTileAction, item: CanvasItem): String? = when (action) {
    CanvasTileAction.Scale -> item.scaleOrNull()?.let { scale ->
        stringResource(
            if (scale == EsdeContentScale.FIT) R.string.jarnfe_canvas_scale_fit else R.string.jarnfe_canvas_scale_crop,
        )
    }
    CanvasTileAction.Games -> (item as? CanvasItem.LastPlayedItem)?.resolvedCount?.toString()
    else -> null
}

@Composable
internal fun CanvasTileMenuSheet(
    item: CanvasItem,
    actions: List<CanvasTileAction>,
    onAction: (CanvasTileAction) -> Unit,
    onClose: () -> Unit,
) {
    val first = remember(item.id) { FocusRequester() }
    CanvasModalSheet(stringResource(R.string.jarnfe_canvas_tile_menu_title), first, onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = CanvasSheetListMaxHeight)
                .verticalScroll(rememberScrollState())
                .padding(vertical = JarnTheme.space.focusEdge),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        ) {
            actions.forEachIndexed { index, action ->
                val focus = if (index == 0) Modifier.focusRequester(first) else Modifier
                JarnNavigateRow(
                    label = stringResource(action.label),
                    supporting = actionDetail(action, item),
                    onClick = { onAction(action) },
                    modifier = Modifier.fillMaxWidth().then(focus),
                )
            }
        }
    }
}

@StringRes
private fun removeMessage(item: CanvasItem): Int = when (item) {
    is CanvasItem.FolderItem -> R.string.jarnfe_canvas_remove_folder_message
    is CanvasItem.WidgetItem -> R.string.jarnfe_canvas_remove_widget_message
    is CanvasItem.PhotoContainer -> R.string.jarnfe_canvas_remove_photo_message
    else -> R.string.jarnfe_canvas_remove_message
}

/** Focus starts on Cancel, because removing a widget or folder cannot be undone. */
@Composable
internal fun CanvasRemoveDialog(item: CanvasItem, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
            on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = onDismiss)
        },
    ) {
        JarnDialog(
            visible = true,
            title = stringResource(R.string.jarnfe_canvas_remove_title),
            message = stringResource(removeMessage(item)),
            confirmLabel = stringResource(R.string.jarnfe_canvas_action_remove),
            dismissLabel = stringResource(R.string.jarnfe_canvas_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
        )
    }
}
