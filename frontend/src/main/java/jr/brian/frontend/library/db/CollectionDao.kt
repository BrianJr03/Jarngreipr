package jr.brian.frontend.library.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Collections and their members. Member reads join `games` on
 * `(systemId, relativePath)` and skip hidden rows, so a missing ROM or a
 * hidden game is not listed or counted. The user's hide rules are applied
 * on top by the caller.
 */
@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY position, name COLLATE NOCASE")
    fun observeAll(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections ORDER BY position, name COLLATE NOCASE")
    suspend fun getAll(): List<CollectionEntity>

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM collections")
    suspend fun nextPosition(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(collection: CollectionEntity): Long

    @Query("UPDATE collections SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String): Int

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("UPDATE collections SET position = :position WHERE id = :id")
    suspend fun setPosition(id: Long, position: Int): Int

    /** Positions become 0, 1, 2 in [ids] order, which also evens out older rows that all read 0. */
    @Transaction
    suspend fun setOrder(ids: List<Long>) {
        ids.forEachIndexed { position, id -> setPosition(id, position) }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addGame(member: CollectionGameEntity): Long

    @Query(
        """
        DELETE FROM collection_games
        WHERE collectionId = :collectionId AND systemId = :systemId AND relativePath = :relativePath
        """
    )
    suspend fun removeGame(collectionId: Long, systemId: String, relativePath: String): Int

    @Query(
        """
        SELECT g.* FROM games g
        JOIN collection_games cg ON cg.systemId = g.systemId AND cg.relativePath = g.relativePath
        WHERE cg.collectionId = :collectionId AND g.isHidden = 0
        ORDER BY g.displayName COLLATE NOCASE
        """
    )
    fun observeGames(collectionId: Long): Flow<List<GameEntity>>

    @Query(
        """
        SELECT cg.collectionId AS collectionId, g.systemId AS systemId,
            g.relativePath AS relativePath, g.absolutePath AS absolutePath
        FROM collection_games cg
        JOIN games g ON g.systemId = cg.systemId AND g.relativePath = cg.relativePath
        WHERE g.isHidden = 0
        """
    )
    fun observePresentMembers(): Flow<List<CollectionMemberRow>>

    /** Every member, including ones whose ROM is missing, for backups. */
    @Query("SELECT * FROM collection_games")
    suspend fun getAllMembers(): List<CollectionGameEntity>

    @Query("SELECT collectionId FROM collection_games WHERE systemId = :systemId AND relativePath = :relativePath")
    fun observeCollectionIdsOf(systemId: String, relativePath: String): Flow<List<Long>>
}
