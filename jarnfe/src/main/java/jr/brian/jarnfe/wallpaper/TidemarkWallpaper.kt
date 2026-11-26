package jr.brian.jarnfe.wallpaper

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.LocalJarnScreen
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private const val TIDEMARK_FPS = 20
private const val TRANSPARENT_RGB_MASK = 0x00FFFFFF
private const val STROKE_WIDTH = 3.2f
private const val STROKE_MIN_PX = 1.2f
private const val NEAR_ALPHA = 0.7f
private const val FAR_ALPHA = 0.85f
private const val CULL_MARGIN = 90f
private const val BAND_TILT_PAD = 0.2f
private const val CROSS_REACH = 0.8f
private const val TRIANGLE_SIDE_X = 0.866f
private const val TRIANGLE_BASE_Y = 0.5f
private const val SQUARE_REACH = 0.78f
private const val CIRCLE_RADIUS = 0.9f

// A soft line at the middle of each swell length, so a crest sits at half the length.
private val BandStops = floatArrayOf(0f, 0.35f, 0.5f, 0.65f, 1f)
private val BandProfile = floatArrayOf(0f, 0.25f, 1f, 0.25f, 0f)

@Composable
internal fun TidemarkWallpaper(modifier: Modifier) {
    val colors = JarnTheme.colors
    val isBottomScreen = rememberJarnWallpaperIsBottomScreen()
    val hasDeckUi = LocalJarnScreen.current == JarnScreen.Deck
    val isMotionReduced = JarnTheme.motion.ambientReduced
    val canAnimate = rememberJarnWallpaperCanAnimate()
    val painter = remember(colors, isBottomScreen, hasDeckUi, isMotionReduced) {
        TidemarkPainter(colors.toJarnWallpaperPalette(), isBottomScreen, hasDeckUi, isMotionReduced)
    }
    Spacer(modifier.jarnWallpaperDraw(canAnimate, TIDEMARK_FPS, painter))
}

