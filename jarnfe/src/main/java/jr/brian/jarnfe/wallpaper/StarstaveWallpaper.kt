package jr.brian.jarnfe.wallpaper

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import jr.brian.core.model.PRIMARY_DISPLAY_ID
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.LocalJarnScreen
import jr.brian.jarnfe.session.JarnSession
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

private const val STARSTAVE_FPS = 12
private const val MIN_STAR_PX = 1.2f
private const val SIGN_WIDTH = 3f
private const val SIGN_MIN_PX = 1.2f
private const val NODE_RADIUS = 11f
private const val DIAL_WIDTH = 2.2f
private const val DIAL_RING_ALPHA = 0.55f
private const val DIAL_TICK_ALPHA = 0.7f
private const val NEEDLE_TIP_RADIUS = 9f
private const val COORDS_PER_LINE = 4

/** The physical screen picks the scene half. The role picks the bands, which protect its UI. */
@Composable
internal fun StarstaveWallpaper(modifier: Modifier) {
    val colors = JarnTheme.colors
    val showsDial = rememberStarstaveShowsDial()
    val hasDeckUi = LocalJarnScreen.current == JarnScreen.Deck
    val isMotionReduced = JarnTheme.motion.reduced
    val canAnimate = rememberJarnWallpaperCanAnimate()
    val painter = remember(colors, showsDial, hasDeckUi, isMotionReduced) {
        StarstavePainter(colors.toStarstavePalette(), showsDial, hasDeckUi, isMotionReduced)
    }
    Spacer(modifier.jarnWallpaperDraw(canAnimate, STARSTAVE_FPS, painter))
}

@Composable
private fun rememberStarstaveShowsDial(): Boolean {
    val windowDisplayId = LocalView.current.display?.displayId
    val hasBothScreens by remember {
        derivedStateOf {
            val state = JarnSession.state
            state.deckDisplayAvailable && state.deckHostReady
        }
    }
    return starstaveShowsDial(windowDisplayId, PRIMARY_DISPLAY_ID, hasBothScreens)
}

private fun JarnColors.toStarstavePalette(): StarstavePalette = starstavePalette(
    bg = bg.toArgb(),
    accent = accent.toArgb(),
    focus = focus.toArgb(),
    textMid = textMid.toArgb(),
    textLow = textLow.toArgb(),
)

