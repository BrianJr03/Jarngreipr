package jr.brian.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme

private const val STATIC_SPINNER_PROGRESS = 0.75f

/** A shadow for every [JarnText] below that has none of its own, so text over busy art stays legible. */
val LocalJarnTextShadow = staticCompositionLocalOf<Shadow?> { null }

@Composable
fun JarnText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign = TextAlign.Unspecified,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color, textAlign = textAlign, shadow = style.shadow ?: LocalJarnTextShadow.current),
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines,
    )
}

@Composable
fun JarnSurface(
    modifier: Modifier = Modifier,
    color: Color = JarnTheme.colors.surface,
    shape: Shape = JarnTheme.radius.mdShape,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.background(color, shape), content = content)
}

enum class JarnScrimEdge { Left, Bottom, Full }

@Composable
fun JarnScrim(edge: JarnScrimEdge, modifier: Modifier = Modifier) {
    val solid = JarnTheme.colors.artScrim
    val clear = solid.copy(alpha = 0f)
    val brush = when (edge) {
        JarnScrimEdge.Left -> Brush.horizontalGradient(0f to solid, 0.55f to solid, 1f to clear)
        JarnScrimEdge.Bottom -> Brush.verticalGradient(0f to clear, 0.45f to solid, 1f to solid)
        JarnScrimEdge.Full -> Brush.linearGradient(listOf(solid, solid))
    }
    Box(modifier.background(brush))
}

@Composable
internal fun JarnSpinner(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    strokeWidth: Dp = 2.dp,
) {
    val sized = modifier.size(size)
    if (JarnTheme.motion.reduced) {
        CircularProgressIndicator(
            progress = { STATIC_SPINNER_PROGRESS },
            modifier = sized,
            color = color,
            trackColor = Color.Transparent,
            strokeWidth = strokeWidth,
        )
    } else {
        CircularProgressIndicator(modifier = sized, color = color, strokeWidth = strokeWidth)
    }
}

@Composable
internal fun JarnCheckGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Canvas(modifier.size(size)) {
        val path = Path().apply {
            moveTo(this@Canvas.size.width * 0.18f, this@Canvas.size.height * 0.54f)
            lineTo(this@Canvas.size.width * 0.42f, this@Canvas.size.height * 0.76f)
            lineTo(this@Canvas.size.width * 0.84f, this@Canvas.size.height * 0.26f)
        }
        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
internal fun JarnChevronGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    val mirrored = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        fun point(x: Float, y: Float) = Offset(if (mirrored) w - w * x else w * x, h * y)
        val top = point(0.36f, 0.2f)
        val tip = point(0.66f, 0.5f)
        val bottom = point(0.36f, 0.8f)
        val path = Path().apply {
            moveTo(top.x, top.y)
            lineTo(tip.x, tip.y)
            lineTo(bottom.x, bottom.y)
        }
        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