/** Holds all drawing state up front, so a frame allocates nothing. */
private class TidemarkPainter(
    private val palette: JarnWallpaperPalette,
    private val isBottomScreen: Boolean,
    hasDeckUi: Boolean,
    private val isMotionReduced: Boolean,
) : JarnWallpaperPainter {
    private val band = JarnWallpaperBand(palette, hasDeckUi)
    private val bandPaints = Array(TidemarkScene.BandLength.size) { bandPaint(it) }
    private val shapePaths = Array(TidemarkScene.KIND_COUNT) { shapePath(it) }
    private val nearPaint = strokePaint(palette.glow, NEAR_ALPHA)
    private val farPaint = strokePaint(palette.midGlow, FAR_ALPHA)

    override fun DrawScope.paint(timeNanos: Long) {
        val height = size.height
        if (height <= 0f || size.width <= 0f) return
        val canvas = drawContext.canvas.nativeCanvas
        val isStill = isMotionReduced || timeNanos == 0L
        val seconds = tidemarkSeconds(timeNanos, isStill)
        val scale = jarnWallpaperScale(height)
        val halfWidth = jarnWallpaperHalfWidth(size.width, height)
        val top = jarnWallpaperOriginY(isBottomScreen)
        val bottom = top + JARN_WALLPAPER_SCREEN_HEIGHT

        canvas.drawColor(palette.bg)
        val checkpoint = canvas.save()
        canvas.translate(size.width / 2f, -top * scale)
        canvas.scale(scale, scale)
        for (i in bandPaints.indices) {
            drawSwell(canvas, i, tidemarkSwellOffset(seconds, i), halfWidth, top, bottom)
        }
        drawShapes(canvas, tidemarkSwellOffset(seconds, 0), halfWidth, top, bottom, 1f / scale)
        canvas.restoreToCount(checkpoint)
        band.draw(canvas, size.width, height)
    }

    /** Shifts the canvas instead of the shader, so the gradient is never rebuilt. */
    private fun drawSwell(canvas: Canvas, index: Int, offset: Float, halfWidth: Float, top: Float, bottom: Float) {
        val pad = halfWidth * BAND_TILT_PAD + TidemarkScene.BandLength[index]
        val checkpoint = canvas.save()
        canvas.rotate(TidemarkScene.BandTiltDegrees[index])
        canvas.translate(0f, offset)
        canvas.drawRect(-halfWidth - pad, top - pad - offset, halfWidth + pad, bottom + pad - offset, bandPaints[index])
        canvas.restoreToCount(checkpoint)
    }

    private fun drawShapes(canvas: Canvas, offset: Float, halfWidth: Float, top: Float, bottom: Float, pixel: Float) {
        val shapes = TidemarkScene.Shapes
        val stroke = max(STROKE_WIDTH, STROKE_MIN_PX * pixel)
        for (i in 0 until TidemarkScene.SHAPE_COUNT) {
            val at = i * TidemarkScene.SHAPE_STRIDE
            val x = shapes[at + TidemarkScene.SHAPE_X]
            val y = shapes[at + TidemarkScene.SHAPE_Y]
            if (y < top - CULL_MARGIN || y > bottom + CULL_MARGIN || abs(x) > halfWidth + CULL_MARGIN) continue
            val size = shapes[at + TidemarkScene.SHAPE_SIZE]
            val angle = tidemarkWaveAngle(x, y, offset)
            val paint = if (shapes[at + TidemarkScene.SHAPE_TONE] > 0f) nearPaint else farPaint
            paint.strokeWidth = stroke / size
            val checkpoint = canvas.save()
            canvas.translate(x, y + tidemarkLift(angle))
            canvas.rotate(tidemarkTurn(angle, shapes[at + TidemarkScene.SHAPE_TURN]))
            canvas.scale(size, size * tidemarkSquash(angle))
            canvas.drawPath(shapePaths[TidemarkScene.Kinds[i]], paint)
            canvas.restoreToCount(checkpoint)
        }
    }

    private fun bandPaint(index: Int) = Paint().apply {
        isDither = true
        val tone = if (TidemarkScene.BandUsesGlow[index]) palette.glow else palette.midGlow
        val peak = TidemarkScene.BandAlpha[index]
        val colors = IntArray(BandProfile.size) { withAlpha(tone, BandProfile[it] * peak) }
        shader = LinearGradient(
            0f,
            0f,
            0f,
            TidemarkScene.BandLength[index],
            colors,
            BandStops,
            Shader.TileMode.REPEAT,
        )
    }

    private fun strokePaint(color: Int, alpha: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = withAlpha(color, alpha)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    /** Unit shapes centered on the origin, scaled to each shape's size when drawn. */
    private fun shapePath(kind: Int) = Path().apply {
        when (kind) {
            TidemarkScene.KIND_CROSS -> {
                moveTo(-CROSS_REACH, -CROSS_REACH)
                lineTo(CROSS_REACH, CROSS_REACH)
                moveTo(-CROSS_REACH, CROSS_REACH)
                lineTo(CROSS_REACH, -CROSS_REACH)
            }
            TidemarkScene.KIND_TRIANGLE -> {
                moveTo(0f, -1f)
                lineTo(TRIANGLE_SIDE_X, TRIANGLE_BASE_Y)
                lineTo(-TRIANGLE_SIDE_X, TRIANGLE_BASE_Y)
                close()
            }
            TidemarkScene.KIND_SQUARE ->
                addRect(-SQUARE_REACH, -SQUARE_REACH, SQUARE_REACH, SQUARE_REACH, Path.Direction.CW)
            else -> addCircle(0f, 0f, CIRCLE_RADIUS, Path.Direction.CW)
        }
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        (color and TRANSPARENT_RGB_MASK) or ((alpha * 255).roundToInt().coerceIn(0, 255) shl 24)
}
