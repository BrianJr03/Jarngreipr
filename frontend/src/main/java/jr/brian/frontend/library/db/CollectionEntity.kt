package jr.brian.frontend.library.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A list of games the user made. [name] is unique ignoring ASCII case at the
 * database level; `CollectionsRepository` also checks full Unicode case.
 * [position] is the user's order of collections, lowest first.
 */
@Entity(
    tableName = "collections",
    indices = [Index(value = ["name"], unique = true)],
)
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "0") val position: Int = 0,
)
