package jr.brian.jarnfe.wallpaper

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
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

private const val HEARTHRISE_FPS = 20
private const val TRANSPARENT_RGB_MASK = 0x00FFFFFF
private const val OFF_SCREEN = -1

@Composable
internal fun HearthriseWallpaper(modifier: Modifier) {
    val colors = JarnTheme.colors
    val isBottomScreen = rememberJarnWallpaperIsBottomScreen()
    val hasDeckUi = LocalJarnScreen.current == JarnScreen.Deck
    val isMotionReduced = JarnTheme.motion.ambientReduced
    val canAnimate = rememberJarnWallpaperCanAnimate()
    val painter = remember(colors, isBottomScreen, hasDeckUi, isMotionReduced) {
        HearthrisePainter(colors.toJarnWallpaperPalette(), isBottomScreen, hasDeckUi, isMotionReduced)
    }
    Spacer(modifier.jarnWallpaperDraw(canAnimate, HEARTHRISE_FPS, painter))
}

/** Holds all drawing state up front, so a frame allocates nothing. */
private class HearthrisePainter(
    private val palette: JarnWallpaperPalette,
    private val isBottomScreen: Boolean,
    hasDeckUi: Boolean,
    private val isMotionReduced: Boolean,
) : JarnWallpaperPainter {
    private val band = JarnWallpaperBand(palette, hasDeckUi)
    private val levels = IntArray(HearthriseScene.EMBER_COUNT)
    private val emberX = FloatArray(HearthriseScene.EMBER_COUNT)
    private val emberY = FloatArray(HearthriseScene.EMBER_COUNT)
    private val emberRadius = FloatArray(HearthriseScene.EMBER_COUNT)
    private val seamPath = buildSeamPath()
    private val coalPaints = Array(HearthriseScene.COAL_COUNT) { coalPaint(HearthriseScene.CoalX[it]) }
    private val emberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = palette.line
        style = Paint.Style.FILL
    }
    private val seamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = palette.line
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }

    override fun DrawScope.paint(timeNanos: Long) {
        val height = size.height
        if (height <= 0f || size.width <= 0f) return
        val canvas = drawContext.canvas.nativeCanvas
        val isStill = isMotionReduced || timeNanos == 0L
        val seconds = hearthriseSeconds(timeNanos, isStill)
        val scale = jarnWallpaperScale(height)
        val halfWidth = jarnWallpaperHalfWidth(size.width, height)
        val top = jarnWallpaperOriginY(isBottomScreen)

        canvas.drawColor(palette.bg)
        val checkpoint = canvas.save()
        canvas.translate(size.width / 2f, -top * scale)
        canvas.scale(scale, scale)
        if (isBottomScreen) {
            drawCoals(canvas, seconds, halfWidth)
            drawSeams(canvas, seconds, 1f / scale)
        }
        drawEmbers(canvas, seconds, top, halfWidth, 1f / scale)
        canvas.restoreToCount(checkpoint)
        band.draw(canvas, size.width, height)
    }

    private fun drawCoals(canvas: Canvas, seconds: Double, halfWidth: Float) {
        for (i in coalPaints.indices) {
            val paint = coalPaints[i]
            paint.alpha = alphaOf(hearthriseCoalAlpha(seconds, HearthriseScene.CoalPhase[i]))
            canvas.drawRect(-halfWidth, HearthriseScene.COAL_TOP, halfWidth, HearthriseScene.SCENE_BOTTOM, paint)
        }
    }

    private fun drawSeams(canvas: Canvas, seconds: Double, pixel: Float) {
        seamPaint.strokeWidth = max(HearthriseScene.SEAM_MIN_WIDTH, pixel)
        seamPaint.alpha = alphaOf(hearthriseSeamAlpha(seconds))
        canvas.drawPath(seamPath, seamPaint)
    }

    private fun drawEmbers(canvas: Canvas, seconds: Double, top: Float, halfWidth: Float, pixel: Float) {
        placeEmbers(seconds, top, halfWidth, pixel)
        for (level in 0 until HearthriseScene.LEVEL_COUNT) {
            emberPaint.alpha = alphaOf(HearthriseScene.LevelAlphas[level])
            for (i in levels.indices) {
                if (levels[i] == level) canvas.drawCircle(emberX[i], emberY[i], emberRadius[i], emberPaint)
            }
        }
    }

    private fun placeEmbers(seconds: Double, top: Float, halfWidth: Float, pixel: Float) {
        val embers = HearthriseScene.Embers
        for (i in levels.indices) {
            val at = i * HearthriseScene.EMBER_STRIDE
            val phase = embers[at + HearthriseScene.EMBER_PHASE]
            val travel = hearthriseTravel(seconds, embers[at + HearthriseScene.EMBER_SPEED], phase)
            val life = hearthriseLife(travel)
            val y = hearthriseEmberY(travel)
            val x = hearthriseEmberX(seconds, embers[at + HearthriseScene.EMBER_X], phase)
            val isVisible = hearthriseEmberIsOnScreen(y, top) && abs(x) <= halfWidth
            levels[i] = if (isVisible) hearthriseEmberLevel(life) else OFF_SCREEN
            emberX[i] = x
            emberY[i] = y
            emberRadius[i] = hearthriseEmberRadius(embers[at + HearthriseScene.EMBER_SIZE], life, pixel)
        }
    }

    private fun coalPaint(centerX: Float) = Paint().apply {
        isDither = true
        shader = RadialGradient(
            centerX,
            HearthriseScene.COAL_CENTER_Y,
            HearthriseScene.COAL_OUTER_RADIUS,
            intArrayOf(palette.glow, palette.midGlow, palette.midGlow and TRANSPARENT_RGB_MASK),
            hearthriseCoalStops(),
            Shader.TileMode.CLAMP,
        )
    }

    private fun buildSeamPath() = Path().apply {
        val seams = HearthriseScene.Seams
        for (seam in 0 until HearthriseScene.SEAM_COUNT) {
            val start = seam * HearthriseScene.SEAM_POINTS * 2
            moveTo(seams[start], seams[start + 1])
            for (point in 1 until HearthriseScene.SEAM_POINTS) {
                lineTo(seams[start + point * 2], seams[start + point * 2 + 1])
            }
        }
    }

    private fun alphaOf(fraction: Float): Int = (fraction * 255).roundToInt().coerceIn(0, 255)
}
