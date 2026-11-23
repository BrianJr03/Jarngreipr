package jr.brian.jarnfe.canvas

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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import jr.brian.core.model.GameImageType
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.focus.JarnRevealScrollTop
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.jarnfe.R

/** Moving through the list previews on the tile. Only A saves, so B leaves the tile as it was. */
@Composable
internal fun CanvasArtMediaSheet(
    item: CanvasItem.EsdeArtItem,
    onPreview: (GameImageType) -> Unit,
    onConfirm: (GameImageType) -> Unit,
    onClose: () -> Unit,
) {
    val current = item.resolvedImageType
    val first = remember(item.id) { FocusRequester() }
    val focusType = current.takeIf { it in CanvasArtMediaTypes } ?: CanvasArtMediaTypes.first()
    val scroll = rememberScrollState()
    CanvasModalSheet(stringResource(R.string.jarnfe_canvas_action_media_type), first, onClose) {
        JarnRevealScrollTop(scroll) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = CanvasSheetListMaxHeight)
                    .verticalScroll(scroll)
                    .padding(vertical = JarnTheme.space.focusEdge),
                verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
            ) {
                CanvasArtMediaTypes.forEach { type ->
                    CanvasArtMediaRow(
                        type = type,
                        isCurrent = type == current,
                        focusRequester = first.takeIf { type == focusType },
                        onPreview = onPreview,
                        onConfirm = onConfirm,
                    )
                }
            }
        }
    }
}

@Composable
private fun CanvasArtMediaRow(
    type: GameImageType,
    isCurrent: Boolean,
    focusRequester: FocusRequester?,
    onPreview: (GameImageType) -> Unit,
    onConfirm: (GameImageType) -> Unit,
) {
    val label = canvasArtMediaLabel(type) ?: return
    val focus = focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
    JarnNavigateRow(
        label = stringResource(label),
        supporting = if (isCurrent) stringResource(R.string.jarnfe_game_current) else null,
        onClick = { onConfirm(type) },
        modifier = Modifier
            .fillMaxWidth()
            .then(focus)
            .onFocusChanged { if (it.hasFocus) onPreview(type) },
    )
}
