package jr.brian.home.esde.util

import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import jr.brian.home.esde.model.GameInfo
import java.io.File

fun gameKey(game: GameInfo) = "${game.systemName}/${game.path}"
fun hiddenGameKey(game: GameInfo) = "${game.systemName}/${game.path}"

/**
 * Candidate (basename, parentDir) pairs to probe when locating scraped media.
 *
 * ES-DE's "Directories interpreted as files" convention (e.g. `Xenogears.m3u/`) means
 * the scraped media basename keeps the directory's extension (`Xenogears.m3u.png`), not
 * the stem (`Xenogears.png`) — see `Utils::FileSystem::getStem` in ES-DE, which skips
 * the extension strip when the path is a directory. Probing both layouts covers regular
 * files, directory-as-file entries where the gamelist records the directory path
 * (`./Xenogears.m3u`), and the inner-file variant (`./Xenogears.m3u/Xenogears.m3u`).
 */
fun mediaBasenameCandidates(gameFilename: String): List<Pair<String, String?>> {
    val file = File(gameFilename)
    val stem = file.nameWithoutExtension
    val fullName = file.name
    val parent = file.parent
    val parentName = file.parentFile?.name
    val grandparent = file.parentFile?.parent

    val candidates = mutableListOf<Pair<String, String?>>()
    candidates += stem to parent
    if (fullName != stem) candidates += fullName to parent
    if (parentName != null && parentName == fullName) candidates += parentName to grandparent
    return candidates.distinct()
}

/**
 * Lazily yields candidate scraped-media files for `gameFilename` under one media folder,
 * walking [systemNames] × [mediaBasenameCandidates] × [suffixVariants] × [extensions] in
 * preference order. Pass [suffixVariants] like `listOf("", ".scummvm")` for ScummVM-style
 * sidecar names.
 */
fun mediaCandidatePaths(
    mediaPath: String,
    systemNames: List<String>,
    folder: String,
    gameFilename: String,
    extensions: List<String>,
    suffixVariants: List<String> = listOf("")
): Sequence<File> = sequence {
    val candidates = mediaBasenameCandidates(gameFilename)
    for (sysName in systemNames) {
        for ((basename, parentDir) in candidates) {
            for (suffix in suffixVariants) {
                for (ext in extensions) {
                    if (parentDir != null) {
                        yield(File(mediaPath, "$sysName/$folder/$parentDir/$basename$suffix.$ext"))
                    }
                    yield(File(mediaPath, "$sysName/$folder/$basename$suffix.$ext"))
                }
            }
        }
    }
}

/**
 * Returns the absolute path of the first existing scraped-media file across [folders],
 * or null if none exist.
 */
fun findFirstMedia(
    mediaPath: String,
    systemNames: List<String>,
    folders: List<String>,
    gameFilename: String,
    extensions: List<String>,
    suffixVariants: List<String> = listOf("")
): String? = folders.firstNotNullOfOrNull { folder ->
    mediaCandidatePaths(mediaPath, systemNames, folder, gameFilename, extensions, suffixVariants)
        .firstOrNull { it.exists() }
        ?.absolutePath
}

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
