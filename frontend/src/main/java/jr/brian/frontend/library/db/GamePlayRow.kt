package jr.brian.frontend.library.db

/** The columns needed to total a system's activity after the user's hide rules. */
data class GamePlayRow(
    val systemId: String,
    val absolutePath: String?,
    val relativePath: String,
    val lastPlayed: Long?,
    val playTimeMinutes: Int,
    val isFavorite: Boolean,
)
