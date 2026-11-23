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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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

@Composable
internal fun CanvasOptionsSheet(
    onAdd: () -> Unit,
    onEdit: () -> Unit,
    onPages: () -> Unit,
    canEdit: Boolean,
    onClose: () -> Unit,
) {
    val first = remember { FocusRequester() }
    CanvasModalSheet(stringResource(R.string.jarnfe_canvas_options_title), first, onClose) {
        JarnNavigateRow(
            label = stringResource(R.string.jarnfe_canvas_option_add),
            supporting = stringResource(R.string.jarnfe_canvas_option_add_detail),
            onClick = onAdd,
            enabled = canEdit,
            focusableWhenDisabled = true,
            modifier = Modifier.fillMaxWidth().focusRequester(first),
        )
        JarnNavigateRow(
            label = stringResource(R.string.jarnfe_canvas_option_edit),
            supporting = stringResource(R.string.jarnfe_canvas_option_edit_detail),
            onClick = onEdit,
            enabled = canEdit,
            focusableWhenDisabled = true,
            modifier = Modifier.fillMaxWidth(),
        )
        JarnNavigateRow(
            label = stringResource(R.string.jarnfe_canvas_option_pages),
            supporting = stringResource(R.string.jarnfe_canvas_option_pages_detail),
            onClick = onPages,
            modifier = Modifier.fillMaxWidth(),
        )
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
