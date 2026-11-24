package jr.brian.frontend.library.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Resolved media paths for one game. One-to-one with [GameEntity] — the
 * media index writes exactly one row per game once resolution runs, so
 * widgets and the wallpaper can look up all art for a focused game in a
 * single DAO call without re-probing disk.
 *
 * Any field is null when no asset is available for that kind. All paths
 * are opaque strings — may be absolute file paths OR `content://` URIs
 * depending on where the source lives — and the renderer should feed
 * them straight to Coil without further parsing.
 *
 * The current `findFirstMedia` probe chain in `ESDEViewModel` is the
 * work this row is designed to make obsolete.
 */
@Entity(
    tableName = "media",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["gameId"], unique = true)],
)
data class MediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val gameId: Long,
    val coverPath: String? = null,
    val marqueePath: String? = null,
    val fanartPath: String? = null,
    val screenshotPath: String? = null,
    val titlescreenPath: String? = null,
    val miximagePath: String? = null,
    val physicalMediaPath: String? = null,
    val videoPath: String? = null,
)
