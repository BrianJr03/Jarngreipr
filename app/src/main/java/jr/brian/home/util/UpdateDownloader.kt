package jr.brian.home.util

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Int, val downloadedBytes: Long, val totalBytes: Long) :
        DownloadState()

    data class Success(val file: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

object UpdateDownloader {

    /**
     * Download an APK file with progress updates.
     * Files are saved to the public Downloads folder for easy access.
     *
     * @param context Android context
     * @param downloadUrl URL to download the APK from
     * @param fileName Name for the downloaded file
     * @return Flow emitting download progress states
     */
    fun downloadApk(
        context: Context,
        downloadUrl: String,
        fileName: String = "update.apk"
    ): Flow<DownloadState> = flow {
        emit(DownloadState.Idle)

        try {
            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/octet-stream")
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                emit(DownloadState.Error("Server returned ${connection.responseCode}"))
                return@flow
            }

            val totalBytes = connection.contentLengthLong
            var downloadedBytes = 0L

            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("Failed to create download file")

            connection.inputStream.use { input ->
                resolver.openOutputStream(uri)?.use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val progress = if (totalBytes > 0) {
                            ((downloadedBytes * 100) / totalBytes).toInt()
                        } else {
                            -1
                        }

                        emit(DownloadState.Downloading(progress, downloadedBytes, totalBytes))
                    }
                } ?: throw Exception("Failed to open output stream")
            }

            // Mark download as complete
            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            // Get the actual file path for installation
            val outputFile = getFileFromUri(context, uri)

            emit(DownloadState.Success(outputFile))

        } catch (e: Exception) {
            emit(DownloadState.Error(e.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Get a File object from a content Uri (for Android 10+)
     */
    private fun getFileFromUri(context: Context, uri: Uri): File {
        // Copy to cache for installation since we can't install directly from MediaStore
        val cacheDir = File(context.cacheDir, "updates")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val fileName = uri.lastPathSegment ?: "update.apk"
        val cacheFile = File(cacheDir, fileName.substringAfterLast("/"))

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(cacheFile).use { output ->
                input.copyTo(output)
            }
        }

        return cacheFile
    }

    /**
     * Install an APK file
     *
     * @param context Android context
     * @param apkFile The APK file to install
     */
    fun installApk(context: Context, apkFile: File) {
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        context.startActivity(intent)
    }

    /**
     * Check if the app can install packages from unknown sources
     */
    fun canInstallPackages(context: Context): Boolean {
        return context.packageManager.canRequestPackageInstalls()
    }

    /**
     * Open settings to allow installing from unknown sources
     */
    fun openInstallPermissionSettings(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = "package:${context.packageName}".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Format bytes to human-readable string
     */
    @SuppressLint("DefaultLocale")
    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
