package jr.brian.home.ui.components.apps

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import jr.brian.home.ui.theme.managers.LocalCustomIconManager

@Composable
fun AppIconImage(
    packageName: String,
    defaultIcon: Drawable,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val customIconManager = LocalCustomIconManager.current
    val customIconUri by customIconManager.getCustomIconUri(packageName)
        .collectAsStateWithLifecycle(initialValue = null)

    val iconModel = if (customIconUri != null) {
        customIconUri!!.toUri()
    } else {
        defaultIcon
    }

    Image(
        painter = rememberAsyncImagePainter(model = iconModel),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}
