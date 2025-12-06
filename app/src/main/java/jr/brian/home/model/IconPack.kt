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
 * Represents a drawable resource from an icon pack
 */
data class IconPackDrawable(
    val drawable: Drawable,
    val resourceName: String
)
