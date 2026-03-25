package jr.brian.home.esde.util

import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import jr.brian.home.esde.model.GameInfo
import java.io.File

fun gameKey(game: GameInfo) = "${game.systemName}/${game.path}"
fun hiddenGameKey(game: GameInfo) = "${game.systemName}/${game.path}"

fun resolveRomPath(game: GameInfo, romsPaths: List<String>): String? {
    if (game.romAbsolutePath != null) return game.romAbsolutePath
    val allPaths = romsPaths + listOf("/storage/emulated/0/Roms")
    val filename = File(game.path).name

    // PSP and PS2: build path directly — no existence check, since File.exists() can
    // return false for these paths even when the file is there (permission timing).
    if (game.systemName.equals("psp", ignoreCase = true) ||
        game.systemName.equals("ps2", ignoreCase = true)
    ) {
        val root = allPaths.firstOrNull() ?: "/storage/emulated/0/Roms"
        return File(root, "${game.systemName}/$filename").absolutePath
    }

    for (root in allPaths) {
        File(root, "${game.systemName}/${game.path}").let { if (it.exists()) return it.absolutePath }
        File(root, "${game.systemName}/$filename").let { if (it.exists()) return it.absolutePath }
        File(root, game.path).let { if (it.exists()) return it.absolutePath }
        File(root, filename).let { if (it.exists()) return it.absolutePath }
        File(root).listFiles()
            ?.firstOrNull { it.isDirectory && it.name.equals(game.systemName, ignoreCase = true) }
            ?.let { systemDir ->
                File(systemDir, game.path).let { if (it.exists()) return it.absolutePath }
                File(systemDir, filename).let { if (it.exists()) return it.absolutePath }
            }
    }
    return null
}

fun sdCardVolumeId(absolutePath: String): String? {
    val withoutStorage = absolutePath.removePrefix("/storage/")
    val volumeId = withoutStorage.substringBefore('/')
    return if (volumeId == "emulated" || volumeId.isEmpty()) null else volumeId
}

fun buildSafDocumentUri(
    absolutePath: String,
    volumeId: String,
    systemName: String,
    getSafTreeUri: (String) -> String?
): Uri? {
    val treeUriString = getSafTreeUri(systemName) ?: return null
    val treeUri = treeUriString.toUri()
    val relativePath = absolutePath.removePrefix("/storage/$volumeId/")
    val documentId = "$volumeId:$relativePath"
    return DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
}

fun buildAetherDocUri(
    systemName: String,
    romAbsPath: String,
    getSafTreeUri: (String) -> String?
): Uri? {
    val treeUriStr = getSafTreeUri(systemName) ?: return null
    val treeUri = treeUriStr.toUri()
    val documentId = when {
        romAbsPath.startsWith("/storage/emulated/0/") ->
            "primary:${romAbsPath.removePrefix("/storage/emulated/0/")}"
        else -> {
            val volId = sdCardVolumeId(romAbsPath) ?: return null
            "$volId:${romAbsPath.removePrefix("/storage/$volId/")}"
        }
    }
    return try {
        DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
    } catch (_: Exception) {
        null
    }
}
