package jr.brian.home.ui.components.apps

import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import jr.brian.home.data.CustomIconManager
import java.io.File

@Composable
fun AppIconImage(
    defaultIcon: Drawable,
    packageName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    customIconManager: CustomIconManager? = null,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val customIconsMap by customIconManager?.customIconsMap?.collectAsStateWithLifecycle(
        initialValue = emptyMap()
    )
        ?: remember { mutableStateOf(emptyMap()) }

    val customIconPath = customIconsMap[packageName]

    val gifImageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (customIconPath != null && File(customIconPath).exists()) {
            val isGif = customIconPath.endsWith(".gif", ignoreCase = true)
            
            Image(
                painter = rememberAsyncImagePainter(
                    model = File(customIconPath),
                    imageLoader = if (isGif) gifImageLoader else ImageLoader(context)
                ),
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale
            )
        } else {
            Image(
                painter = rememberAsyncImagePainter(model = defaultIcon),
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale
            )
        }
    }
}