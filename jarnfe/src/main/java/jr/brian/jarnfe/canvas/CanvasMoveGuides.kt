package jr.brian.jarnfe.canvas

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import jr.brian.core.ui.components.JarnSurface
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R

private const val GUIDE_HALO_ALPHA = 0.5f
private const val GUIDE_LINE_ALPHA = 0.6f
private const val MOVE_CHIP_Z_INDEX = 3f
private val GuideHaloWidth = 3.dp
private val GuideLineWidth = 1.dp
private val HomeGhostHaloWidth = 4.dp
private val HomeGhostLineWidth = 2.dp
private val HomeGhostDash = 6.dp
private val MoveChipInset = 6.dp

/** Fades in once per grab, and shows at once under reduced motion. */
@Composable
private fun Modifier.canvasGuideFade(): Modifier {
    val motion = JarnTheme.motion
    val shown = remember { Animatable(if (motion.reduced) 1f else 0f) }
    LaunchedEffect(shown) { shown.animateTo(1f, motion.small.spec()) }
    return graphicsLayer { alpha = shown.value }
}

/** One cached path in its own layer, so a moving tile never redraws the cells. */
@Composable
internal fun BoxScope.CanvasGridCells(board: CanvasBoardGeometry, columns: Int, rows: Int) {
    val colors = JarnTheme.colors
    val shape = JarnTheme.tileShape
    Box(
        Modifier
            .matchParentSize()
            .canvasGuideFade()
            .drawWithCache {
                val cell = Size(board.cellPx, board.cellPx)
                val path = canvasCellsPath(shape.createOutline(cell, layoutDirection, this), board, columns, rows)
                val halo = Stroke(GuideHaloWidth.toPx())
                val line = Stroke(GuideLineWidth.toPx())
                onDrawBehind {
                    drawPath(path, colors.surface.copy(alpha = GUIDE_HALO_ALPHA), style = halo)
                    drawPath(path, colors.textHigh.copy(alpha = GUIDE_LINE_ALPHA), style = line)
                }
            },
    )
}

private fun canvasCellsPath(outline: Outline, board: CanvasBoardGeometry, columns: Int, rows: Int): Path {
    val cell = Path().apply { addOutline(outline) }
    return Path().apply {
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                addPath(cell, Offset(board.paddingPx + board.pitch * col, board.paddingPx + board.pitch * row))
            }
        }
    }
}

/** The spot the tile came from. Neutral, so it never reads as the accent landing spot. */
@Composable
internal fun CanvasHomeGhost() {
    val colors = JarnTheme.colors
    val shape = JarnTheme.tileShape
    Box(
        Modifier
            .fillMaxSize()
            .canvasGuideFade()
            .drawWithCache {
                val dash = HomeGhostDash.toPx()
                val effect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))
                val halo = Stroke(HomeGhostHaloWidth.toPx(), pathEffect = effect)
                val line = Stroke(HomeGhostLineWidth.toPx(), pathEffect = effect)
                val outline = shape.createOutline(size, layoutDirection, this)
                onDrawBehind {
                    drawOutline(outline, colors.surface.copy(alpha = GUIDE_HALO_ALPHA), style = halo)
                    drawOutline(outline, colors.textHigh, style = line)
                }
            },
    )
}

/** Follows the tile in its layer, so a finger move never recomposes or remeasures the chip. */
@Composable
internal fun CanvasMoveChip(
    grab: CanvasGrab,
    board: CanvasBoardGeometry,
    visual: CanvasDragVisual,
    boardWidth: Float,
    boardHeight: Float,
    showSize: Boolean,
) {
    val refusal = grab.refusal
    if (!showSize && refusal == null) return
    val rect = grab.rect
    val itemId = grab.itemId
    val inset = with(LocalDensity.current) { MoveChipInset.toPx() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        modifier = Modifier
            .zIndex(MOVE_CHIP_Z_INDEX)
            .graphicsLayer {
                val floating = visual.tileId == itemId
                val origin = canvasChipOrigin(
                    tile = if (floating) Offset(visual.x, visual.y) else board.originOf(rect),
                    tileWidth = board.extentOf(rect.colSpan),
                    tileHeight = board.extentOf(rect.rowSpan),
                    chipWidth = size.width,
                    chipHeight = size.height,
                    boardWidth = boardWidth,
                    boardHeight = boardHeight,
                    inset = inset,
                )
                translationX = origin.x
                translationY = origin.y
            },
    ) {
        if (showSize) {
            CanvasChipLabel(
                text = stringResource(R.string.jarnfe_canvas_size_readout, rect.colSpan, rect.rowSpan),
                background = JarnTheme.colors.accent,
                content = JarnTheme.colors.onAccent,
            )
        }
        refusal?.let { CanvasRefusalLabel(it) }
    }
}

@Composable
private fun CanvasRefusalLabel(refusal: CanvasMoveRefusal) {
    CanvasChipLabel(
        text = stringResource(refusal.label),
        background = JarnTheme.colors.danger,
        content = JarnTheme.colors.onDanger,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun CanvasChipLabel(text: String, background: Color, content: Color, modifier: Modifier = Modifier) {
    JarnSurface(modifier = modifier, color = background) {
        JarnText(
            text = text,
            style = JarnTheme.type.labelStrong,
            color = content,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = JarnTheme.space.sm, vertical = JarnTheme.space.xs),
        )
    }
}
