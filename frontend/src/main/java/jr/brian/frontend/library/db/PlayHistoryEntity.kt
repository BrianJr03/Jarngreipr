package jr.brian.frontend.library.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One recorded play session. The frontend writes a row on every
 * `GameStarted` event from the launch flow, and updates [durationSeconds]
 * when the matching `GameEnded` arrives.
 *
 * Lets the Recent / Most-played collections (plan §3.2 `ui/home/`) read
 * directly from Room instead of maintaining a parallel `playCount` map
 * in `ESDEPrefsState`. The matching aggregate fields on [GameEntity]
 * (`playCount`, `playTimeMinutes`, `lastPlayed`) are kept in sync by
 * the scanner / repository so queries that don't need per-session
 * granularity avoid the join.
 */
@Entity(
    tableName = "play_history",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["gameId"]),
        Index(value = ["startedAt"]),
    ],
)
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val gameId: Long,
    val startedAt: Long,
    val durationSeconds: Int? = null,
)
