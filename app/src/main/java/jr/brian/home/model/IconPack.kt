package jr.brian.home.model

import android.graphics.drawable.Drawable

/**
 * Represents an installed icon pack
 */
data class IconPack(
    val packageName: String,
    val name: String,
    val icon: Drawable?
)

/**
 * Represents a component mapping in an icon pack's appfilter.xml
 */
@Suppress("unused")
data class IconPackMapping(
    val componentName: String,
    val drawableName: String
)

/**
 * Contains icon pack configuration and resources
 */
data class IconPackResources(
    val maskIcons: List<Drawable> = emptyList(),
    val backIcons: List<Drawable> = emptyList(),
    val frontIcon: Drawable? = null,
    val scale: Float = 1.0f
)
