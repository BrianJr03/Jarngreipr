package jr.brian.jarnfe.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.model.theme.ViewSpec
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.themes.toReadableJarnColors

private const val STAGE_ASPECT = 16f / 9f
private const val TEXT_BAR_HEIGHT = 0.45f
private const val PIECE_ALPHA = 0.85f
private const val ITEM_GAP = 0.02f
private const val MIN_ASPECT = 0.1f
private const val SHELF_FOCUSED_WIDTH = 0.3f
private const val SHELF_FOCUSED_HEIGHT = 0.8f

/**
 * A small picture of [spec]'s [view], drawn from the theme itself so every saved version has one
 * without storing images: the list as its items (tiles, a shelf or rows), then each piece as its
 * kind's shape, in the theme's own colors (or the Look's, when it follows the Look). The list is
 * drawn as items rather than its box, since a full screen list's box is the whole picture.
 */
@Composable
internal fun ThemeVersionThumbnail(spec: JarnThemeSpec, view: ThemeView, modifier: Modifier = Modifier) {
    val colors = spec.tokens.colors.takeUnless { spec.meta.followsLook }?.toReadableJarnColors() ?: JarnTheme.colors
    Canvas(modifier.aspectRatio(STAGE_ASPECT).clip(JarnTheme.radius.smShape)) {
        drawThemeThumbnail(spec.views[view], colors)
    }
}

/** The picture itself: background, pieces below the list, the list's items, then pieces above. */
internal fun DrawScope.drawThemeThumbnail(viewSpec: ViewSpec?, colors: JarnColors) {
    drawRect(colors.bg)
    if (viewSpec == null) return
    val pieces = viewSpec.layers.sortedBy { it.z }
    pieces.filter { it.z <= 0 }.forEach { drawLayer(it, colors) }
    drawPrimary(viewSpec.primary, colors)
    pieces.filter { it.z > 0 }.forEach { drawLayer(it, colors) }
}

/** The list as its items: tiles in a grid, a row with a larger center for a shelf, bars for a list. */
private fun DrawScope.drawPrimary(primary: ThemePrimary, colors: JarnColors) {
    val area = primary.box.pixelRect(size)
    when (primary) {
        is ThemePrimary.Grid -> drawGridItems(area, primary.columns.coerceAtLeast(1), primary.itemAspect, colors)
        is ThemePrimary.Shelf -> drawShelfItems(area, primary.itemsBefore, primary.itemsAfter, primary.focusedScale, colors)
        is ThemePrimary.List -> drawListRows(area, primary.rows.coerceAtLeast(1), colors)
    }
}

private fun DrawScope.drawGridItems(area: Rect, columns: Int, itemAspect: Float, colors: JarnColors) {
    val gap = area.width * ITEM_GAP
    val cellWidth = (area.width - gap * (columns - 1)) / columns
    val cellHeight = cellWidth / itemAspect.coerceAtLeast(MIN_ASPECT)
    var top = area.top
    var row = 0
    while (top + cellHeight <= area.bottom + 1f && cellHeight > 0f) {
        repeat(columns) { column ->
            val left = area.left + column * (cellWidth + gap)
            drawItem(Rect(left, top, left + cellWidth, top + cellHeight), colors, focused = row == 0 && column == 0)
        }
        top += cellHeight + gap
        row += 1
    }
}

private fun DrawScope.drawShelfItems(area: Rect, before: Int, after: Int, focusedScale: Float, colors: JarnColors) {
    val gap = area.width * ITEM_GAP
    val focusedWidth = area.width * SHELF_FOCUSED_WIDTH
    val focusedHeight = area.height * SHELF_FOCUSED_HEIGHT
    val sideWidth = focusedWidth / focusedScale.coerceAtLeast(1f)
    val sideHeight = focusedHeight / focusedScale.coerceAtLeast(1f)
    val centerX = area.center.x
    val centerY = area.center.y
    drawItem(Rect(centerX - focusedWidth / 2, centerY - focusedHeight / 2, centerX + focusedWidth / 2, centerY + focusedHeight / 2), colors, focused = true)
    fun side(index: Int, direction: Int) {
        val offset = focusedWidth / 2 + gap + index * (sideWidth + gap)
        val left = if (direction > 0) centerX + offset else centerX - offset - sideWidth
        val item = Rect(left, centerY - sideHeight / 2, left + sideWidth, centerY + sideHeight / 2)
        if (item.left >= area.left - 1f && item.right <= area.right + 1f) drawItem(item, colors, focused = false)
    }
    repeat(before) { side(it, -1) }
    repeat(after) { side(it, 1) }
}

private fun DrawScope.drawListRows(area: Rect, rows: Int, colors: JarnColors) {
    val gap = area.height * ITEM_GAP
    val rowHeight = (area.height - gap * (rows - 1)) / rows
    repeat(rows) { row ->
        val top = area.top + row * (rowHeight + gap)
        drawItem(Rect(area.left, top, area.right, top + rowHeight), colors, focused = row == 0)
    }
}

private fun DrawScope.drawItem(rect: Rect, colors: JarnColors, focused: Boolean) {
    val corner = CornerRadius(2.dp.toPx())
    drawRoundRect(if (focused) colors.accent else colors.raised, rect.topLeft, rect.size, corner)
}

private fun ThemeRect.pixelRect(size: Size): Rect {
    val edges = edges
    return Rect(edges.left * size.width, edges.top * size.height, edges.right * size.width, edges.bottom * size.height)
}

private fun DrawScope.drawLayer(layer: ThemeLayer, colors: JarnColors) {
    when (layer) {
        is ThemeLayer.Picture -> drawBox(layer.rect, colors.raised, fill = true)
        is ThemeLayer.Video -> {
            drawBox(layer.rect, colors.raised, fill = true)
            drawBox(layer.rect, colors.accent, fill = false)
        }
        is ThemeLayer.Panel -> drawBox(layer.rect, colors.surface, fill = true)
        is ThemeLayer.Text, is ThemeLayer.Date -> drawBox(layer.rect.textBar(), colors.textMid, fill = true)
        is ThemeLayer.Rating -> drawBox(layer.rect.textBar(), colors.accent, fill = true)
    }
}

/** Text reads as a bar across the middle of its rect, like a line of words. */
private fun ThemeRect.textBar(): ThemeRect {
    val edges = edges
    val barHeight = edges.height * TEXT_BAR_HEIGHT
    return ThemeRect(edges.left, edges.top + (edges.height - barHeight) / 2, edges.width, barHeight)
}

private fun DrawScope.drawBox(rect: ThemeRect, color: Color, fill: Boolean) {
    val edges = rect.edges
    val topLeft = Offset(edges.left * size.width, edges.top * size.height)
    val boxSize = Size(edges.width * size.width, edges.height * size.height)
    val corner = CornerRadius(2.dp.toPx())
    if (fill) {
        drawRoundRect(color.copy(alpha = color.alpha * PIECE_ALPHA), topLeft, boxSize, corner)
    } else {
        drawRoundRect(color, topLeft, boxSize, corner, style = Stroke(1.dp.toPx()))
    }
}
