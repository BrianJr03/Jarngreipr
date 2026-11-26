package jr.brian.frontend.library.collections

import android.database.sqlite.SQLiteConstraintException
import jr.brian.frontend.library.db.CollectionDao
import jr.brian.frontend.library.db.CollectionEntity
import jr.brian.frontend.library.db.CollectionGameEntity
import jr.brian.frontend.library.db.CollectionMemberRow
import jr.brian.frontend.library.db.GameEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** A collection with every member, present or not. */
data class CollectionSnapshot(val collection: CollectionEntity, val members: List<CollectionGameEntity>)

/**
 * The user's collections. Games are named by `(systemId, relativePath)`, the
 * key that survives rescans and restores. Reads skip missing and hidden games;
 * the user's hide rules are applied by the caller.
 */
@Singleton
class CollectionsRepository @Inject constructor(private val dao: CollectionDao) {
    private val writes = Mutex()

    /** In the user's order. */
    val collections: Flow<List<CollectionEntity>> = dao.observeAll()

    /** One row per member whose game is in the library and not hidden. */
    val presentMembers: Flow<List<CollectionMemberRow>> = dao.observePresentMembers()

    /** A to Z, the same order as Favorites. */
    fun games(collectionId: Long): Flow<List<GameEntity>> = dao.observeGames(collectionId)

    /** The collections holding this game. Its size is the game's collection count. */
    fun collectionIdsOf(systemId: String, relativePath: String): Flow<Set<Long>> =
        dao.observeCollectionIdsOf(systemId, relativePath).map { it.toSet() }.distinctUntilChanged()

    suspend fun create(name: String): CollectionNameResult = writes.withLock {
        val trimmed = name.trim()
        collectionNameProblem(trimmed, dao.getAll())?.let { return it }
        val row = CollectionEntity(name = trimmed, createdAt = now(), position = dao.nextPosition())
        savedOrDuplicate { dao.insert(row) }
    }

    suspend fun rename(collectionId: Long, name: String): CollectionNameResult = writes.withLock {
        val trimmed = name.trim()
        val existing = dao.getAll()
        if (existing.none { it.id == collectionId }) return CollectionNameResult.NotFound
        collectionNameProblem(trimmed, existing, selfId = collectionId)?.let { return it }
        savedOrDuplicate { dao.rename(collectionId, trimmed).let { collectionId } }
    }

    /** Its members go with it. False when it was already gone. */
    suspend fun delete(collectionId: Long): Boolean = writes.withLock { dao.delete(collectionId) > 0 }

    /** Moves it [delta] places in the user's order. False when it is gone or already at that end. */
    suspend fun move(collectionId: Long, delta: Int): Boolean = writes.withLock {
        val order = collectionOrderMoved(dao.getAll().map { it.id }, collectionId, delta) ?: return false
        dao.setOrder(order)
        true
    }

    /** True when the game is in the collection afterwards. False when the collection is gone. */
    suspend fun addGame(collectionId: Long, systemId: String, relativePath: String): Boolean =
        writes.withLock {
            try {
                dao.addGame(CollectionGameEntity(collectionId, systemId, relativePath, addedAt = now()))
                true
            } catch (_: SQLiteConstraintException) {
                false
            }
        }

    /** False when the game wasn't in the collection. */
    suspend fun removeGame(collectionId: Long, systemId: String, relativePath: String): Boolean =
        writes.withLock { dao.removeGame(collectionId, systemId, relativePath) > 0 }

    /** Every collection and member, missing ROMs included, in the user's order. */
    suspend fun snapshot(): List<CollectionSnapshot> {
        val members = dao.getAllMembers().groupBy { it.collectionId }
        return dao.getAll().map { CollectionSnapshot(it, members[it.id].orEmpty()) }
    }

    /** The NOCASE index backs up the check above, for a write from outside this repository. */
    private inline fun savedOrDuplicate(write: () -> Long): CollectionNameResult = try {
        CollectionNameResult.Saved(write())
    } catch (_: SQLiteConstraintException) {
        CollectionNameResult.Duplicate
    }

    private fun now() = System.currentTimeMillis()
}
