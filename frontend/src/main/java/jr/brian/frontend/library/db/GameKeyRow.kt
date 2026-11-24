package jr.brian.frontend.library.db

/** Query projection of [GameDao.observeVisibleKeys]. Not a table. */
data class GameKeyRow(val systemId: String, val absolutePath: String?, val relativePath: String)
