package jr.brian.frontend.look.esde

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import jr.brian.core.ui.theme.jarnRelativeLuminance
import java.io.File

/** Enough pixels to find a background's main tone, and cheap to decode. */
private const val SAMPLE_WIDTH = 64
private const val SAMPLE_HEIGHT = 36
private const val DARK_LUMINANCE = 0.12
private const val MIN_DARK_SHARE = 0.1f
private const val DARKEN_BRIGHT = 0.8f
private const val CHANNEL_BUCKET_SHIFT = 4

/** Reads a small grid of ARGB pixels from a picture, or null when it can't be decoded. */
fun interface EsdeColorSampler {
    fun pixels(file: File): IntArray?
}

/** Decodes at a power-of-two step near [SAMPLE_WIDTH] by [SAMPLE_HEIGHT], then scales to exactly that. */
object AndroidEsdeColorSampler : EsdeColorSampler {
    override fun pixels(file: File): IntArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= SAMPLE_WIDTH && bounds.outHeight / (sample * 2) >= SAMPLE_HEIGHT) sample *= 2
        val decoded = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val small = Bitmap.createScaledBitmap(decoded, SAMPLE_WIDTH, SAMPLE_HEIGHT, true)
        return try {
            IntArray(SAMPLE_WIDTH * SAMPLE_HEIGHT).also { small.getPixels(it, 0, SAMPLE_WIDTH, 0, 0, SAMPLE_WIDTH, SAMPLE_HEIGHT) }
        } finally {
            if (small !== decoded) small.recycle()
            decoded.recycle()
        }
    }
}

/**
 * A background color for a theme that only has a background picture: the most common dark tone
 * in [pixels]. A mostly bright picture gives its most common tone, darkened, since Jarn's
 * backgrounds are dark. Null when there are no opaque pixels.
 */
internal fun dominantDarkColor(pixels: IntArray): Color? {
    val opaque = pixels.filter { (it ushr 24) >= 0x80 }
    if (opaque.isEmpty()) return null
    val dark = opaque.filter { jarnRelativeLuminance(Color(it)) <= DARK_LUMINANCE }
    val useDark = dark.size >= opaque.size * MIN_DARK_SHARE
    val tone = mostCommonTone(if (useDark) dark else opaque)
    return if (useDark) tone else lerp(tone, Color.Black, DARKEN_BRIGHT)
}

/** The average of the most common bucket of similar colors, so noise and gradients don't split a tone. */
private fun mostCommonTone(pixels: List<Int>): Color {
    val bucket = pixels.groupBy { it.bucket() }.maxBy { it.value.size }.value
    fun channel(shift: Int) = bucket.sumOf { (it ushr shift) and 0xFF } / bucket.size
    return Color(red = channel(16), green = channel(8), blue = channel(0))
}

private fun Int.bucket(): Int {
    val r = (this ushr 16 and 0xFF) ushr CHANNEL_BUCKET_SHIFT
    val g = (this ushr 8 and 0xFF) ushr CHANNEL_BUCKET_SHIFT
    val b = (this and 0xFF) ushr CHANNEL_BUCKET_SHIFT
    return (r shl 8) or (g shl 4) or b
}
