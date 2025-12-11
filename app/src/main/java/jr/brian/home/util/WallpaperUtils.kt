package jr.brian.home.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import jr.brian.home.ui.theme.managers.WallpaperType
import java.io.File

object WallpaperUtils {
    private const val WALLPAPER_DIR = "wallpapers"
    private const val WALLPAPER_FILE_PREFIX = "wallpaper_"

    /**
     * Detects the wallpaper type based on MIME type or file extension
     */
    fun detectWallpaperType(
        context: Context,
        uri: Uri
    ): WallpaperType {
        val mimeType = context.contentResolver.getType(uri)

        if (mimeType != null) {
            return when {
                mimeType.startsWith("image/gif") -> WallpaperType.GIF
                mimeType.startsWith("video/") -> WallpaperType.VIDEO
                mimeType.startsWith("image/") -> WallpaperType.IMAGE
                else -> WallpaperType.IMAGE
            }
        }

        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        return when (extension.lowercase()) {
            "gif" -> WallpaperType.GIF
            "mp4", "mov", "avi", "mkv", "webm", "m4v" -> WallpaperType.VIDEO
            "jpg", "jpeg", "png", "bmp", "webp" -> WallpaperType.IMAGE
            else -> WallpaperType.IMAGE
        }
    }

    /**
     * Copies the wallpaper from the given URI to the app's internal storage.
     * This ensures the wallpaper remains accessible even after device restarts.
     *
     * @return The URI string of the copied file, or null if the operation fails
     */
    fun copyWallpaperToInternalStorage(
        context: Context,
        sourceUri: Uri,
        wallpaperType: WallpaperType
    ): String? {
        return try {
            // Create wallpapers directory if it doesn't exist
            val wallpaperDir = File(context.filesDir, WALLPAPER_DIR)
            if (!wallpaperDir.exists()) {
                wallpaperDir.mkdirs()
            }

            // Delete old wallpapers to save space
            cleanupOldWallpapers(wallpaperDir)

            // Determine file extension based on type
            val extension = when (wallpaperType) {
                WallpaperType.GIF -> "gif"
                WallpaperType.VIDEO -> getVideoExtension(context, sourceUri)
                WallpaperType.IMAGE -> getImageExtension(context, sourceUri)
                else -> return null
            }

            // Create the destination file
            val fileName = "$WALLPAPER_FILE_PREFIX${System.currentTimeMillis()}.$extension"
            val destFile = File(wallpaperDir, fileName)

            // Copy the file
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Return the file URI
            "file://${destFile.absolutePath}"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Cleans up old wallpaper files, keeping only the most recent one
     */
    private fun cleanupOldWallpapers(wallpaperDir: File) {
        try {
            val wallpaperFiles = wallpaperDir.listFiles { file ->
                file.isFile && file.name.startsWith(WALLPAPER_FILE_PREFIX)
            }?.sortedByDescending { it.lastModified() } ?: return

            // Keep only the most recent file, delete the rest
            wallpaperFiles.drop(1).forEach { file ->
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Deletes all wallpaper files from internal storage
     */
    fun deleteAllWallpapers(context: Context) {
        try {
            val wallpaperDir = File(context.filesDir, WALLPAPER_DIR)
            if (wallpaperDir.exists()) {
                wallpaperDir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Gets the appropriate video extension from the source URI
     */
    private fun getVideoExtension(context: Context, uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri)
        return when {
            mimeType?.contains("mp4") == true -> "mp4"
            mimeType?.contains("webm") == true -> "webm"
            mimeType?.contains("mkv") == true -> "mkv"
            else -> "mp4" // Default to mp4
        }
    }

    /**
     * Gets the appropriate image extension from the source URI
     */
    private fun getImageExtension(context: Context, uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri)
        return when {
            mimeType?.contains("png") == true -> "png"
            mimeType?.contains("webp") == true -> "webp"
            mimeType?.contains("bmp") == true -> "bmp"
            else -> "jpg" // Default to jpg
        }
    }
}
