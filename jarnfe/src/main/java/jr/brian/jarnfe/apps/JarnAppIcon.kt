package jr.brian.jarnfe.apps

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import coil.compose.AsyncImage
import java.io.File

/** Keyed on the file's version, because replacing a custom icon keeps the same path. */
@Composable
internal fun JarnAppIcon(
    icon: Drawable?,
    iconPath: String?,
    iconVersion: Long,
    modifier: Modifier = Modifier,
) {
    key(iconPath, iconVersion) {
        AsyncImage(
            model = iconPath?.let(::File) ?: icon,
            contentDescription = null,
            modifier = modifier,
        )
    }
}
