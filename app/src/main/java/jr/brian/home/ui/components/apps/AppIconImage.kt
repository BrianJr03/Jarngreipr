package jr.brian.home.ui.components.apps

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import jr.brian.home.data.CustomIconManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Composable that displays either a custom icon (if available) or the default app icon.
 * Handles loading custom PNG icons from internal storage with fallback to system icon.
 * Automatically updates when custom icons are changed.
 */
@Composable
fun AppIconImage(
    defaultIcon: Drawable,
    packageName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    customIconManager: CustomIconManager? = null,
    contentScale: ContentScale = ContentScale.Fit
) {
    // Observe the custom icons map to automatically react to changes
    val customIconsMap by customIconManager?.customIconsMap?.collectAsStateWithLifecycle(
        initialValue = emptyMap()
    )
        ?: remember { mutableStateOf(emptyMap()) }

    val customIconPath = customIconsMap[packageName]

    // Display custom icon if available, otherwise default icon
    if (customIconPath != null && File(customIconPath).exists()) {
        // Use Coil to load custom PNG with caching
        Image(
            painter = rememberAsyncImagePainter(
                model = File(customIconPath)
            ),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        // Use default app icon
        Image(
            painter = rememberAsyncImagePainter(model = defaultIcon),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

/**
 * Alternative version that returns the bitmap directly for non-Compose usage
 */
@Composable
fun rememberAppIconBitmap(
    defaultIcon: Drawable,
    packageName: String,
    customIconManager: CustomIconManager?
): androidx.compose.ui.graphics.ImageBitmap {
    var customBitmap by remember(packageName) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(
            null
        )
    }
    val defaultBitmap = remember(defaultIcon) { defaultIcon.toBitmap().asImageBitmap() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(packageName, customIconManager) {
        if (customIconManager != null) {
            scope.launch {
                val path = withContext(Dispatchers.IO) {
                    customIconManager.getCustomIconPath(packageName)
                }
                if (path != null) {
                    val bitmap = withContext(Dispatchers.IO) {
                        customIconManager.loadCustomIconBitmap(path)
                    }
                    customBitmap = bitmap?.asImageBitmap()
                }
            }
        }
    }

    return customBitmap ?: defaultBitmap
}
