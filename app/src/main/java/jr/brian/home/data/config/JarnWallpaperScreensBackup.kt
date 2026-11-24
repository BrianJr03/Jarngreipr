package jr.brian.home.data.config

import jr.brian.frontend.look.JarnLookStore
import kotlinx.serialization.Serializable

/**
 * v54: Jarn FE "Same on both screens" and the bottom screen's own image or GIF. Like the frontend
 * wallpaper, the backup carries the path and not the file, so a restore on another device shows
 * the theme until a new image is picked. Each field is nullable so a partial backup keeps the rest.
 */
@Serializable
data class JarnWallpaperScreensConfig(
    val sameOnBoth: Boolean? = null,
    val bottomUri: String? = null,
)

internal fun JarnLookStore.wallpaperScreensBackup(): JarnWallpaperScreensConfig =
    JarnWallpaperScreensConfig(sameOnBoth = sameWallpaperOnBoth, bottomUri = bottomWallpaperUri)

/** Null means the backup predates v54, so the current choice stays. */
internal fun JarnLookStore.restoreWallpaperScreens(backup: JarnWallpaperScreensConfig?) {
    backup ?: return
    backup.sameOnBoth?.let(::setSameWallpaperOnBoth)
    if (backup.sameOnBoth != null || backup.bottomUri != null) {
        setBottomWallpaperUri(backup.bottomUri.takeUnless { sameWallpaperOnBoth })
    }
}
