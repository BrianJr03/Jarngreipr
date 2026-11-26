package jr.brian.jarnfe.library

import jr.brian.frontend.library.art.JarnLibraryData
import jr.brian.frontend.library.collections.CollectionNameResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Bridges the shared library's collections to the Library screens. */
internal class JarnCollectionDataSource(private val data: JarnLibraryData) : JarnCollectionSource {
    private val collections = data.collections

    override fun observeCollections(): Flow<JarnLibraryFeed<JarnCollection>> =
        collections.collections.map { list ->
            JarnLibraryFeed.ready(list.map { JarnCollection(it.id, it.name, it.gameCount) })
        }

    override fun observeCollectionGames(collectionId: Long): Flow<JarnLibraryFeed<JarnLibraryGame>> =
        data.withArt(collections.games(collectionId)).map { list ->
            JarnLibraryFeed.ready(list.items.map { it.toJarnGame() }, list.hiddenCount)
        }

    override fun observeGameCollections(gameId: String): Flow<Set<Long>> = collections.collectionsOf(gameId)

    override suspend fun createCollection(name: String): CollectionNameResult = collections.create(name)

    override suspend fun renameCollection(collectionId: Long, name: String): CollectionNameResult =
        collections.rename(collectionId, name)

    override suspend fun deleteCollection(collectionId: Long): Boolean = collections.delete(collectionId)

    override suspend fun moveCollection(collectionId: Long, delta: Int): Boolean =
        collections.move(collectionId, delta)

    override suspend fun addToCollection(collectionId: Long, gameId: String): Boolean =
        collections.add(collectionId, gameId)

    override suspend fun removeFromCollection(collectionId: Long, gameId: String): Boolean =
        collections.remove(collectionId, gameId)
}
