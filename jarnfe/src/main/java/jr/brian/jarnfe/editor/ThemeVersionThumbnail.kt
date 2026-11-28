package jr.brian.jarnfe.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.themes.toReadableJarnColors

private const val STAGE_ASPECT = 16f / 9f
private const val TEXT_BAR_HEIGHT = 0.45f
private const val PIECE_ALPHA = 0.85f

/**
 * A small picture of [spec]'s [view], drawn from the theme itself so every saved version has one
 * without storing images: the list's box, then each piece as its kind's shape, in the theme's own
 * colors (or the Look's, when it follows the Look).
 */
@Composable
internal fun ThemeVersionThumbnail(spec: JarnThemeSpec, view: ThemeView, modifier: Modifier = Modifier) {
    val colors = spec.tokens.colors.takeUnless { spec.meta.followsLook }?.toReadableJarnColors() ?: JarnTheme.colors
    val viewSpec = spec.views[view]
    Canvas(modifier.aspectRatio(STAGE_ASPECT).clip(JarnTheme.radius.smShape)) {
        drawRect(colors.bg)
        if (viewSpec == null) return@Canvas
        val pieces = viewSpec.layers.sortedBy { it.z }
        pieces.filter { it.z <= 0 }.forEach { drawLayer(it, colors) }
        drawBox(viewSpec.primary.box, colors.surface, fill = true)
        drawBox(viewSpec.primary.box, colors.accent, fill = false)
        pieces.filter { it.z > 0 }.forEach { drawLayer(it, colors) }
    }
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
