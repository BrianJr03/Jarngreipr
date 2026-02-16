package jr.brian.home.esde.util

import androidx.compose.runtime.staticCompositionLocalOf
import coil.ImageLoader

/**
 * CompositionLocal for the shared ESDE ImageLoader.
 * 
 * This ImageLoader is configured with:
 * - Memory cache (30% of available memory)
 * - Disk cache (100MB)
 * - Support for GIF animations and SVG images
 */
val LocalESDEImageLoader = staticCompositionLocalOf<ImageLoader> {
    error("No ESDEImageLoader provided. Make sure to provide it in the composition hierarchy.")
}
