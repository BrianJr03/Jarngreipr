package jr.brian.jarnfe.library

import androidx.compose.runtime.Immutable
import jr.brian.frontend.library.collections.CollectionNameResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** [gameCount] counts only games that are in the library and not hidden. */
@Immutable
data class JarnCollection(val id: Long, val name: String, val gameCount: Int)

/**
 * The user's collections. Games are named by [JarnLibraryGame.id]. Names are
 * trimmed and checked here, so the UI only shows the result. A game whose ROM
 * is missing stays a member but is not listed until it is back.
 */
interface JarnCollectionSource {
    /** In the user's order. */
    fun observeCollections(): Flow<JarnLibraryFeed<JarnCollection>>

    /** A to Z, after the user's hide rules. */
    fun observeCollectionGames(collectionId: Long): Flow<JarnLibraryFeed<JarnLibraryGame>>

    /** The ids of the collections holding [gameId], for the picker. Its size is the count. */
    fun observeGameCollections(gameId: String): Flow<Set<Long>>

    suspend fun createCollection(name: String): CollectionNameResult
    suspend fun renameCollection(collectionId: Long, name: String): CollectionNameResult

    /** False when it was already gone. */
    suspend fun deleteCollection(collectionId: Long): Boolean

    /** Moves it [delta] places in the user's order. False when nothing moved. */
    suspend fun moveCollection(collectionId: Long, delta: Int): Boolean

    /** True when the game is in the collection afterwards. */
    suspend fun addToCollection(collectionId: Long, gameId: String): Boolean

    /** False when the game wasn't in the collection. */
    suspend fun removeFromCollection(collectionId: Long, gameId: String): Boolean
}

/** Until a real library is connected: no collections, and nothing can be saved. */
object EmptyJarnCollectionSource : JarnCollectionSource {
    override fun observeCollections() = flowOf(JarnLibraryFeed.ready(emptyList<JarnCollection>()))
    override fun observeCollectionGames(collectionId: Long) =
        flowOf(JarnLibraryFeed.ready(emptyList<JarnLibraryGame>()))
    override fun observeGameCollections(gameId: String) = flowOf(emptySet<Long>())
    override suspend fun createCollection(name: String) = CollectionNameResult.NotFound
    override suspend fun renameCollection(collectionId: Long, name: String) = CollectionNameResult.NotFound
    override suspend fun deleteCollection(collectionId: Long) = false
    override suspend fun moveCollection(collectionId: Long, delta: Int) = false
    override suspend fun addToCollection(collectionId: Long, gameId: String) = false
    override suspend fun removeFromCollection(collectionId: Long, gameId: String) = false
}
