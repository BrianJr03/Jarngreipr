package jr.brian.home.esde.ui.frontend

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import jr.brian.home.esde.model.PlatformImageFolderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "webp")

@Composable
fun rememberPlatformImageMap(
    enabled: Boolean,
    folderUri: String?,
    folderType: PlatformImageFolderType
): State<Map<String, Uri>> {
    val context = LocalContext.current
    val state = remember { mutableStateOf<Map<String, Uri>>(emptyMap()) }

    LaunchedEffect(enabled, folderUri, folderType) {
        if (!enabled || folderUri == null) {
            state.value = emptyMap()
            return@LaunchedEffect
        }
        state.value = withContext(Dispatchers.IO) {
            runCatching {
                val treeUri = folderUri.toUri()
                val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                    ?: return@runCatching emptyMap()
                when (folderType) {
                    PlatformImageFolderType.Smart -> rootDoc.smartFolderImages()
                    PlatformImageFolderType.Default -> rootDoc.flatFolderImages()
                }
            }.getOrDefault(emptyMap())
        }
    }

    return state
}

private fun DocumentFile.smartFolderImages(): Map<String, Uri> =
    listFiles()
        .filter { it.isDirectory }
        .mapNotNull { dir ->
            val image = dir.listFiles().firstOrNull { it.isImageFile() }
            val name = dir.name?.lowercase()
            if (image != null && name != null) name to image.uri else null
        }
        .toMap()

private fun DocumentFile.flatFolderImages(): Map<String, Uri> =
    listFiles()
        .filter { it.isFile }
        .mapNotNull { file ->
            val name = file.name?.substringBeforeLast(".")?.lowercase()
            if (name != null) name to file.uri else null
        }
        .toMap()

private fun DocumentFile.isImageFile(): Boolean =
    isFile && name?.substringAfterLast(".", "")?.lowercase() in IMAGE_EXTENSIONS
