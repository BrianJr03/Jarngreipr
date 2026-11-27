package jr.brian.jarnfe.themekit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal const val THEME_STAR_COUNT = 5
private const val STAR_POINTS = 5
private const val STAR_INNER_RATIO = 0.42f
private const val STAR_EMPTY_ALPHA = 0.3f

/** [rating] from 0 to 1 as five stars, the filled part in [color] and the rest dimmed. */
@Composable
internal fun ThemeRatingStars(rating: Float, color: Color) {
    val filled = rating.coerceIn(0f, 1f)
    Canvas(Modifier.fillMaxHeight().aspectRatio(THEME_STAR_COUNT.toFloat())) {
        val stars = starRow()
        drawPath(stars, color.copy(alpha = color.alpha * STAR_EMPTY_ALPHA))
        clipRect(right = size.width * filled) { drawPath(stars, color) }
    }
}

private fun DrawScope.starRow(): Path {
    val cell = size.width / THEME_STAR_COUNT
    val outer = minOf(cell, size.height) / 2f
    return Path().apply {
        repeat(THEME_STAR_COUNT) { index -> addStar(Offset(cell * index + cell / 2f, size.height / 2f), outer) }
    }
}

private fun Path.addStar(center: Offset, outer: Float) {
    val inner = outer * STAR_INNER_RATIO
    val step = PI / STAR_POINTS
    for (point in 0 until STAR_POINTS * 2) {
        val radius = if (point % 2 == 0) outer else inner
        val angle = -PI / 2 + point * step
        val x = center.x + (radius * cos(angle)).toFloat()
        val y = center.y + (radius * sin(angle)).toFloat()
        if (point == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
