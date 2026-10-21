package jr.brian.frontend.library.db

/**
 * Thin projection returned by [GameDao.observeAllVisibleWithMedia].
 * Only the columns the launcher's browse / search UI consumes,
 * joined server-side so a single cursor read replaces the previous
 * `combine(allGames, mediaDao.observeAll())` which materialised two
 * full tables.
 *
 * Fields the detail view reads but the list / search don't —
 * `description`, `releaseDate`, `players` — are deliberately absent.
 * They dominated per-row cursor memory (descriptions in particular
 * are hundreds of chars per game and multiply by every row in the
 * library) and had no readers in the browse grid, so holding them
 * in the all-games List was pure heap pressure. Detail-only readers
 * should fetch them via a per-id query instead.
 *
 * `developer`, `publisher`, and `genre` stay in the projection
 * because `rememberFilteredGames` and `RomSearchResultsActivity`
 * both filter the full list by these fields when the user types a
 * search query, so dropping them would break those searches.
 *
 * Field names match the aliased SELECT columns verbatim so Room's
 * default name-based mapping handles the hydration without extra
 * `@ColumnInfo` plumbing.
 */
data class GameInfoRow(
    val gameId: Long,
    val systemId: String,
    val absolutePath: String?,
    val relativePath: String,
    val displayName: String,
    val rating: Float?,
    val developer: String?,
    val publisher: String?,
    val genre: String?,
    val isFavorite: Boolean,
    val playCount: Int,
    val playTimeMinutes: Int,
    val lastPlayed: Long?,
    val emulatorPackage: String?,
    val launchCommand: String?,
    val coverPath: String?,
    val physicalMediaPath: String?,
    val marqueePath: String?,
    val screenshotPath: String?,
    val fanartPath: String?,
    val titlescreenPath: String?,
    val miximagePath: String?,
)
