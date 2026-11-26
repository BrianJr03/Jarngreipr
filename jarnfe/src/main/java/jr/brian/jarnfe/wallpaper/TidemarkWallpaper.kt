package jr.brian.jarnfe.wallpaper

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.LocalJarnScreen
import kotlin.math.max
import kotlin.math.roundToInt

private const val TIDEMARK_FPS = 20
private const val STROKE_MIN_PX = 1.2f
private const val TILE_MIN_PIXELS = 16
private const val COVER_REACH = 1.42f
private const val CROSS_REACH = 0.8f
private const val TRIANGLE_SIDE_X = 0.866f
private const val TRIANGLE_TOP_Y = -0.9f
private const val TRIANGLE_BASE_Y = 0.6f
private const val SQUARE_REACH = 0.78f
private const val CIRCLE_RADIUS = 0.9f

@Composable
internal fun TidemarkWallpaper(modifier: Modifier) {
    val colors = JarnTheme.colors
    val isBottomScreen = rememberJarnWallpaperIsBottomScreen()
    val hasDeckUi = LocalJarnScreen.current == JarnScreen.Deck
    val isMotionReduced = JarnTheme.motion.ambientReduced
    val canAnimate = rememberJarnWallpaperCanAnimate()
    val palette = remember(colors) { colors.toJarnWallpaperPalette() }
    val tile = remember(palette) { TidemarkTile(palette) }
    DisposableEffect(tile) { onDispose(tile::release) }
    val painter = remember(palette, tile, isBottomScreen, hasDeckUi, isMotionReduced) {
        TidemarkPainter(palette, tile, isBottomScreen, hasDeckUi, isMotionReduced)
    }
    Spacer(modifier.jarnWallpaperDraw(canAnimate, TIDEMARK_FPS, painter))
}

internal fun tidemarkBaseColor(palette: JarnWallpaperPalette): Int =
    jarnWallpaperMix(palette.bg, palette.midGlow, TidemarkScene.BASE_ALPHA)

internal fun tidemarkStroke(scale: Float): Float = max(TidemarkScene.STROKE, STROKE_MIN_PX / scale)

/** Unit shapes centered on the origin, scaled to each cell's size when drawn. */
private val TidemarkPaths: Array<Path> = Array(TidemarkScene.KIND_COUNT) { kind ->
    Path().apply {
        when (kind) {
            TidemarkScene.KIND_CROSS -> {
                moveTo(-CROSS_REACH, -CROSS_REACH)
                lineTo(CROSS_REACH, CROSS_REACH)
                moveTo(-CROSS_REACH, CROSS_REACH)
                lineTo(CROSS_REACH, -CROSS_REACH)
            }
            TidemarkScene.KIND_TRIANGLE -> {
                moveTo(0f, TRIANGLE_TOP_Y)
                lineTo(TRIANGLE_SIDE_X, TRIANGLE_BASE_Y)
                lineTo(-TRIANGLE_SIDE_X, TRIANGLE_BASE_Y)
                close()
            }
            TidemarkScene.KIND_SQUARE ->
                addRect(-SQUARE_REACH, -SQUARE_REACH, SQUARE_REACH, SQUARE_REACH, Path.Direction.CW)
            else -> addCircle(0f, 0f, CIRCLE_RADIUS, Path.Direction.CW)
        }
    }
}

private fun outlinePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
}

private fun drawOutline(canvas: Canvas, kind: Int, x: Float, y: Float, half: Float, stroke: Float, paint: Paint) {
    paint.strokeWidth = stroke / half
    val checkpoint = canvas.save()
    canvas.translate(x, y)
    canvas.scale(half, half)
    canvas.drawPath(TidemarkPaths[kind], paint)
    canvas.restoreToCount(checkpoint)
}

/** The resting grid, one repeat of the pattern, baked once per scale so a frame never redraws it. */
private class TidemarkTile(private val palette: JarnWallpaperPalette) {
    private var shader: BitmapShader? = null
    private var pixels = 0