/** Holds all drawing state up front, so a frame allocates nothing. */
private class StarstavePainter(
    private val palette: StarstavePalette,
    private val showsDial: Boolean,
    hasDeckUi: Boolean,
    private val isMotionReduced: Boolean,
) : JarnWallpaperPainter {
    private val levels = IntArray(StarstaveScene.STAR_COUNT)
    private val tickLines = FloatArray(StarstaveScene.DIAL_TICK_COUNT * COORDS_PER_LINE)
    private val signPath = Path()
    private val bandStops = starstaveBandStops(hasDeckUi)
    private val bandColors = starstaveBandColors(palette, hasDeckUi)
    private val starPaint = fillPaint(palette.glow)
    private val dotPaint = fillPaint(palette.line)
    private val bandPaint = Paint()
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = palette.line
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private var bandHeight = -1f

    override fun DrawScope.paint(timeNanos: Long) {
        val height = size.height
        if (height <= 0f || size.width <= 0f) return
        val canvas = drawContext.canvas.nativeCanvas
        val isStill = isMotionReduced || timeNanos == 0L
        val seconds = starstaveSeconds(timeNanos, isStill)
        val cycle = starstaveCycle(seconds)
        val reach = (starstaveDrawProgress(cycle, isStill) * StarstaveScene.TotalReach).toFloat()
        val fade = starstaveFade(cycle, isStill).toFloat()
        val scale = starstaveScale(height)

        canvas.drawColor(palette.bg)
        val checkpoint = canvas.save()
        canvas.translate(size.width / 2f, -starstaveOriginY(showsDial) * scale)
        canvas.scale(scale, scale)
        drawStars(canvas, seconds, starstaveHalfWidth(size.width, height), 1f / scale)
        if (showsDial) {
            drawDial(canvas, seconds, reach, fade, 1f / scale)
        } else {
            drawSign(canvas, reach, fade, 1f / scale)
        }
        canvas.restoreToCount(checkpoint)
        drawBand(canvas, size.width, height)
    }

    private fun drawStars(canvas: Canvas, seconds: Double, halfWidth: Float, pixel: Float) {
        val stars = StarstaveScene.Stars
        val top = starstaveOriginY(showsDial)
        val bottom = top + StarstaveScene.SCREEN_HEIGHT
        val minSize = MIN_STAR_PX * pixel
        for (i in levels.indices) {
            val phase = stars[i * StarstaveScene.STAR_STRIDE + StarstaveScene.STAR_PHASE]
            levels[i] = starstaveStarLevel(seconds, phase)
        }
        for (level in 0 until StarstaveScene.LEVEL_COUNT) {
            starPaint.alpha = alphaOf(StarstaveScene.LevelAlphas[level])
            for (i in levels.indices) {
                if (levels[i] != level) continue
                val at = i * StarstaveScene.STAR_STRIDE
                val x = stars[at + StarstaveScene.STAR_X]
                val y = stars[at + StarstaveScene.STAR_Y]
                if (y < top || y > bottom || abs(x) > halfWidth) continue
                val half = max(stars[at + StarstaveScene.STAR_SIZE], minSize) / 2f
                canvas.drawRect(x - half, y - half, x + half, y + half, starPaint)
            }
        }
    }

    private fun drawSign(canvas: Canvas, reach: Float, fade: Float, pixel: Float) {
        val xs = StarstaveScene.NodeX
        val ys = StarstaveScene.NodeY
        val lengths = StarstaveScene.NodeReach
        signPath.rewind()
        signPath.moveTo(xs[0], ys[0])
        for (i in 1 until xs.size) {
            if (reach <= lengths[i - 1]) break
            if (reach >= lengths[i]) {
                signPath.lineTo(xs[i], ys[i])
            } else {
                signPath.lineTo(starstaveHeadX(reach), starstaveHeadY(reach))
            }
        }
        linePaint.strokeWidth = max(SIGN_WIDTH, SIGN_MIN_PX * pixel)
        linePaint.alpha = alphaOf(fade)
        canvas.drawPath(signPath, linePaint)
        dotPaint.alpha = alphaOf(fade)
        for (i in xs.indices) {
            if (lengths[i] <= reach) canvas.drawCircle(xs[i], ys[i], NODE_RADIUS, dotPaint)
        }
    }

    private fun drawDial(canvas: Canvas, seconds: Double, reach: Float, fade: Float, pixel: Float) {
        val cx = StarstaveScene.DIAL_CENTER_X
        val cy = StarstaveScene.DIAL_CENTER_Y
        val outer = StarstaveScene.DIAL_OUTER_RADIUS
        val inner = StarstaveScene.DIAL_INNER_RADIUS
        linePaint.strokeWidth = max(DIAL_WIDTH, pixel)
        linePaint.alpha = alphaOf(DIAL_RING_ALPHA)
        canvas.drawCircle(cx, cy, inner, linePaint)
        canvas.drawCircle(cx, cy, StarstaveScene.DIAL_MIDDLE_RADIUS, linePaint)
        canvas.drawCircle(cx, cy, outer, linePaint)

        fillTicks(seconds)
        linePaint.alpha = alphaOf(DIAL_TICK_ALPHA)
        canvas.drawLines(tickLines, linePaint)

        val angle = atan2(starstaveHeadY(reach) - cy, starstaveHeadX(reach) - cx)
        val dirX = cos(angle)
        val dirY = sin(angle)
        linePaint.alpha = alphaOf(fade)
        canvas.drawLine(
            cx + dirX * inner,
            cy + dirY * inner,
            cx + dirX * outer,
            cy + dirY * outer,
            linePaint,
        )
        dotPaint.alpha = alphaOf(fade)
        canvas.drawCircle(cx + dirX * outer, cy + dirY * outer, NEEDLE_TIP_RADIUS, dotPaint)
    }

    private fun fillTicks(seconds: Double) {
        val cx = StarstaveScene.DIAL_CENTER_X
        val cy = StarstaveScene.DIAL_CENTER_Y
        val outer = StarstaveScene.DIAL_OUTER_RADIUS
        val spin = seconds * StarstaveScene.DIAL_TICK_SPIN
        for (i in 0 until StarstaveScene.DIAL_TICK_COUNT) {
            val angle = spin + i * StarstaveScene.DIAL_TICK_STEP
            val dirX = cos(angle).toFloat()
            val dirY = sin(angle).toFloat()
            val isLong = i % StarstaveScene.DIAL_LONG_TICK_EVERY == 0
            val length = if (isLong) StarstaveScene.DIAL_TICK_LONG else StarstaveScene.DIAL_TICK_SHORT
            val at = i * COORDS_PER_LINE
            tickLines[at] = cx + dirX * outer
            tickLines[at + 1] = cy + dirY * outer
            tickLines[at + 2] = cx + dirX * (outer - length)
            tickLines[at + 3] = cy + dirY * (outer - length)
        }
    }

    private fun drawBand(canvas: Canvas, width: Float, height: Float) {
        if (bandHeight != height) {
            bandPaint.shader = LinearGradient(
                0f, 0f, 0f, height, bandColors, bandStops, Shader.TileMode.CLAMP,
            )
            bandHeight = height
        }
        canvas.drawRect(0f, 0f, width, height, bandPaint)
    }

    private fun fillPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    private fun alphaOf(fraction: Float): Int = (fraction * 255).roundToInt().coerceIn(0, 255)
}
