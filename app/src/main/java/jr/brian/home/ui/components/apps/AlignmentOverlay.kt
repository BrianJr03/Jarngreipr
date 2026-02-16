package jr.brian.home.ui.components.apps

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import jr.brian.home.model.DistanceMeasurement
import jr.brian.home.model.GuideType
import jr.brian.home.model.alignment.AlignmentState
import jr.brian.home.ui.theme.AlignmentGuideColor

/**
 * Renders alignment guides and distance measurements overlay during drag operations.
 *
 * @param alignmentState The current alignment state with guides and distance measurements
 * @param modifier The modifier for the Canvas
 */
@Composable
fun AlignmentOverlay(
    alignmentState: AlignmentState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Draw alignment guides
        alignmentState.guides.forEach { guide ->
            when (guide.type) {
                GuideType.VERTICAL -> {
                    drawLine(
                        color = AlignmentGuideColor,
                        start = Offset(guide.position, 0f),
                        end = Offset(guide.position, size.height),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(10f, 10f),
                            0f
                        )
                    )
                }

                GuideType.HORIZONTAL -> {
                    drawLine(
                        color = AlignmentGuideColor,
                        start = Offset(0f, guide.position),
                        end = Offset(size.width, guide.position),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(10f, 10f),
                            0f
                        )
                    )
                }
            }
        }

        // Draw distance measurements
        alignmentState.distances.forEach { measurement ->
            drawDistanceMeasurement(measurement)
        }
    }
}

/**
 * Draws a single distance measurement with line, caps, and text label.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDistanceMeasurement(
    measurement: DistanceMeasurement
) {
    val distanceColor = Color(0xFFFF9800)
    val textColor = Color.White
    val capSize = 8f

    // Draw main measurement line
    drawLine(
        color = distanceColor,
        start = Offset(measurement.startX, measurement.startY),
        end = Offset(measurement.endX, measurement.endY),
        strokeWidth = 2f
    )

    // Draw end caps
    if (measurement.isHorizontal) {
        // Vertical caps for horizontal measurements
        drawLine(
            color = distanceColor,
            start = Offset(measurement.startX, measurement.startY - capSize),
            end = Offset(measurement.startX, measurement.startY + capSize),
            strokeWidth = 2f
        )
        drawLine(
            color = distanceColor,
            start = Offset(measurement.endX, measurement.endY - capSize),
            end = Offset(measurement.endX, measurement.endY + capSize),
            strokeWidth = 2f
        )
    } else {
        // Horizontal caps for vertical measurements
        drawLine(
            color = distanceColor,
            start = Offset(measurement.startX - capSize, measurement.startY),
            end = Offset(measurement.startX + capSize, measurement.startY),
            strokeWidth = 2f
        )
        drawLine(
            color = distanceColor,
            start = Offset(measurement.endX - capSize, measurement.endY),
            end = Offset(measurement.endX + capSize, measurement.endY),
            strokeWidth = 2f
        )
    }

    // Draw distance text with background
    val distanceText = "${measurement.distance.toInt()}dp"
    val textX = (measurement.startX + measurement.endX) / 2
    val textY = (measurement.startY + measurement.endY) / 2

    drawIntoCanvas { canvas ->
        val paint = Paint().asFrameworkPaint().apply {
            color = textColor.toArgb()
            textSize = 32f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            setShadowLayer(4f, 0f, 0f, Color.Black.toArgb())
        }

        // Draw background rectangle for better readability
        val textBounds = android.graphics.Rect()
        paint.getTextBounds(distanceText, 0, distanceText.length, textBounds)
        val padding = 8f

        val bgPaint = android.graphics.Paint().apply {
            color = Color(0xCC000000).toArgb() // Semi-transparent black
            style = android.graphics.Paint.Style.FILL
        }

        canvas.nativeCanvas.drawRoundRect(
            textX - textBounds.width() / 2 - padding,
            textY + textBounds.top - padding,
            textX + textBounds.width() / 2 + padding,
            textY + textBounds.bottom + padding,
            8f,
            8f,
            bgPaint
        )

        canvas.nativeCanvas.drawText(
            distanceText,
            textX,
            textY,
            paint
        )
    }
}
