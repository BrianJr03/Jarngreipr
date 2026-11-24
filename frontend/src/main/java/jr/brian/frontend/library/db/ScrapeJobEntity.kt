package jr.brian.frontend.library.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One scrape attempt for a game — success or failure. Replaces the
 * fire-and-forget `catch { errors++ }` in `ScraperCoordinator` with
 * persistent per-game error records so a user who hits ScreenScraper
 * quota can see which titles failed and retry them specifically.
 *
 * [state] values:
 *  - `pending` — queued but not yet attempted.
 *  - `running` — currently in flight.
 *  - `succeeded` — media written for at least one requested kind.
 *  - `failed` — the lookup failed before any media was written. See
 *    [errorMessage].
 *  - `partial` — media written for some requested kinds but not others.
 *
 * [backend] identifies which API call this row records — `screenscraper`
 * or `steamgriddb`. A scrape job for one game commonly has one row per
 * backend because they're consulted in sequence.
 */
@Entity(
    tableName = "scrape_jobs",
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
        Index(value = ["state"]),
    ],
)
data class ScrapeJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val gameId: Long,
    val backend: String,
    val state: String,
    val errorMessage: String? = null,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val mediaKindsWritten: Int = 0,
)
