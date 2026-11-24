package jr.brian.jarnfe.library

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DEFAULT_COVER_ASPECT = 4f / 5f
private const val MIN_COVER_ASPECT = 0.6f
private const val MAX_COVER_ASPECT = 1.6f
private const val MIN_MARQUEE_ASPECT = 1.25f
private const val BOUNDS_CACHE_SIZE = 128
private const val CONTENT_SCHEME = "content:"

private val artBounds = LruCache<String, IntSize>(BOUNDS_CACHE_SIZE)

/** The width over height a cover's tile should use, so no empty bars show around the art. */
internal fun nowCoverAspect(bounds: IntSize?): Float {
    if (bounds == null || bounds.width <= 0 || bounds.height <= 0) return DEFAULT_COVER_ASPECT
    return (bounds.width.toFloat() / bounds.height).coerceIn(MIN_COVER_ASPECT, MAX_COVER_ASPECT)
}

/** A square or tall image isn't a usable title banner. An unreadable size gets the benefit of the doubt. */
internal fun nowMarqueeFits(bounds: IntSize): Boolean =
    bounds.width <= 0 || bounds.height <= 0 || bounds.width.toFloat() / bounds.height >= MIN_MARQUEE_ASPECT

private fun readArtBounds(context: Context, path: String): IntSize {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching {
        if (path.startsWith(CONTENT_SCHEME)) {
            context.contentResolver.openInputStream(Uri.parse(path))?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        } else {
            BitmapFactory.decodeFile(path, options)
        }
    }
    return IntSize(options.outWidth, options.outHeight)
}

/** Null until the image's size is known. Reads only the header, and waits while a game is up. */
@Composable
internal fun rememberNowArtBounds(path: String?): IntSize? {
    val context = LocalContext.current.applicationContext
    val canLoad = rememberJarnArtCanLoad()
    val read by produceState<Pair<String, IntSize>?>(null, path, canLoad) {
        if (path == null || !canLoad || artBounds.get(path) != null) return@produceState
        val bounds = withContext(Dispatchers.IO) { readArtBounds(context, path) }
        artBounds.put(path, bounds)
        value = path to bounds
    }
    if (path == null) return null
    return artBounds.get(path) ?: read?.takeIf { it.first == path }?.second
}
