package jr.brian.jarnfe.themekit

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import jr.brian.core.model.theme.ThemeAlign
import jr.brian.core.model.theme.ThemeColorRole
import jr.brian.core.model.theme.ThemeCorner
import jr.brian.core.model.theme.ThemeEdge
import jr.brian.core.model.theme.ThemeFade
import jr.brian.core.model.theme.ThemeFit
import jr.brian.core.model.theme.ThemeTextRole
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.JarnTheme

/** Roles read the repaired palette, so a layer recolors with the theme and keeps Jarn's contrast. */
internal fun ThemeColorRole.color(colors: JarnColors): Color = when (this) {
    ThemeColorRole.Bg -> colors.bg
    ThemeColorRole.Surface -> colors.surface
    ThemeColorRole.Raised -> colors.raised
    ThemeColorRole.Line -> colors.line
    ThemeColorRole.TextHigh -> colors.textHigh
    ThemeColorRole.TextMid -> colors.textMid
    ThemeColorRole.TextLow -> colors.textLow
    ThemeColorRole.Accent -> colors.accent
    ThemeColorRole.Danger -> colors.danger
    ThemeColorRole.Success -> colors.success
}

/**
 * Text color on the readable plate. Tonal roles are backgrounds, not text, so they fall back to
 * high text rather than vanish into the plate.
 */
internal fun ThemeColorRole.textColor(colors: JarnColors): Color = when (this) {
    ThemeColorRole.Bg, ThemeColorRole.Surface, ThemeColorRole.Raised, ThemeColorRole.Line -> colors.textHigh
    else -> color(colors)
}

@Composable
internal fun ThemeTextRole.style(): TextStyle {
    val type = JarnTheme.type
    return when (this) {
        ThemeTextRole.Display -> type.display
        ThemeTextRole.Title -> type.title
        ThemeTextRole.Heading -> type.heading
        ThemeTextRole.Body -> type.body
        ThemeTextRole.Label -> type.label
        ThemeTextRole.LabelStrong -> type.labelStrong
        ThemeTextRole.Caption -> type.caption
        ThemeTextRole.CaptionStrong -> type.captionStrong
    }
}

@Composable
internal fun ThemeCorner.shape(): Shape {
    val radius = JarnTheme.radius
    return when (this) {
        ThemeCorner.None -> RectangleShape
        ThemeCorner.Sm -> radius.smShape
        ThemeCorner.Md -> radius.mdShape
        ThemeCorner.Lg -> radius.lgShape
        ThemeCorner.Xl -> radius.xlShape
    }
}

internal val ThemeAlign.textAlign: TextAlign
    get() = when (this) {
        ThemeAlign.Start -> TextAlign.Start
        ThemeAlign.Center -> TextAlign.Center
        ThemeAlign.End -> TextAlign.End
    }

/** Where the plate sits in the layer's rect: [align] across, [verticalAlign] top (start) to bottom (end). */
internal fun plateAlignment(align: ThemeAlign, verticalAlign: ThemeAlign): Alignment = BiasAlignment(align.bias, verticalAlign.bias)

private val ThemeAlign.bias: Float
    get() = when (this) {
        ThemeAlign.Start -> -1f
        ThemeAlign.Center -> 0f
        ThemeAlign.End -> 1f
    }

internal val ThemeFit.contentScale: ContentScale
    get() = when (this) {
        ThemeFit.Fit -> ContentScale.Fit
        ThemeFit.Crop -> ContentScale.Crop
        ThemeFit.Fill -> ContentScale.FillBounds
    }

/**
 * The fill for a fading panel: solid [color] that fades to transparent over the [fade] length at
 * its edge. Start and end follow [direction].
 */
internal fun themeFadeBrush(color: Color, fade: ThemeFade, direction: LayoutDirection): Brush {
    val stops = themeFadeStops(color, fade.edge.resolved(direction), fade.length)
    return when (fade.edge.resolved(direction)) {
        ThemeEdge.Top, ThemeEdge.Bottom -> Brush.verticalGradient(*stops)
        ThemeEdge.Start, ThemeEdge.End -> Brush.horizontalGradient(*stops)
    }
}

/**
 * Gradient stops from the top or left: transparent at a [ThemeEdge.Top] or [ThemeEdge.Start]
 * (left) edge, solid from [length] in, and the mirror for the other edges.
 */
internal fun themeFadeStops(color: Color, edge: ThemeEdge, length: Float): Array<Pair<Float, Color>> {
    val span = length.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: ThemeFade.DEFAULT_LENGTH
    val clear = color.copy(alpha = 0f)
    return when (edge) {
        ThemeEdge.Top, ThemeEdge.Start -> arrayOf(0f to clear, span to color, 1f to color)
        ThemeEdge.Bottom, ThemeEdge.End -> arrayOf(0f to color, 1f - span to color, 1f to clear)
    }
}

/** The edge as drawn: in right-to-left text start is the right side, so after this Start is always left. */
private fun ThemeEdge.resolved(direction: LayoutDirection): ThemeEdge = when {
    direction == LayoutDirection.Ltr -> this
    this == ThemeEdge.Start -> ThemeEdge.End
    this == ThemeEdge.End -> ThemeEdge.Start
    else -> this
}
