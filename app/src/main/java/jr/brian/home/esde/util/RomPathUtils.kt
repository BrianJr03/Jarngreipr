package jr.brian.home.esde.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.addRomsPath
import jr.brian.home.esde.data.setSafTreeUri
import jr.brian.home.esde.model.GameInfo
import java.io.File

/** Case-fold + collapse variant dash characters so gamelist/disk naming quirks match. */
private fun normalizeName(name: String): String {
    val sb = StringBuilder(name.length)
    for (ch in name) sb.append(if (ch == '–' || ch == '—' || ch == '−') '-' else ch)
    return sb.toString().lowercase()
}

fun gameKey(game: GameInfo) = "${game.systemName}/${game.path}"
fun hiddenGameKey(game: GameInfo) = "${game.systemName}/${game.path}"

/**
 * Builds the ordered list of media roots the resolvers should search.
 * The primary (ES-DE) root always comes first; a non-blank secondary
 * (e.g. RetroHrai!) is appended when enabled. Duplicates are dropped so
 * a misconfigured secondary pointing at the same dir doesn't double the work.
 */
fun mediaRoots(primary: String, secondary: String?): List<String> =
    listOfNotNull(primary, secondary?.takeIf { it.isNotBlank() })
        .map { it.trimEnd('/') }
        .distinct()

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
 * Lazily yields candidate scraped-media files for `gameFilename` under each media root,
 * walking [mediaPaths] × [systemNames] × [mediaBasenameCandidates] × [suffixVariants] ×
 * [extensions] in preference order. Roots are the outermost loop so a first-root hit wins.
 * Pass [suffixVariants] like `listOf("", ".scummvm")` for ScummVM-style sidecar names.
 */
