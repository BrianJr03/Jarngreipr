package jr.brian.jarnfe.backdrop

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/** One sampled pixel per this many screen pixels each way. Labels span several, so it stays accurate. */
internal const val JARN_BACKDROP_DOWNSCALE = 8

/** A small copy of the backdrop, read where header labels sit. */
internal class JarnBackdropPixels(
    private val width: Int,
    private val height: Int,
    private val scale: Float,
    private val pixels: IntArray,
) {
    /** Mean luminance under [rect], given in the backdrop's full-size pixels, or null when it misses. */
    fun meanLuminance(rect: Rect): Double? {
        val left = floor(rect.left / scale).toInt().coerceIn(0, width)
        val top = floor(rect.top / scale).toInt().coerceIn(0, height)
        val right = ceil(rect.right / scale).toInt().coerceIn(0, width)
        val bottom = ceil(rect.bottom / scale).toInt().coerceIn(0, height)
        if (right <= left || bottom <= top) return null
        var sum = 0.0
        for (y in top until bottom) {
            val row = y * width
            for (x in left until right) sum += jarnPixelLuminance(pixels[row + x])
        }
        return sum / ((right - left) * (bottom - top))
    }
}

/** Reads a small copy of what is drawn behind the UI. */
internal interface JarnBackdropReader {
    /** [windowRect] moved into the backdrop's own pixels. */
    fun local(windowRect: Rect): Rect

    suspend fun capture(density: Density, direction: LayoutDirection): JarnBackdropPixels?
}

/**
 * Records everything drawn behind the screen's UI, so labels can ask how bright it is under them.
 * Recording costs nothing extra per frame. Only [capture] reads pixels, at a small size.
 */
@Stable
internal class JarnBackdropSampler(
    private val source: GraphicsLayer,
    private val small: GraphicsLayer,
) : JarnBackdropReader {
    private var origin = Offset.Zero
    private var size = IntSize.Zero
    private var buffer = IntArray(0)

    fun place(originInWindow: Offset, layoutSize: IntSize) {
        origin = originInWindow
        size = layoutSize
    }

    override fun local(windowRect: Rect): Rect = windowRect.translate(-origin)

    fun record(scope: ContentDrawScope) = with(scope) {
        source.record { scope.drawContent() }
        drawLayer(source)
    }

    override suspend fun capture(density: Density, direction: LayoutDirection): JarnBackdropPixels? {
        if (size.width <= 0 || size.height <= 0) return null
        val width = max(1, size.width / JARN_BACKDROP_DOWNSCALE)
        val height = max(1, size.height / JARN_BACKDROP_DOWNSCALE)
        small.record(density, direction, IntSize(width, height)) {
            scale(1f / JARN_BACKDROP_DOWNSCALE, pivot = Offset.Zero) { drawLayer(source) }
        }
        val bitmap = small.toImageBitmap().asAndroidBitmap()
        val readable = if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
        if (buffer.size != width * height) buffer = IntArray(width * height)
        readable.getPixels(buffer, 0, width, 0, 0, width, height)
        if (readable !== bitmap) readable.recycle()
        return JarnBackdropPixels(width, height, JARN_BACKDROP_DOWNSCALE.toFloat(), buffer)
    }
}

@Composable
internal fun rememberJarnBackdropSampler(): JarnBackdropSampler {
    val source = rememberGraphicsLayer()
    val small = rememberGraphicsLayer()
    return remember(source, small) { JarnBackdropSampler(source, small) }
}

/** Marks the layer whose drawing [sampler] reads. Put it on the backdrop, under all UI. */
internal fun Modifier.jarnBackdropSource(sampler: JarnBackdropSampler): Modifier = this
    .onGloballyPositioned { sampler.place(it.positionInWindow(), it.size) }
    .drawWithContent { sampler.record(this) }
