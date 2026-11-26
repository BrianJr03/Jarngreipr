package jr.brian.frontend.library.collections

import android.util.Log
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CollectionsBackup"

data class BackedUpCollectionMember(val systemId: String, val relativePath: String)

data class BackedUpCollection(
    val name: String,
    val position: Int,
    val members: List<BackedUpCollectionMember>,
)

/**
 * Saves and restores collections. A restore only adds: a collection whose name matches one here,
 * ignoring case, gets the backup's members, and the rest are made in the backup's order after
 * this device's own. Nothing here is renamed, reordered or removed. Members need no pending step,
 * since they are keyed by system and path with no tie to a scanned game, and show once a scan
 * finds the ROM.
 */
@Singleton
class CollectionsBackup @Inject constructor(private val repository: CollectionsRepository) {

    /** Null when collections could not be read, so a restore leaves the device's alone. */
    suspend fun export(): List<BackedUpCollection>? = guarded(fallback = null) {
        repository.snapshot().map { snapshot ->
            BackedUpCollection(
                name = snapshot.collection.name,
                position = snapshot.collection.position,
                members = snapshot.members
                    .sortedBy { it.addedAt }
                    .map { BackedUpCollectionMember(it.systemId, it.relativePath) },
            )
        }
    }

    /** An empty list adds nothing. A failure never fails the wider import. */
    suspend fun restore(collections: List<BackedUpCollection>) {
        guarded(fallback = Unit) {
            val existing = repository.snapshot().map { it.collection }
            collections.sortedBy { it.position }.forEach { backup ->
                val id = existing.firstOrNull { it.name.equals(backup.name.trim(), ignoreCase = true) }?.id
                    ?: created(backup.name)
                    ?: return@forEach
                backup.members
                    .filter { it.systemId.isNotBlank() && it.relativePath.isNotBlank() }
                    .forEach { repository.addGame(id, it.systemId, it.relativePath) }
            }
        }
    }

    /** Null for a name this device can't save, such as a blank one from a hand-edited backup. */
    private suspend fun created(name: String): Long? = when (val result = repository.create(name)) {
        is CollectionNameResult.Saved -> result.collectionId
        else -> {
            Log.w(TAG, "Skipped a backed up collection: $result")
            null
        }
    }

    private suspend fun <T> guarded(fallback: T, block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Collections backup step failed", e)
        fallback
    }
}
