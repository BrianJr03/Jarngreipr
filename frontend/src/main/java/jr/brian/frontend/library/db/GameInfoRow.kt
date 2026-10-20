package jr.brian.frontend.library.db

/**
 * Thin projection returned by [GameDao.observeAllVisibleWithMedia].
 * Only the columns the launcher's `toGameInfo` mapping consumes,
 * joined server-side so a single cursor read replaces the previous
 * `combine(allGames, mediaDao.observeAll())` which materialised two
 * full tables.
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
    val description: String?,
    val rating: Float?,
    val releaseDate: String?,
    val developer: String?,
    val publisher: String?,
    val genre: String?,
    val players: String?,
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
