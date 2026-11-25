package jr.brian.jarnfe.apps

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.settings.jarnAppLabelScale
import java.io.File

private const val ICON_SCALE_WITHOUT_NAME = 1.25f

/** Icons grow into the space a hidden name leaves. Tile bounds still cap the result. */
internal fun jarnAppIconSize(base: Dp, nameVisible: Boolean): Dp =
    if (nameVisible) base else base * ICON_SCALE_WITHOUT_NAME

/** Keeps a hidden name available to screen readers. A visible name already speaks for itself. */
internal fun Modifier.jarnHiddenAppName(label: String, nameVisible: Boolean): Modifier =
    if (nameVisible) this else semantics { contentDescription = label }

/** Keyed on the file's version, because replacing a custom icon keeps the same path. */
@Composable
internal fun JarnAppIcon(
    icon: Drawable?,
    iconPath: String?,
    iconVersion: Long,
    modifier: Modifier = Modifier,
) {
    if (iconPath == null) {
        JarnDrawableIcon(icon, modifier)
        return
    }
    key(iconPath, iconVersion) {
        AsyncImage(model = File(iconPath), contentDescription = null, modifier = modifier)
    }
}

/**
 * Draws on the first frame. Coil never memory caches a Drawable, so loading one through it leaves
 * the icon blank for a frame each time a tile enters composition, and a whole grid flashes.
 */
@Composable
internal fun JarnDrawableIcon(icon: Drawable?, modifier: Modifier = Modifier) {
    if (icon == null) {
        Spacer(modifier)
        return
    }
    val painter = remember(icon) { JarnDrawablePainter(icon) }
    Image(painter = painter, contentDescription = null, modifier = modifier)
}

@Composable
internal fun JarnAppNameLabel(label: String, labelSize: Int, modifier: Modifier = Modifier) {
    val base = JarnTheme.baseType.label
    val scale = jarnAppLabelScale(labelSize)
    val style = base.copy(
        fontSize = if (base.fontSize.isSp) base.fontSize * scale else base.fontSize,
        lineHeight = if (base.lineHeight.isSp) base.lineHeight * scale else base.lineHeight,
    )
    JarnText(label, style, JarnTheme.colors.textHigh, modifier, maxLines = 1)
}
