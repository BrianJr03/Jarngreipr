package jr.brian.frontend.library.db

/** Query projection of [GameDao.observeSourceCounts]. Not a table. */
data class SourceGameCount(val sourceId: Long, val systemId: String, val gameCount: Int)
