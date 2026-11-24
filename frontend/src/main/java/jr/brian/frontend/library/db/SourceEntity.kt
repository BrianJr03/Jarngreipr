package jr.brian.frontend.library.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user-added ROM folder — the Phase 2+ replacement for the three-way
 * split between `romsPaths`, SAF tree mappings, and `SystemFolderMapping`
 * described in the plan's problem #10.
 *
 * One row per added folder, keyed by its tree URI. The scanner reads each
 * source and distributes its games across systems; a single source can
 * contain many systems (ES-DE's `Roms/<systemId>/...` layout).
 *
 * @property id auto-generated row id. Use this as the FK target on
 *   [GameEntity] rather than the tree URI so a source rename doesn't
 *   orphan the games it scanned.
 * @property treeUri SAF `content://` tree URI. Unique — a given folder
 *   should only ever be present once in the user's library.
 * @property displayPath human-readable path (e.g. `/storage/emulated/0/Roms`
 *   or `SD:/ES-DE/Roms`). Shown in the Sources screen.
 * @property volumeId `primary` for internal storage, otherwise the SD
 *   card's volume id as returned by the StorageVolume API. Null when the
 *   source is `content://` only (SAF-exclusive, no file-path backing).
 * @property hasPersistedGrant whether we successfully called
 *   `takePersistableUriPermission` for this source. The scanner skips
 *   sources whose grant was revoked.
 * @property lastScannedAt epoch millis of the most recent successful
 *   full scan. 0 means never scanned.
 */
@Entity(
    tableName = "sources",
    indices = [Index(value = ["treeUri"], unique = true)],
)
data class SourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "treeUri") val treeUri: String,
    val displayPath: String,
    val volumeId: String? = null,
    val hasPersistedGrant: Boolean = true,
    val lastScannedAt: Long = 0L,
)
