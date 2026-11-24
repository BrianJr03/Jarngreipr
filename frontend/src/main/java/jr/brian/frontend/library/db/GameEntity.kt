package jr.brian.frontend.library.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One scanned ROM. Mirrors the current `GameInfo` text metadata (so the
 * ES-DE gamelist importer in Phase 2d can land into the same row shape)
 * plus the per-game user flags (`isFavorite`, `isHidden`, play stats,
 * emulator / launch overrides) that currently live scattered across
 * `ESDEPrefsState` maps.
 *
 * ### Identity
 *
 * The `(sourceId, systemId, relativePath)` triple is the stable identity
 * — same ROM moved between sources gets a new row, which is correct
 * because the user-facing launch flow depends on which source grant we
 * resolve against. A unique index enforces this; a rescan that sees the
 * same triple updates in place.
 *
 * ### Scraping fingerprint
 *
 * [crc32] / [md5] / [fileSizeBytes] exist so ScreenScraper hash-first
 * matching (plan §3.7) can look up a game without re-hashing. They're
 * populated lazily — a game scanned before hashing was added just has
 * them null.
 */
@Entity(
    tableName = "games",
    foreignKeys = [
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SystemEntity::class,
            parentColumns = ["id"],
            childColumns = ["systemId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["sourceId", "systemId", "relativePath"], unique = true),
        Index(value = ["systemId"]),
        Index(value = ["isFavorite"]),
    ],
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sourceId: Long,
    val systemId: String,
    val relativePath: String,
    val absolutePath: String? = null,
    val displayName: String,
    val description: String? = null,
    val rating: Float? = null,
    val releaseDate: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val genre: String? = null,
    val players: String? = null,
    @ColumnInfo(defaultValue = "0") val isFavorite: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isHidden: Boolean = false,
    @ColumnInfo(defaultValue = "0") val playCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val playTimeMinutes: Int = 0,
    val lastPlayed: Long? = null,
    val crc32: String? = null,
    val md5: String? = null,
    val fileSizeBytes: Long? = null,
    /** Per-game emulator override, overrides [SystemEntity]'s default. */
    val emulatorPackage: String? = null,
    /** Saved ES-DE command string if the user picked one; null to use the system default. */
    val launchCommand: String? = null,
    /** Per-game emulator core override (RetroArch). */
    val emulatorCore: String? = null,
)
