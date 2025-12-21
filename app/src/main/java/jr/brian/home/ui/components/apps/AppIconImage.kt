package jr.brian.home.ui.components.apps

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
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
    val customIconsMap by customIconManager?.customIconsMap?.collectAsStateWithLifecycle(
        initialValue = emptyMap()
    )
        ?: remember { mutableStateOf(emptyMap()) }

    val customIconPath = customIconsMap[packageName]

    if (customIconPath != null && File(customIconPath).exists()) {
        Image(
            painter = rememberAsyncImagePainter(
                model = File(customIconPath)
            ),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Image(
            painter = rememberAsyncImagePainter(model = defaultIcon),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}