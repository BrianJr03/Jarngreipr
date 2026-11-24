package jr.brian.jarnfe.canvas

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnScrim
import jr.brian.core.ui.components.JarnScrimEdge
import jr.brian.core.ui.components.JarnSurface
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.jarnfe.R
import jr.brian.jarnfe.session.JarnSessionState
import kotlinx.coroutines.flow.distinctUntilChanged

/** What Stage shows about the tile in hand. Only whole-cell changes produce a new value. */
internal data class CanvasStageStatus(
    val tileName: String?,
    @param:StringRes val tileNameRes: Int,
    val colSpan: Int,
    val rowSpan: Int,
    val pageNumber: Int,
    val pageCount: Int,
    val holding: Boolean,
)

/** Shared by the Stage and Deck windows, like [JarnCanvas]. */
internal object CanvasStageBoard {
    var status by mutableStateOf<CanvasStageStatus?>(null)
}

internal fun canvasStageStatus(
    editor: CanvasEditor,
    page: JarnCanvasPage?,
    pageCount: Int,
    focusedId: String?,
    isActive: Boolean,
): CanvasStageStatus? {
    if (!isActive || !editor.editing || page == null) return null
    val id = editor.grab?.itemId ?: focusedId ?: return null
    val rect = editor.placements(page.layout)[id] ?: return null
    val item = page.layout.items.firstOrNull { it.id == id } ?: return null
    return CanvasStageStatus(
        tileName = item.savedName(page),
        tileNameRes = item.kindNameRes(),
        colSpan = rect.colSpan,
        rowSpan = rect.rowSpan,
        pageNumber = page.index + 1,
        pageCount = pageCount,
        holding = editor.grab != null,
    )
}

private fun CanvasItem.savedName(page: JarnCanvasPage): String? = when (this) {
    is CanvasItem.AppItem -> page.apps[packageName]?.label
    is CanvasItem.FolderItem -> page.folders[folderId]?.name
    is CanvasItem.RomItem -> page.roms[romKey]?.title
    else -> null
}

@StringRes
private fun CanvasItem.kindNameRes(): Int = when (this) {
    is CanvasItem.WidgetItem -> R.string.jarnfe_canvas_tile_widget
    is CanvasItem.RssLauncherItem -> R.string.jarnfe_canvas_tile_rss
    is CanvasItem.RssMusicItem -> R.string.jarnfe_canvas_stage_music
    is CanvasItem.EsdeArtItem -> R.string.jarnfe_canvas_tile_art
    is CanvasItem.PhotoContainer -> R.string.jarnfe_canvas_tile_photo
    is CanvasItem.LastPlayedItem -> R.string.jarnfe_canvas_tile_last_played
    else -> R.string.jarnfe_canvas_stage_tile
}

@Composable
internal fun PublishCanvasStageStatus(
    editor: CanvasEditor,
    page: JarnCanvasPage?,
    pageCount: Int,
    focusedId: String?,
    isActive: Boolean,
) {
    val latestPage by rememberUpdatedState(page)
    val latestCount by rememberUpdatedState(pageCount)
    val latestFocus by rememberUpdatedState(focusedId)
    val latestActive by rememberUpdatedState(isActive)
    LaunchedEffect(editor) {
        snapshotFlow { canvasStageStatus(editor, latestPage, latestCount, latestFocus, latestActive) }
            .distinctUntilChanged()
            .collect { CanvasStageBoard.status = it }
    }
    DisposableEffect(editor) {
        onDispose { CanvasStageBoard.status = null }
    }
}

@Composable
internal fun CanvasStagePreview(state: JarnSessionState) {
    if (!state.deckDisplayAvailable) return
    val status = CanvasStageBoard.status
    val lastShown = remember { arrayOfNulls<CanvasStageStatus>(1) }
    if (status != null) lastShown[0] = status
    val motion = JarnTheme.motion
    AnimatedVisibility(
        visible = status != null,
        enter = fadeIn(motion.panel.spec()),
        exit = fadeOut(motion.panel.spec()),
    ) {
        lastShown[0]?.let { CanvasStageCard(it) }
    }
}

@Composable
private fun CanvasStageCard(status: CanvasStageStatus) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        JarnScrim(JarnScrimEdge.Full, Modifier.fillMaxSize())
        JarnSurface(Modifier.padding(JarnTheme.space.stageMargin)) {
            Column(
                modifier = Modifier.padding(JarnTheme.space.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
            ) {
                CanvasStageCardText(status)
            }
        }
    }
}

@Composable
private fun CanvasStageCardText(status: CanvasStageStatus) {
    val colors = JarnTheme.colors
    val action = if (status.holding) R.string.jarnfe_canvas_stage_holding else R.string.jarnfe_canvas_arrange_title
    JarnText(stringResource(action), JarnTheme.type.labelStrong, colors.accent, maxLines = 1)
    JarnText(
        text = status.tileName ?: stringResource(status.tileNameRes),
        style = JarnTheme.type.title,
        color = colors.textHigh,
        maxLines = 2,
    )
    JarnText(
        text = stringResource(R.string.jarnfe_canvas_stage_size, status.colSpan, status.rowSpan),
        style = JarnTheme.type.body,
        color = colors.textHigh,
        maxLines = 1,
    )
    JarnText(
        text = stringResource(R.string.jarnfe_canvas_page_of, status.pageNumber, status.pageCount),
        style = JarnTheme.type.body,
        color = colors.textMid,
        maxLines = 1,
    )
}