    fun shaderFor(scale: Float): Shader {
        val wanted = max(TILE_MIN_PIXELS, (TidemarkScene.TILE_SIZE * scale).roundToInt())
        val current = shader
        if (current != null && pixels == wanted) return current
        val baked = bake(wanted, scale)
        shader = baked
        pixels = wanted
        return baked
    }

    fun release() {
        shader = null
        pixels = 0
    }

    private fun bake(size: Int, scale: Float): BitmapShader {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(palette.bg)
        val pixelsPerUnit = size / TidemarkScene.TILE_SIZE
        canvas.scale(pixelsPerUnit, pixelsPerUnit)
        val paint = outlinePaint().apply { color = tidemarkBaseColor(palette) }
        val stroke = tidemarkStroke(scale)
        for (row in 0 until TidemarkScene.PATTERN_SIZE) {
            for (col in 0 until TidemarkScene.PATTERN_SIZE) {
                val x = tidemarkCellCenter(col)
                val y = tidemarkCellCenter(row)
                drawOutline(canvas, tidemarkKind(col, row), x, y, TidemarkScene.BASE_HALF, stroke, paint)
            }
        }
        val unitsPerPixel = 1f / pixelsPerUnit
        return BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT).apply {
            setLocalMatrix(Matrix().apply { setScale(unitsPerPixel, unitsPerPixel) })
        }
    }
}

/** Holds all drawing state up front, so a frame allocates nothing once the window size settles. */
private class TidemarkPainter(
    private val palette: JarnWallpaperPalette,
    private val tile: TidemarkTile,
    private val isBottomScreen: Boolean,
    hasDeckUi: Boolean,
    private val isMotionReduced: Boolean,
) : JarnWallpaperPainter {
    private val band = JarnWallpaperBand(palette, hasDeckUi)
    private val baseColor = tidemarkBaseColor(palette)
    private val tilePaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val crestPaint = outlinePaint()
    private val coverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.bg }
    private var crest = FloatArray(0)

    override fun DrawScope.paint(timeNanos: Long) {
        val height = size.height
        if (height <= 0f || size.width <= 0f) return
        val canvas = drawContext.canvas.nativeCanvas
        val isStill = isMotionReduced || timeNanos == 0L
        val scale = jarnWallpaperScale(height)
        val halfWidth = jarnWallpaperHalfWidth(size.width, height)
        val top = jarnWallpaperOriginY(isBottomScreen)
        val capacity = tidemarkCrestCapacity(halfWidth, top)
        if (crest.size != capacity) crest = FloatArray(capacity)

        val checkpoint = canvas.save()
        canvas.translate(size.width / 2f, -top * scale)
        canvas.scale(scale, scale)
        tilePaint.shader = tile.shaderFor(scale)
        canvas.drawRect(-halfWidth, top, halfWidth, top + JARN_WALLPAPER_SCREEN_HEIGHT, tilePaint)
        val phase = tidemarkPhase(tidemarkSeconds(timeNanos, isStill))
        drawCrest(canvas, tidemarkFillCrest(phase, halfWidth, top, crest), tidemarkStroke(scale))
        canvas.restoreToCount(checkpoint)
        band.draw(canvas, size.width, height)
    }

    /** Each lifted cell hides its resting shape under a background disc, then draws itself larger. */
    private fun drawCrest(canvas: Canvas, count: Int, stroke: Float) {
        val cover = TidemarkScene.BASE_HALF * COVER_REACH + stroke
        for (i in 0 until count) {
            val at = i * TidemarkScene.CREST_STRIDE
            val x = crest[at + TidemarkScene.CREST_X]
            val y = crest[at + TidemarkScene.CREST_Y]
            val amount = crest[at + TidemarkScene.CREST_AMOUNT]
            canvas.drawCircle(x, y, cover, coverPaint)
            crestPaint.color = tidemarkMix(baseColor, palette.glow, amount)
            val kind = crest[at + TidemarkScene.CREST_KIND].toInt()
            drawOutline(canvas, kind, x, y, tidemarkHalfSize(amount), stroke, crestPaint)
        }
    }
}
