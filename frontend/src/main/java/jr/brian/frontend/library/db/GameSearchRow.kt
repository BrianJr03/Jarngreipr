package jr.brian.frontend.library.db

/**
 * The few columns a title search needs, from [GameDao.observeSearchRows]. Descriptions and
 * other detail text stay out, so searching a very large library never loads them all.
 */
data class GameSearchRow(
    val id: Long,
    val systemId: String,
    val displayName: String,
    val relativePath: String,
    val absolutePath: String?,
)
