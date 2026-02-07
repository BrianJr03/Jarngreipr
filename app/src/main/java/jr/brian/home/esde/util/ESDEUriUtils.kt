package jr.brian.home.esde.util

import android.net.Uri

/**
 * Convert content URI to file path.
 * Handles both primary storage and external SD card paths.
 */
fun getPathFromUri(uri: Uri): String? {
    val path = uri.path ?: return null

    return when {
        path.contains("/tree/primary:") -> {
            val relativePath = path.substringAfter("/tree/primary:")
            "/storage/emulated/0/$relativePath"
        }

        path.contains("/tree/") -> {
            // Handle external SD card or other storage
            val storagePart = path.substringAfter("/tree/").substringBefore(":")
            val relativePath = path.substringAfter(":")
            "/storage/$storagePart/$relativePath"
        }

        else -> null
    }
}
