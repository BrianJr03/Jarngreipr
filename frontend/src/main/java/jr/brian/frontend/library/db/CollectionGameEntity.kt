package jr.brian.frontend.library.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * One game in one collection, keyed like play stats by `(systemId, relativePath)`
 * rather than `games.id` or the source. Rescans and overlap merges delete and
 * re-create game rows, and a restore can land the same ROM under a new source,
 * so membership has no foreign key to `games`. A member whose ROM is gone stays
 * here and is listed again once a scan finds it.
 */
@Entity(
    tableName = "collection_games",
    primaryKeys = ["collectionId", "systemId", "relativePath"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["systemId", "relativePath"])],
)
data class CollectionGameEntity(
    val collectionId: Long,
    val systemId: String,
    val relativePath: String,
    val addedAt: Long,
)
