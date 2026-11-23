package jr.brian.frontend.library.setup

import android.content.Context
import android.provider.DocumentsContract
import androidx.core.net.toUri
import jr.brian.frontend.library.LibraryState
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.systems.SystemCatalog
import jr.brian.frontend.library.systems.SystemDef
import java.io.File

private const val CONTENT_SCHEME = "content://"
private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
private const val PRIMARY_VOLUME = "primary"
private const val PRIMARY_PATH = "/storage/emulated/0"
private const val STORAGE_PATH = "/storage"

/**
 * What this package can really read, from live grants rather than the stored
 * flag, which a restored backup can carry over from another package.
 *
 * @property scannable has its own access, and no other readable source holds it.
 * @property covered sits inside a readable source, whose scan finds its games.
 * @property unreadable needs the user to pick a folder again.
 */
internal data class SourceAccess(
    val scannable: List<SourceEntity>,
    val covered: List<SourceEntity>,
    val unreadable: List<SourceEntity>,
)

internal fun classifySources(
    sources: List<SourceEntity>,
    grantedTrees: Set<String>,
    canReadPath: (String) -> Boolean,
): SourceAccess {
    val direct = sources.filter { source ->
        if (source.treeUri.startsWith(CONTENT_SCHEME)) {
            source.treeUri in grantedTrees
        } else {
            canReadPath(source.treeUri)
        }
    }
    val directUris = direct.mapTo(HashSet()) { it.treeUri }
    val order = sources.withIndex().associate { (index, source) -> source.treeUri to index }
    fun isHeld(source: SourceEntity): Boolean {
        val location = sourceLocation(source.treeUri) ?: return false
        return direct.any { other ->
            val otherLocation = sourceLocation(other.treeUri)
            when {
                other.treeUri == source.treeUri || otherLocation == null -> false
                otherLocation == location ->
                    source.treeUri !in directUris ||
                        order.getValue(other.treeUri) < order.getValue(source.treeUri)
                else -> locationContains(otherLocation, location)
            }
        }
    }
    val scannable = ArrayList<SourceEntity>()
    val covered = ArrayList<SourceEntity>()
    val unreadable = ArrayList<SourceEntity>()
    for (source in sources) {
        when {
            isHeld(source) -> covered += source
            source.treeUri in directUris -> scannable += source
            else -> unreadable += source
        }
    }
    return SourceAccess(scannable, covered, unreadable)
}

internal fun Context.grantedTrees(): Set<String> =
    contentResolver.persistedUriPermissions
        .filter { it.isReadPermission }
        .mapTo(HashSet()) { it.uri.toString() }

internal fun canReadFolder(path: String): Boolean =
    File(path).let { it.isDirectory && it.canRead() && it.list() != null }

/** The storage path of a file path or external storage tree, or null for other providers. */
internal fun sourceLocation(uri: String): String? {
    if (!uri.startsWith(CONTENT_SCHEME)) return uri.trimEnd('/')
    val parsed = uri.toUri()
    if (parsed.authority != EXTERNAL_STORAGE_AUTHORITY) return null
    val documentId = runCatching { DocumentsContract.getTreeDocumentId(parsed) }.getOrNull()
        ?: return null
    val volume = documentId.substringBefore(':', "")
    if (volume.isEmpty()) return null
    val base = if (volume == PRIMARY_VOLUME) PRIMARY_PATH else "$STORAGE_PATH/$volume"
    val relative = documentId.substringAfter(':').trim('/')
    return if (relative.isEmpty()) base else "$base/$relative"
}

/** True for the same folder or one inside it, never for a sibling sharing a prefix. */
internal fun locationContains(parent: String, child: String): Boolean =
    child == parent || child.startsWith("$parent/")

internal fun folderName(uri: String): String =
    (sourceLocation(uri) ?: uri).trimEnd('/').substringAfterLast('/')

/**
 * What a folder holds. System subfolders make it a library root, which the
 * existing scan already understands. Otherwise a folder named after a system
 * is that one system, reported through [singleSystem].
 */
internal data class FolderMatch(val systemIds: Set<String>, val singleSystem: SystemDef?)

internal fun detectSystems(
    ownName: String,
    childFolders: List<String>,
    catalog: SystemCatalog,
): FolderMatch {
    val children = childFolders.mapNotNullTo(LinkedHashSet()) { catalog.findByFolderAlias(it)?.id }
    if (children.isNotEmpty()) return FolderMatch(children, null)
    val own = ownName.takeIf { it.isNotBlank() }?.let(catalog::findByFolderAlias)
        ?: return FolderMatch(emptySet(), null)
    return FolderMatch(setOf(own.id), own)
}

/** What the Library should show while it has no games, or Ready once it does. */
sealed interface LibrarySetupState {
    data object Checking : LibrarySetupState
    data class Scanning(val systemsDone: Int, val systemsTotal: Int) : LibrarySetupState
    data object ScanFailed : LibrarySetupState
    data object NoSources : LibrarySetupState

    /** [nextTreeUri] opens the picker near the folder that lost access. */
    data class NeedsReconnect(
        val nextName: String,
        val nextTreeUri: String,
        val remaining: Int,
    ) : LibrarySetupState

    data object NoGamesFound : LibrarySetupState
    data object Ready : LibrarySetupState
}

enum class GameFolderStatus { Connected, InsideAnother, NeedsReconnect }

data class GameFolder(
    val name: String,
    val treeUri: String,
    val status: GameFolderStatus,
    val gameCount: Int,
)

internal fun SourceEntity.displayName(): String = sourceLocation(treeUri) ?: displayPath

internal fun reduceSetup(
    access: SourceAccess,
    library: LibraryState,
    hasGames: Boolean,
    hasPendingScan: Boolean,
): LibrarySetupState {
    val next = access.unreadable.firstOrNull()
    return when {
        library is LibraryState.Scanning ->
            LibrarySetupState.Scanning(library.systemsDone, library.systemsTotal)
        hasGames -> LibrarySetupState.Ready
        hasPendingScan -> LibrarySetupState.Checking
        library is LibraryState.Error -> LibrarySetupState.ScanFailed
        next != null -> LibrarySetupState.NeedsReconnect(
            nextName = next.displayName(),
            nextTreeUri = next.treeUri,
            remaining = access.unreadable.size,
        )
        access.scannable.isEmpty() && access.covered.isEmpty() -> LibrarySetupState.NoSources
        else -> LibrarySetupState.NoGamesFound
    }
}
