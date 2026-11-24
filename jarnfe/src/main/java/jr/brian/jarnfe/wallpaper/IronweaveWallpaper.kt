package jr.brian.jarnfe.wallpaper

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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

private const val IRONWEAVE_FPS = 15
private const val TRANSPARENT_RGB_MASK = 0x00FFFFFF

@Composable
internal fun IronweaveWallpaper(modifier: Modifier) {
    val colors = JarnTheme.colors
    val isBottomScreen = rememberJarnWallpaperIsBottomScreen()
    val hasDeckUi = LocalJarnScreen.current == JarnScreen.Deck
    val isMotionReduced = JarnTheme.motion.reduced
    val canAnimate = rememberJarnWallpaperCanAnimate()
    val palette = remember(colors) { colors.toJarnWallpaperPalette() }
    val tile = remember(palette.bg) { IronweaveTile(palette.bg) }
    DisposableEffect(tile) { onDispose(tile::release) }
    val painter = remember(palette, tile, isBottomScreen, hasDeckUi, isMotionReduced) {
        IronweavePainter(palette, tile, isBottomScreen, hasDeckUi, isMotionReduced)
    }
    Spacer(modifier.jarnWallpaperDraw(canAnimate, IRONWEAVE_FPS, painter))
}

/**
 * A background-colored tile with the rings punched out. Light painted beneath it shows only
 * through the rings, which needs no offscreen layer and no per-frame shader.
 */
private class IronweaveTile(private val bg: Int) {
    private var shader: BitmapShader? = null
    private var pixels = 0

    fun shaderFor(scale: Float): Shader {
        val wanted = ironweaveTilePixels(scale)
        val current = shader
        if (current != null && pixels == wanted) return current
        val baked = bake(wanted, ironweaveTileSize(scale) / wanted)
        shader = baked
        pixels = wanted
        return baked
    }

    fun release() {
        shader = null
        pixels = 0
    }

    private fun bake(size: Int, sceneUnitsPerPixel: Float): BitmapShader {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(bg)
        val punch = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * IronweaveScene.RING_WIDTH_RATIO
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        val radius = size * IronweaveScene.RING_RADIUS_RATIO
        for (i in IronweaveScene.RingCentersX.indices) {
            val x = IronweaveScene.RingCentersX[i] * size
            val y = IronweaveScene.RingCentersY[i] * size
            canvas.drawCircle(x, y, radius, punch)
        }
        return BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT).apply {
            setLocalMatrix(Matrix().apply { setScale(sceneUnitsPerPixel, sceneUnitsPerPixel) })
        }
    }
}

/** Holds all drawing state up front, so a frame allocates nothing. */
private class IronweavePainter(
    palette: JarnWallpaperPalette,
    private val tile: IronweaveTile,
    private val isBottomScreen: Boolean,
    hasDeckUi: Boolean,
    private val isMotionReduced: Boolean,
) : JarnWallpaperPainter {
    private val ringBase = ironweaveRingBase(palette)
    private val band = JarnWallpaperBand(palette, hasDeckUi)
    private val tilePaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val sheenPaint = Paint().apply {
        isDither = true
        val clear = palette.glow and TRANSPARENT_RGB_MASK
        shader = LinearGradient(
            IronweaveScene.SHEEN_START_X,
            IronweaveScene.SHEEN_START_Y,
            IronweaveScene.SHEEN_END_X,
            IronweaveScene.SHEEN_END_Y,
            intArrayOf(clear, palette.glow, clear),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
    }

    override fun DrawScope.paint(timeNanos: Long) {
        val height = size.height
        if (height <= 0f || size.width <= 0f) return
        val canvas = drawContext.canvas.nativeCanvas
        val isStill = isMotionReduced || timeNanos == 0L
        val scale = jarnWallpaperScale(height)
        val halfWidth = jarnWallpaperHalfWidth(size.width, height)
        val top = jarnWallpaperOriginY(isBottomScreen)
        val bottom = top + JARN_WALLPAPER_SCREEN_HEIGHT

        canvas.drawColor(ringBase)
        val checkpoint = canvas.save()
        canvas.translate(size.width / 2f, -top * scale)
        canvas.scale(scale, scale)
        drawSheen(canvas, ironweaveSheenPosition(ironweaveSeconds(timeNanos, isStill)), halfWidth, top, bottom)
        tilePaint.shader = tile.shaderFor(scale)
        canvas.drawRect(-halfWidth, top, halfWidth, bottom, tilePaint)
        canvas.restoreToCount(checkpoint)
        band.draw(canvas, size.width, height)
    }

    private fun drawSheen(canvas: Canvas, position: Float, halfWidth: Float, top: Float, bottom: Float) {
        val driftY = position * IronweaveScene.SHEEN_DRIFT_Y
        val checkpoint = canvas.save()
        canvas.translate(position, driftY)
        canvas.drawRect(-halfWidth - position, top - driftY, halfWidth - position, bottom - driftY, sheenPaint)
        canvas.restoreToCount(checkpoint)
    }
}
