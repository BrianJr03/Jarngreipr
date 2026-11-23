package jr.brian.frontend.library.art

import android.os.Environment
import jr.brian.core.model.ApplicationScope
import jr.brian.core.model.ArtSource
import jr.brian.frontend.library.scan.ScanRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** [Unknown] means the folder could not be read, so it may still hold media. */
internal enum class ArtMediaPresence { HasMedia, Empty, Unknown }

/** ES-DE wins, so RetroHrai is only picked once ES-DE is known to have nothing. */
internal fun decideArtSource(esDe: ArtMediaPresence, retroHrai: ArtMediaPresence): ArtSource? =
    when {
        esDe == ArtMediaPresence.HasMedia -> ArtSource.EsDe
        esDe == ArtMediaPresence.Unknown -> null
        retroHrai == ArtMediaPresence.HasMedia -> ArtSource.RetroHrai
        else -> null
    }

/** A listing that found files proves media exists, even when it may be incomplete. */
internal fun mediaPresence(
    fileCount: Int,
    roots: List<File>,
    hasAllFilesAccess: () -> Boolean,
): ArtMediaPresence = when {
    roots.isEmpty() -> ArtMediaPresence.Empty
    fileCount > 0 -> ArtMediaPresence.HasMedia
    roots.any { rootPresence(it, hasAllFilesAccess) == ArtMediaPresence.Unknown } ->
        ArtMediaPresence.Unknown
    else -> ArtMediaPresence.Empty
}

private fun rootPresence(root: File, hasAllFilesAccess: () -> Boolean): ArtMediaPresence = when {
    !ScanRules.isListingTrusted(root.path, hasAllFilesAccess) -> ArtMediaPresence.Unknown
    root.isDirectory ->
        if (root.list() == null) ArtMediaPresence.Unknown else ArtMediaPresence.Empty
    isOnMissingVolume(root) -> ArtMediaPresence.Unknown
    else -> ArtMediaPresence.Empty
}

/** An unmounted SD card looks the same as a folder that was never created. */
private fun isOnMissingVolume(root: File): Boolean {
    val segments = root.path.trim('/').split('/')
    if (segments.size < 2 || segments[0] != STORAGE_DIR) return false
    if (segments[1] in BUILT_IN_VOLUMES) return false
    return !File("/$STORAGE_DIR/${segments[1]}").isDirectory
}

/** Whether [root] is [other] or a folder inside it. */
private fun isWithin(root: File, other: File): Boolean {
    val path = root.path.trimEnd('/')
    val parent = other.path.trimEnd('/')
    return path == parent || path.startsWith("$parent/")
}

private const val STORAGE_DIR = "storage"
private val BUILT_IN_VOLUMES = setOf("emulated", "self")

/**
 * Picks the first art source for someone who never chose one. Saved once, so
 * it cannot flip later, and never replaces a pick or a restored backup.
 */
@Singleton
class ArtSourceDetector internal constructor(
    private val indexes: ArtIndexes,
    private val roots: ArtRoots,
    private val store: ArtSourceStore,
    private val scope: CoroutineScope,
    private val hasAllFilesAccess: () -> Boolean,
) {
    @Inject
    constructor(
        indexes: ArtIndexes,
        roots: ArtRoots,
        store: ArtSourceStore,
        @ApplicationScope scope: CoroutineScope,
    ) : this(indexes, roots, store, scope, { Environment.isExternalStorageManager() })

    private val mutex = Mutex()
    private var lastAccess: Boolean? = null

    /** The source picked here while it is still in use. Null after any user or backup choice. */
    val detected: StateFlow<ArtSource?> = store.detectedState

    /** Safe to call often: once a source is saved this returns without touching the disk. */
    fun check(mediaChanged: Boolean = false) {
        if (store.savedSource != null) return
        scope.launch(Dispatchers.IO) { detect(mediaChanged) }
    }

    internal suspend fun detect(mediaChanged: Boolean = false) = mutex.withLock {
        if (store.savedSource != null) return@withLock
        val access = hasAllFilesAccess()
        val accessChanged = lastAccess != null && lastAccess != access
        lastAccess = access
        if (mediaChanged || accessChanged) indexes.invalidate()
        val built = indexes.current()
        val esDe = mediaPresence(
            fileCount = built[ArtSource.EsDe]?.fileCount ?: 0,
            roots = realEsDeRoots(),
            hasAllFilesAccess = hasAllFilesAccess,
        )
        val retroHrai = mediaPresence(
            fileCount = built[ArtSource.RetroHrai]?.fileCount ?: 0,
            roots = roots.rootsFor(ArtSource.RetroHrai),
            hasAllFilesAccess = hasAllFilesAccess,
        )
        decideArtSource(esDe, retroHrai)?.let(store::saveDetected)
        Unit
    }

    /** The ES-DE path can be pointed at another source's folder, which is not ES-DE media. */
    private fun realEsDeRoots(): List<File> {
        val others = roots.rootsFor(ArtSource.RetroHrai) + roots.rootsFor(ArtSource.Scraped)
        return roots.rootsFor(ArtSource.EsDe).filterNot { root ->
            others.any { isWithin(root, it) }
        }
    }
}
