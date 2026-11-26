package jr.brian.frontend.library.collections

import jr.brian.frontend.library.art.LibraryGame
import jr.brian.frontend.library.art.LibraryVisibilitySource
import jr.brian.frontend.library.art.VisibleList
import jr.brian.frontend.library.art.hides
import jr.brian.frontend.library.art.parseGameKey
import jr.brian.frontend.library.art.toLibraryGame
import jr.brian.frontend.library.db.GameEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/** [gameCount] counts only games that are in the library and not hidden. */
data class LibraryCollection(val id: Long, val name: String, val gameCount: Int)

/**
 * Collections for `:jarnfe`, with the user's hide rules applied. Games are
 * named by [LibraryGame.key]. A game found under two sources is listed once.
 */
@Singleton
class JarnCollectionsData @Inject constructor(
    private val repository: CollectionsRepository,
    private val visibilitySource: LibraryVisibilitySource,
) {
    /** In the user's order. */
    val collections: Flow<List<LibraryCollection>> = combine(
        repository.collections,
        repository.presentMembers,
        visibilitySource.visibility(),
    ) { collections, members, visibility ->
        val counts = members
            .filterNot { visibility.hides(it.systemId, it.absolutePath, it.relativePath) }
            .distinctBy { Triple(it.collectionId, it.systemId, it.relativePath) }
            .groupingBy { it.collectionId }
            .eachCount()
        collections.map { LibraryCollection(it.id, it.name, counts[it.id] ?: 0) }
    }.distinctUntilChanged()

    /** A to Z. [VisibleList.hiddenCount] is how many the hide rules removed. */
    fun games(collectionId: Long): Flow<VisibleList<LibraryGame>> =
        combine(repository.games(collectionId), visibilitySource.visibility()) { games, visibility ->
            val shown = games.filterNot { visibility.hides(it) }.onePerPath()
            VisibleList(shown.map { it.toLibraryGame() }, games.onePerPath().size - shown.size)
        }.distinctUntilChanged()

    /** The ids of the collections holding [gameKey]. Its size is the game's collection count. */
    fun collectionsOf(gameKey: String): Flow<Set<Long>> {
        val game = parseGameKey(gameKey) ?: return flowOf(emptySet())
        return repository.collectionIdsOf(game.systemId, game.relativePath)
    }

    suspend fun create(name: String): CollectionNameResult = repository.create(name)

    suspend fun rename(collectionId: Long, name: String): CollectionNameResult =
        repository.rename(collectionId, name)

    suspend fun delete(collectionId: Long): Boolean = repository.delete(collectionId)

    /** False when the key can't be read or the collection is gone. */
    suspend fun add(collectionId: Long, gameKey: String): Boolean {
        val game = parseGameKey(gameKey) ?: return false
        return repository.addGame(collectionId, game.systemId, game.relativePath)
    }

    suspend fun remove(collectionId: Long, gameKey: String): Boolean {
        val game = parseGameKey(gameKey) ?: return false
        return repository.removeGame(collectionId, game.systemId, game.relativePath)
    }

    private fun List<GameEntity>.onePerPath() = distinctBy { it.systemId to it.relativePath }
}
