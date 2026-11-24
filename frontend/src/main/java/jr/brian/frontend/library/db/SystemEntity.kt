package jr.brian.frontend.library.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One game system known to the library (e.g. `nes`, `snes`, `psx`).
 *
 * Mostly a projection of a `SystemCatalog` JSON entry (plan §3.2
 * `library/systems/`) into Room, so a system that disappears from the
 * shipped catalog can still produce correct results for games already
 * scanned against it. The catalog JSON remains authoritative for freshly
 * added systems; this row is the stable identity.
 *
 * @property id folder-alias canonical form (e.g. `nes`, `snes-msu1`).
 * @property displayName user-facing name shown in UI.
 * @property folderAliases lowercase folder names on disk that map to this
 *   system. The scanner uses `id` as the first alias; additional entries
 *   cover ES-DE renames (`nintendo-ds` ↔ `nds`, etc.).
 * @property extensions ROM extensions this system accepts, lowercase and
 *   without the leading dot. Resolved at scan time from the catalog JSON;
 *   empty means "fall through to the EmulatorRegistry transitive set".
 * @property screenScraperSystemId numeric id for ScreenScraper lookups,
 *   null when the system is not supported by ScreenScraper.
 * @property retroArchSystemId directory name under RetroArch's `system/`,
 *   null when the system has no bios/firmware requirements.
 */
@Entity(tableName = "systems")
data class SystemEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val folderAliases: List<String> = emptyList(),
    val extensions: Set<String> = emptySet(),
    val screenScraperSystemId: Int? = null,
    val retroArchSystemId: String? = null,
)
