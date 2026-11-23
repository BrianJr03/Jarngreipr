package jr.brian.jarnfe.apps

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import coil.compose.AsyncImage
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.settings.jarnAppLabelScale
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

@Composable
internal fun JarnAppNameLabel(label: String, labelSize: Int, modifier: Modifier = Modifier) {
    val base = JarnTheme.type.label
    val scale = jarnAppLabelScale(labelSize)
    val style = base.copy(
        fontSize = if (base.fontSize.isSp) base.fontSize * scale else base.fontSize,
        lineHeight = if (base.lineHeight.isSp) base.lineHeight * scale else base.lineHeight,
    )
    JarnText(label, style, JarnTheme.colors.textHigh, modifier, maxLines = 1)
}
