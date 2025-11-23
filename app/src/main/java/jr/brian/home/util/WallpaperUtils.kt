package jr.brian.home.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import jr.brian.home.ui.theme.WallpaperType

object WallpaperUtils {
    /**
     * Detects the wallpaper type based on MIME type or file extension
     */
    fun detectWallpaperType(context: Context, uri: Uri): WallpaperType {
        val mimeType = context.contentResolver.getType(uri)

        if (mimeType != null) {
            return when {
                mimeType.startsWith("image/gif") -> WallpaperType.GIF
                mimeType.startsWith("video/") -> WallpaperType.VIDEO
                mimeType.startsWith("image/") -> WallpaperType.IMAGE
                else -> WallpaperType.IMAGE // Default to image
            }
        }

        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        return when (extension.lowercase()) {
            "gif" -> WallpaperType.GIF
            "mp4", "mov", "avi", "mkv", "webm", "m4v" -> WallpaperType.VIDEO
            "jpg", "jpeg", "png", "bmp", "webp" -> WallpaperType.IMAGE
            else -> WallpaperType.IMAGE // Default to image
        }
    }
}
