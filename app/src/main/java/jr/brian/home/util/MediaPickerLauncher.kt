package jr.brian.home.util

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import jr.brian.home.ui.theme.managers.LocalWallpaperManager

@Composable
fun MediaPickerLauncher(
    onResult: (() -> Unit)? = null
): ManagedActivityResultLauncher<Array<String>, Uri?> {
    val context = LocalContext.current
    val wallpaperManager = LocalWallpaperManager.current
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val detectedType = WallpaperUtils.detectWallpaperType(context, uri)
            val internalUri = WallpaperUtils.copyWallpaperToInternalStorage(
                context,
                uri,
                detectedType
            )
            if (internalUri != null) {
                wallpaperManager.setWallpaper(internalUri, detectedType)
            } else {
                try {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                    wallpaperManager.setWallpaper(uri.toString(), detectedType)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        onResult?.invoke()
    }
}