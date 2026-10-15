package jr.brian.frontend.library.systems

import kotlinx.serialization.Serializable

/**
 * One game system as described by a shipped `assets/systems/<id>.json`
 * file. Authoritative source for Phase 2+ catalog lookups; later phases
 * read from this instead of `EmulatorRegistry` for everything except
 * actual launch-intent plumbing (plan §Phase 4).
 *
 * ### Schema contract
 *
 * Field names are the on-disk JSON keys; renaming breaks every
 * community-maintained catalog file. Make additive changes only.
 * Optional fields default to null / empty so an older JSON authored
 * before a new field existed decodes cleanly.
 *
 * @property id canonical system id, lowercase. Matches the filename
 *   (e.g. `nes.json` carries `"id": "nes"`). The `SystemEntity.id` row
 *   in Room is populated from this value.
 * @property displayName user-visible name. Shown in the Systems grid
 *   and in settings; prefer the common English name of the hardware
 *   (`Nintendo 64`, not `n64`).
 * @property folderAliases lowercase folder names on disk that the
 *   scanner should treat as this system. The id itself is implicitly
 *   included; this list covers ES-DE renames (`nintendo-nes` ↔ `nes`),
 *   historic variants (`famicom` ↔ `nes`), and regional splits
 *   (`snesna` ↔ `snes`) without each needing a separate SystemDef.
 * @property extensions ROM extensions this system accepts, lowercase
 *   and without the leading dot. The scanner uses this set to decide
 *   whether a file in the folder is a ROM.
 * @property screenScraperSystemId numeric id for ScreenScraper's
 *   `jeuInfos.php?systemeid=...`. Null when ScreenScraper doesn't
 *   catalogue this system; the scraper falls back to name-only
 *   lookup for those.
 * @property retroArchSystemId directory name under RetroArch's
 *   `system/` for bios/firmware (`gba`, `psx`, etc). Null when the
 *   system has no bios requirement.
 */
@Serializable
data class SystemDef(
    val id: String,
    val displayName: String,
    val folderAliases: List<String> = emptyList(),
    val extensions: Set<String> = emptySet(),
    val screenScraperSystemId: Int? = null,
    val retroArchSystemId: String? = null,
)
