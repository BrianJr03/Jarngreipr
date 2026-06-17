package jr.brian.home.ui.components.apps

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import jr.brian.home.data.SNAP_GRID_STEP

@Composable
fun SnapGridOverlay(
    borderPaddingPx: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val stepPx = with(density) { SNAP_GRID_STEP.toPx() }
    val strokePx = with(density) { 1.dp.toPx() }
    val lineColor = Color.White.copy(alpha = 0.28f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (stepPx <= 0f || width <= 0f || height <= 0f) return@Canvas

        var x = borderPaddingPx
        while (x <= width) {
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = strokePx
            )
            x += stepPx
        }

        var y = borderPaddingPx
        while (y <= height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = strokePx
            )
            y += stepPx
        }
    }
}
