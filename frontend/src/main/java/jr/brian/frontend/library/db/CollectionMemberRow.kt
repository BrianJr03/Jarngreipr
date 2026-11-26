package jr.brian.frontend.library.db

/** Query projection of [CollectionDao.observePresentMembers]. Not a table. */
data class CollectionMemberRow(
    val collectionId: Long,
    val systemId: String,
    val relativePath: String,
    val absolutePath: String?,
)
