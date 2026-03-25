package jr.brian.home.ui.colors

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.animatedGradientBorder(
    colors: List<Color>,
    shape: Shape,
    borderWidth: Dp = 2.dp,
    durationMs: Int = 2000
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "gradientBorder")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "borderAngle"
    )
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    drawWithContent {
        drawContent()
        val strokePx = borderWidth.toPx()
        val outline = shape.createOutline(size, layoutDirection, density)
        val safeColors = if (colors.size < 2) colors + colors else colors
        val argbColors = safeColors.map { it.toArgb() }.toIntArray()
        val brush = object : ShaderBrush() {
            override fun createShader(size: Size): android.graphics.Shader {
                val shader = android.graphics.SweepGradient(
                    size.width / 2f,
                    size.height / 2f,
                    argbColors,
                    null
                )
                android.graphics.Matrix().apply {
                    postRotate(angle, size.width / 2f, size.height / 2f)
                    shader.setLocalMatrix(this)
                }
                return shader
            }
        }
        drawOutline(
            outline = outline,
            brush = brush,
            style = Stroke(width = strokePx)
        )
    }
}
