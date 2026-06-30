package jr.brian.home.ui.components.apps

import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import jr.brian.home.data.CustomIconManager
import jr.brian.home.ui.theme.managers.LocalIconPackManager
import jr.brian.home.ui.theme.managers.LocalIconShapeManager
import java.io.File

@Composable
fun AppIconImage(
    defaultIcon: Drawable,
    packageName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    customIconManager: CustomIconManager? = null,
    contentScale: ContentScale = ContentScale.Fit,
    shape: Shape? = null,
    filterQuality: FilterQuality = DrawScope.DefaultFilterQuality
) {
    val iconShapeManager = LocalIconShapeManager.current
    val resolvedShape = shape ?: iconShapeManager.iconShape.toComposeShape()
    val context = LocalContext.current
    val iconPackManager = LocalIconPackManager.current
    val customIconsMap by customIconManager?.customIconsMap?.collectAsStateWithLifecycle(
        initialValue = emptyMap()
    )
        ?: remember { mutableStateOf(emptyMap()) }
    val selectedIconPack by iconPackManager.selectedIconPack.collectAsStateWithLifecycle(
        initialValue = null
    )

    val customIconPath = customIconsMap[packageName]

    val iconCacheKey = "${packageName}_${selectedIconPack ?: "default"}"

    val gifImageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(ImageDecoderDecoder.Factory())
            }.build()
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                clip = true
                this.shape = resolvedShape
            },
        contentAlignment = Alignment.Center
    ) {
        if (customIconPath != null && File(customIconPath).exists()) {
            val isGif = customIconPath.endsWith(".gif", ignoreCase = true)
            
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(customIconPath))
                    .crossfade(!isGif)
                    .memoryCacheKey("${packageName}_custom")
                    .placeholderMemoryCacheKey("${packageName}_custom")
                    .build(),
                imageLoader = if (isGif) gifImageLoader else ImageLoader(context),
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale,
                filterQuality = filterQuality
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(defaultIcon)
                    .crossfade(true)
                    .memoryCacheKey(iconCacheKey)
                    .placeholderMemoryCacheKey(iconCacheKey)
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale,
                filterQuality = filterQuality
            )
        }
    }
}