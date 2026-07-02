package jr.brian.home.canvas.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import jr.brian.home.util.WallpaperUtils

// The image-star mime would in principle cover the animated types too, but
// some system picker implementations trim the filter to still images —
// listing GIF and WebP explicitly guarantees the picker keeps them
// selectable. Video is intentionally excluded (this is a photo container).
private val PHOTO_MIME_TYPES: Array<String> = arrayOf(
    "image/*",
    "image/gif",
    "image/webp"
)

private const val PHOTO_STORAGE_SUBDIR = "canvas_photos"
private const val PHOTO_FILE_PREFIX = "photo_"

/**
 * A generic image picker launcher for canvas photo tiles. Mirrors the
 * `OpenDocument` + `WallpaperUtils` copy pattern used by
 * [jr.brian.home.util.MediaPickerLauncher] but hands the persisted URI back
 * through [onPicked] instead of tying the result to the wallpaper flow.
 *
 * The chosen image is copied into `filesDir/canvas_photos/` so it survives
 * reboots and SAF permission revocation. If the copy fails, we fall back to
 * `takePersistableUriPermission` on the source URI and hand *that* back;
 * callers can persist either shape verbatim.
 *
 * The callback fires with `null` when the user cancels the picker so callers
 * can clear whatever "picking-for-{id}" state they used to route the pick.
 */
@Composable
fun rememberPhotoPickerLauncher(
    onPicked: (persistedUri: String?) -> Unit
): ManagedActivityResultLauncher<Array<String>, Uri?> {
    val context = LocalContext.current
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            onPicked(null)
            return@rememberLauncherForActivityResult
        }

        val internalUri = WallpaperUtils.copyMediaToInternalStorage(
            context = context,
            sourceUri = uri,
            subdirName = PHOTO_STORAGE_SUBDIR,
            filePrefix = PHOTO_FILE_PREFIX
        )

        if (internalUri != null) {
            onPicked(internalUri)
            return@rememberLauncherForActivityResult
        }

        val safFallback = runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            uri.toString()
        }.getOrNull()

        onPicked(safFallback)
    }
}

/** Mime type array to launch [rememberPhotoPickerLauncher] with. */
fun photoPickerMimeTypes(): Array<String> = PHOTO_MIME_TYPES
