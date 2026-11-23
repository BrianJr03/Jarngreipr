package jr.brian.jarnfe.library

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.SuccessResult
import jr.brian.core.ui.theme.JarnTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val LOGO_SAMPLE_PX = 48
private const val LOGO_VISIBLE_ALPHA = 128
private const val LOGO_MIN_CONTRAST = 3f
private const val LOGO_CONTRAST_OFFSET = 0.05f
private const val LOGO_BACKING_ALPHA = 0.92f
private val NowLogoHeight = 40.dp
private val NowLogoMaxWidth = 240.dp

/** True when most of a logo's visible pixels would be hard to read on [plate]. */
internal fun nowLogoNeedsBacking(pixels: IntArray, plate: Color): Boolean {
    val visible = pixels.filter { it ushr 24 >= LOGO_VISIBLE_ALPHA }
    if (visible.isEmpty()) return false
    val plateLuminance = plate.luminance()
    val faint = visible.count { logoContrast(Color(it).luminance(), plateLuminance) < LOGO_MIN_CONTRAST }
    return faint * 2 >= visible.size
}

private fun logoContrast(first: Float, second: Float): Float =
    (maxOf(first, second) + LOGO_CONTRAST_OFFSET) / (minOf(first, second) + LOGO_CONTRAST_OFFSET)

private suspend fun logoNeedsBacking(context: Context, path: String, plate: Color): Boolean =
    withContext(Dispatchers.Default) {
        val request = stillImageBuilder(context, path)
            .size(LOGO_SAMPLE_PX)
            .allowHardware(false)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .build()
        val drawable = (context.imageLoader.execute(request) as? SuccessResult)?.drawable
            ?: return@withContext false
        val pixels = IntArray(LOGO_SAMPLE_PX * LOGO_SAMPLE_PX)
        drawable.toBitmap(LOGO_SAMPLE_PX, LOGO_SAMPLE_PX, Bitmap.Config.ARGB_8888)
            .getPixels(pixels, 0, LOGO_SAMPLE_PX, 0, 0, LOGO_SAMPLE_PX, LOGO_SAMPLE_PX)
        nowLogoNeedsBacking(pixels, plate)
    }

/** Stays hidden until its contrast is known, so a backing never pops in behind a visible logo. */
@Composable
internal fun NowSystemLogo(path: String, title: String, onFailed: () -> Unit) {
    val context = LocalContext.current
    val colors = JarnTheme.colors
    val canLoad = rememberJarnArtCanLoad()
    val needsBacking by produceState<Boolean?>(null, path, colors.bg, canLoad) {
        if (canLoad) value = logoNeedsBacking(context, path, colors.bg)
    }
    NowRevealWhen(needsBacking != null)
    val backing = if (needsBacking == true) {
        Modifier
            .background(colors.textHigh.copy(alpha = LOGO_BACKING_ALPHA), JarnTheme.radius.smShape)
            .padding(horizontal = JarnTheme.space.sm, vertical = JarnTheme.space.xs)
    } else {
        Modifier
    }
    AsyncImage(
        model = stillImageBuilder(context, path).build(),
        contentDescription = title,
        modifier = Modifier
            .alpha(if (needsBacking == null) 0f else 1f)
            .then(backing)
            .height(NowLogoHeight)
            .widthIn(max = NowLogoMaxWidth),
        alignment = Alignment.CenterStart,
        contentScale = ContentScale.Fit,
        onError = { onFailed() },
    )
}