fun mediaCandidatePaths(
    mediaPaths: List<String>,
    systemNames: List<String>,
    folder: String,
    gameFilename: String,
    extensions: List<String>,
    suffixVariants: List<String> = listOf("")
): Sequence<File> = sequence {
    val candidates = mediaBasenameCandidates(gameFilename)
    for (mediaPath in mediaPaths) {
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
}

/**
 * Returns the absolute path of the first existing scraped-media file across [folders]
 * and [mediaPaths] (first-root, first-folder hit wins), or null if none exist.
 */
fun findFirstMedia(
    mediaPaths: List<String>,
    systemNames: List<String>,
    folders: List<String>,
    gameFilename: String,
    extensions: List<String>,
    suffixVariants: List<String> = listOf("")
): String? = folders.firstNotNullOfOrNull { folder ->
    mediaCandidatePaths(mediaPaths, systemNames, folder, gameFilename, extensions, suffixVariants)
        .firstOrNull { it.exists() }
        ?.absolutePath
}

fun resolveRomPath(game: GameInfo, romsPaths: List<String>): String? {
    if (game.romAbsolutePath != null) return game.romAbsolutePath
    val allPaths = romsPaths + listOf("/storage/emulated/0/Roms")
    // The relative path is preserved verbatim: game.path is ES-DE's own entry
    // (e.g. `./Disc1/Game.iso`, `./Xenogears.m3u/Xenogears.m3u`). Reducing it
    // to the basename breaks multi-disc games under the "directories
    // interpreted as files" convention that mediaBasenameCandidates handles.
    val relativePath = game.path.removePrefix("./")
    val filename = File(game.path).name

    for (root in allPaths) {
        File(root, "${game.systemName}/$relativePath").let { if (it.exists()) return it.absolutePath }
        File(root, "${game.systemName}/$filename").let { if (it.exists()) return it.absolutePath }
        File(root, relativePath).let { if (it.exists()) return it.absolutePath }
        File(root, filename).let { if (it.exists()) return it.absolutePath }
        File(root).listFiles()
            ?.firstOrNull { it.isDirectory && it.name.equals(game.systemName, ignoreCase = true) }
            ?.let { systemDir ->
                File(systemDir, relativePath).let { if (it.exists()) return it.absolutePath }
                File(systemDir, filename).let { if (it.exists()) return it.absolutePath }
            }
    }

    // No candidate exists on disk. Under scoped storage, File.exists() can
    // report false for a path our process cannot stat even though the emulator
    // can open it via SAF — historically observed for PSP and PS2 folders. As
    // a last resort, return the primary-root canonical path so the caller can
    // build a SAF document URI from it. When even that is wrong, the URI
    // verification in RomGameLauncher.verifyReadable surfaces a clear failure.
    val primaryRoot = allPaths.firstOrNull() ?: return null
    return File(primaryRoot, "${game.systemName}/$relativePath").absolutePath
}

/**
 * The ROMs-root directory implied by picking [pickedDir] for [systemName].
 *
 * If the picked directory's own name matches the system, treat its parent as
 * the ROMs root — the user picked `Roms/ps2` and expected the launcher to
 * search under `Roms`. Otherwise take the picked directory verbatim — the
 * user picked `Roms` itself and expected the system subdirectory to be looked
 * up beneath it.
 */
fun deriveRomsRoot(pickedDir: String, systemName: String?): String {
    if (systemName == null) return pickedDir
    val picked = File(pickedDir)
    return if (picked.name.equals(systemName, ignoreCase = true)) {
        picked.parent ?: pickedDir
    } else {
        pickedDir
    }
}

/**
 * Persists a picked SAF tree URI for [systemName]:
 *  - takes a persistable read grant,
 *  - stores the URI as the system's per-system tree,
 *  - registers the ROMs root implied by [deriveRomsRoot] when the picked
 *    tree is on primary storage (the only case where the on-disk path is
 *    unambiguous).
 *
 * Consolidates the previously-duplicated OpenDocumentTree callbacks in
 * RomSearchResultsActivity and FrontEndActivity. Callers that only need
 * partial behaviour (e.g. deliberate picker in settings, no launch to chain
 * off) still use the whole function — the persisted grant and the derived
 * root should not drift.
 */
fun persistSafTreeForSystem(
    context: Context,
    esdePrefs: ESDEPreferencesManager,
    systemName: String?,
    treeUri: Uri,
) {
    context.contentResolver.takePersistableUriPermission(
        treeUri,
        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
    )
    val treeDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
    if (treeDocId?.startsWith("primary:") == true) {
        val rel = treeDocId.removePrefix("primary:")
        val pickedDir = "/storage/emulated/0/$rel"
        esdePrefs.addRomsPath(deriveRomsRoot(pickedDir, systemName))
    }
    if (systemName != null) {
        esdePrefs.setSafTreeUri(systemName, treeUri.toString())
    }
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
    getSafTreeUri: (String) -> String?,
    context: Context? = null
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
    val direct = try {
        DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
    } catch (_: Exception) {
        null
    }

    // Return the direct URI when we can't verify (no context), or when it resolves. Otherwise
    // probe the expected parent directory before falling back to a bounded tree walk —
    // handles case differences and typographic dashes between gamelist entries and on-disk
    // filenames without doing multi-second I/O on the launch tap for a large ROM tree.
    if (context == null || direct == null) return direct
    val exists = runCatching {
        context.contentResolver.query(direct, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)
            ?.use { it.moveToFirst() } == true
    }.getOrDefault(false)
    if (exists) return direct

    val target = normalizeName(File(romAbsPath).name)
    val root = DocumentFile.fromTreeUri(context, treeUri) ?: return direct

    val treeDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
    val expectedParent = expectedParentSegments(treeDocId, documentId)
    if (expectedParent != null) {
        findInDirectory(root, expectedParent, target)?.let { return it }
    }

    return findByNormalizedName(root, target, depthRemaining = SAF_WALK_MAX_DEPTH) ?: direct
}

/**
 * Depth cap for the fallback walk over the SAF tree. Real ROM libraries rarely nest
 * more than a couple of levels below the system folder (e.g. `System/Genre/Game.iso`
 * or `System/Multi-disc/Game.m3u/Disc1.bin`), so a small bound covers the observed
 * layouts without turning the launch tap into multi-second UI jank on a large tree.
 */
private const val SAF_WALK_MAX_DEPTH = 3

/**
 * Path segments from the tree root to the file's expected parent, computed by
 * stripping the tree's own document id off the constructed direct-child id.
 * Returns null when the direct id doesn't sit under the tree root (unusual, but
 * possible if the caller passed a mismatched treeUri).
 */
private fun expectedParentSegments(treeDocId: String?, documentId: String): List<String>? {
    if (treeDocId == null) return null
    if (!documentId.startsWith("$treeDocId/")) return null
    val relative = documentId.removePrefix("$treeDocId/")
    val parent = relative.substringBeforeLast('/', "")
    return if (parent.isEmpty()) emptyList() else parent.split('/')
}

private fun findInDirectory(
    root: DocumentFile,
    segments: List<String>,
    target: String,
): Uri? {
    var current: DocumentFile = root
    for (segment in segments) {
        val normalized = normalizeName(segment)
        val next = current.listFiles().firstOrNull {
            it.isDirectory && it.name?.let(::normalizeName) == normalized
        } ?: return null
        current = next
    }
    return current.listFiles().firstOrNull {
        it.isFile && it.name?.let(::normalizeName) == target
    }?.uri
}

private fun findByNormalizedName(dir: DocumentFile, target: String, depthRemaining: Int): Uri? {
    if (depthRemaining < 0) return null
    for (child in dir.listFiles()) {
        val name = child.name ?: continue
        if (child.isFile && normalizeName(name) == target) return child.uri
        if (child.isDirectory) {
            findByNormalizedName(child, target, depthRemaining - 1)?.let { return it }
        }
    }
    return null
}
