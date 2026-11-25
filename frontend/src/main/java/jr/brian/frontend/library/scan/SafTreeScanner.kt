package jr.brian.frontend.library.scan

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.frontend.library.setup.documentStoragePath
import jr.brian.frontend.library.systems.SystemCatalog
import jr.brian.frontend.library.systems.SystemDef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import javax.inject.Inject

/**
 * [LibraryScanner] backed by `DocumentsContract` projection queries.
 * Used for SAF-only sources (SD cards, user-granted tree URIs).
 *
 * ### Why this exists
 *
 * `:app`'s existing scanner walks SAF trees via
 * `DocumentFile.listFiles()` + per-child `name` / `isDirectory` /
 * `isFile` calls — three IPCs per discovered entry, times the roughly
 * N files on a 5000-ROM SD card. That alone accounts for the
 * SAF-is-slow symptom in the Phase plan's problem #5.
 *
 * This scanner issues **one** `ContentResolver.query` per directory,
 * asking for every column we need in the projection, so N files is
 * N / batch_size queries total (ContentProvider cursors stream).
 *
 * ### Rules
 *
 * Shares [ScanRules] with [FastWalkScanner] — SKIPPED_NAMES,
 * ALWAYS_SKIPPED_EXTENSIONS, WALK_MAX_DEPTH, directories-as-files all
 * apply identically. If a scenario works on one scanner it works on
 * the other.
 */
class SafTreeScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val catalog: SystemCatalog,
) : LibraryScanner {

    private val resolver: ContentResolver get() = context.contentResolver

    override suspend fun scanWithReport(
        sourceRoot: String,
        systems: Set<String>?,
    ): ScanReport = withContext(Dispatchers.IO) {
        val treeUri = runCatching { sourceRoot.toUri() }.getOrNull()
        if (treeUri == null) {
            Log.w(TAG, "Not a parseable tree URI: $sourceRoot")
            return@withContext ScanReport(roms = emptyList(), listingTrusted = false)
        }

        val filter = systems?.map { it.lowercase() }?.toSet()
        val rootDocId = runCatching {
            DocumentsContract.getTreeDocumentId(treeUri)
        }.getOrNull()
        if (rootDocId == null) {
            Log.w(TAG, "Could not resolve tree document id for $treeUri")
            return@withContext ScanReport(roms = emptyList(), listingTrusted = false)
        }

        val out = mutableListOf<ScannedRom>()
        val unreadable = mutableSetOf<String>()
        // Pass 1: list the root's children to find system folders.
        val systemCandidates = queryChildren(treeUri, rootDocId)
            ?: return@withContext ScanReport(roms = emptyList(), listingTrusted = false)
        for (child in systemCandidates) {
            if (!child.isDirectory) continue
            if (!ScanRules.shouldDescend(child.displayName)) continue
            val def = catalog.findByFolderAlias(child.displayName)
            if (def == null) {
                Log.d(TAG, "Skipping unknown system folder '${child.displayName}'")
                continue
            }
            if (filter != null && def.id.lowercase() !in filter) continue
            if (def.extensions.isEmpty() && def.folderGameMarkers.isEmpty()) continue
            walk(
                treeUri = treeUri,
                parentDocId = child.documentId,
                def = def,
                relativeSegments = emptyList(),
                out = out,
                unreadable = unreadable,
                depthRemaining = ScanRules.WALK_MAX_DEPTH,
            )
        }
        ScanReport(roms = out, unreadableSystems = unreadable)
    }

    /** Walks the tree as one system's folder, with the same rules as a system subfolder. */
    suspend fun scanSystemFolder(sourceRoot: String, def: SystemDef): ScanReport =
        withContext(Dispatchers.IO) {
            val treeUri = sourceRoot.toUri()
            val rootDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
                ?: return@withContext ScanReport(roms = emptyList(), listingTrusted = false)
            val out = mutableListOf<ScannedRom>()
            val unreadable = mutableSetOf<String>()
            if (def.extensions.isNotEmpty() || def.folderGameMarkers.isNotEmpty()) {
                walk(
                    treeUri = treeUri,
                    parentDocId = rootDocId,
                    def = def,
                    relativeSegments = emptyList(),
                    out = out,
                    unreadable = unreadable,
                    depthRemaining = ScanRules.WALK_MAX_DEPTH,
                )
            }
            ScanReport(roms = out, unreadableSystems = unreadable)
        }

    private fun walk(
        treeUri: Uri,
        parentDocId: String,
        def: SystemDef,
        relativeSegments: List<String>,
        out: MutableList<ScannedRom>,
        unreadable: MutableSet<String>,
        depthRemaining: Int,
    ) {
        val systemId = def.id
        val extensions = def.extensions
        if (depthRemaining < 0) return
        val children = queryChildren(treeUri, parentDocId)
        if (children == null) {
            unreadable += systemId
            return
        }
        for (child in children) {
            val name = child.displayName
            if (!ScanRules.shouldDescend(name)) continue
            val childSegments = relativeSegments + name

            if (child.isDirectory) {
                val isMarkedGame = hasAnyMarker(treeUri, child.documentId, def)
                if (isMarkedGame == null) {
                    unreadable += systemId
                } else if (isMarkedGame) {
                    out += toScanned(treeUri, child, systemId, childSegments, isDirectoryAsFile = true)
                } else if (ScanRules.matchesExtension(name, extensions)) {
                    out += toScanned(treeUri, child, systemId, childSegments, isDirectoryAsFile = true)
                } else {
                    walk(
                        treeUri = treeUri,
                        parentDocId = child.documentId,
                        def = def,
                        relativeSegments = childSegments,
                        out = out,
                        unreadable = unreadable,
                        depthRemaining = depthRemaining - 1,
                    )
                }
            } else if (ScanRules.matchesExtension(name, extensions)) {
                out += toScanned(treeUri, child, systemId, childSegments, isDirectoryAsFile = false)
            }
        }
    }

    /**
     * Does [relativePath] (`base/exefs/main.npdm`) name a file under [dirDocId]?
     * One query per path segment, and only for systems that declare
     * [SystemDef.folderGameMarkers], so extension-only systems pay nothing.
     * Null when a folder on the way couldn't be read, which is not a "no".
     */
    private fun hasFile(treeUri: Uri, dirDocId: String, relativePath: String): Boolean? {
        val segments = relativePath.split('/')
        var docId = dirDocId
        for ((i, segment) in segments.withIndex()) {
            val isLast = i == segments.lastIndex
            val listing = queryChildren(treeUri, docId) ?: return null
            val match = listing
                .firstOrNull { it.displayName == segment && it.isDirectory != isLast }
                ?: return false
            docId = match.documentId
        }
        return true
    }

    /** True on the first marker found; null if none matched and one couldn't be read. */
    private fun hasAnyMarker(treeUri: Uri, dirDocId: String, def: SystemDef): Boolean? {
        var sawUnreadable = false
        for (marker in def.folderGameMarkers) {
            when (hasFile(treeUri, dirDocId, marker)) {
                true -> return true
                null -> sawUnreadable = true
                false -> Unit
            }
        }
        return if (sawUnreadable) null else false
    }

    private fun toScanned(
        treeUri: Uri,
        child: DocumentChild,
        systemId: String,
        segments: List<String>,
        isDirectoryAsFile: Boolean,
    ): ScannedRom {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, child.documentId)
        return ScannedRom(
            systemId = systemId,
            relativePath = segments.joinToString("/"),
            absolutePath = documentStoragePath(treeUri.authority, child.documentId),
            documentUri = docUri.toString(),
            isDirectoryAsFile = isDirectoryAsFile,
            fileSizeBytes = child.sizeBytes,
            lastModifiedEpochMillis = child.lastModified,
        )
    }

    /**
     * Null means the folder couldn't be fully listed (provider failure,
     * lost grant, or a provider still loading), as opposed to an empty
     * folder.
     */
    private fun queryChildren(treeUri: Uri, parentDocId: String): List<DocumentChild>? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        return try {
            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val extras = cursor.extras
                if (
                    extras?.getString(DocumentsContract.EXTRA_ERROR) != null ||
                    extras?.getBoolean(DocumentsContract.EXTRA_LOADING) == true
                ) {
                    Log.w(TAG, "SAF listing incomplete for $childrenUri")
                    return null
                }
                val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val mtimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val out = ArrayList<DocumentChild>(cursor.count)
                while (cursor.moveToNext()) {
                    val mime = cursor.getString(mimeIdx) ?: ""
                    out += DocumentChild(
                        documentId = cursor.getString(idIdx),
                        displayName = cursor.getString(nameIdx) ?: continue,
                        isDirectory = mime == DocumentsContract.Document.MIME_TYPE_DIR,
                        sizeBytes = if (cursor.isNull(sizeIdx)) -1L else cursor.getLong(sizeIdx),
                        lastModified = if (cursor.isNull(mtimeIdx)) 0L else cursor.getLong(mtimeIdx),
                    )
                }
                out
            }
        } catch (e: Exception) {
            Log.w(TAG, "SAF query failed for $childrenUri", e)
            null
        }
    }

    /**
     * Minimal projection of one row from `COLUMN_DOCUMENT_ID / NAME /
     * MIME_TYPE / SIZE / LAST_MODIFIED`. Rows missing a display name are
     * filtered out at the cursor loop since they can't be ROM-matched.
     */
    private data class DocumentChild(
        val documentId: String,
        val displayName: String,
        val isDirectory: Boolean,
        val sizeBytes: Long,
        val lastModified: Long,
    )

    private companion object {
        const val TAG = "SafTreeScanner"
    }
}
