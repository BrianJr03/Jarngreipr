package jr.brian.jarnfe.library

import jr.brian.frontend.library.collections.CollectionNameResult
import jr.brian.frontend.library.collections.collectionNameProblem
import jr.brian.frontend.library.db.CollectionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** In-memory collections with the real name rules. [games] is the library the members are looked up in. */
class FakeJarnCollectionSource(
    private val games: List<JarnLibraryGame> = emptyList(),
) : JarnCollectionSource {
    private val names = MutableStateFlow<List<CollectionEntity>>(emptyList())
    private val members = MutableStateFlow<Map<Long, Set<String>>>(emptyMap())
    private var nextId = 1L

    override fun observeCollections(): Flow<JarnLibraryFeed<JarnCollection>> =
        combine(names, members) { list, byId ->
            val present = games.mapTo(HashSet()) { it.id }
            JarnLibraryFeed.ready(list.map { JarnCollection(it.id, it.name, byId[it.id].orEmpty().count(present::contains)) })
        }

    override fun observeCollectionGames(collectionId: Long): Flow<JarnLibraryFeed<JarnLibraryGame>> =
        members.map { byId ->
            val ids = byId[collectionId].orEmpty()
            JarnLibraryFeed.ready(games.filter { it.id in ids }.sortedBy { it.title.lowercase() })
        }

    override fun observeGameCollections(gameId: String): Flow<Set<Long>> =
        members.map { byId -> byId.filterValues { gameId in it }.keys }

    override suspend fun createCollection(name: String): CollectionNameResult {
        val trimmed = name.trim()
        collectionNameProblem(trimmed, names.value)?.let { return it }
        val id = nextId++
        names.value += CollectionEntity(id = id, name = trimmed, createdAt = 0L, position = id.toInt())
        return CollectionNameResult.Saved(id)
    }

    override suspend fun renameCollection(collectionId: Long, name: String): CollectionNameResult {
        val trimmed = name.trim()
        if (names.value.none { it.id == collectionId }) return CollectionNameResult.NotFound
        collectionNameProblem(trimmed, names.value, collectionId)?.let { return it }
        names.value = names.value.map { if (it.id == collectionId) it.copy(name = trimmed) else it }
        return CollectionNameResult.Saved(collectionId)
    }

    override suspend fun deleteCollection(collectionId: Long): Boolean {
        if (names.value.none { it.id == collectionId }) return false
        names.value = names.value.filterNot { it.id == collectionId }
        members.value -= collectionId
        return true
    }

    override suspend fun addToCollection(collectionId: Long, gameId: String): Boolean {
        if (names.value.none { it.id == collectionId }) return false
        members.value += collectionId to (members.value[collectionId].orEmpty() + gameId)
        return true
    }

    override suspend fun removeFromCollection(collectionId: Long, gameId: String): Boolean {
        val current = members.value[collectionId].orEmpty()
        if (gameId !in current) return false
        members.value += collectionId to (current - gameId)
        return true
    }
}
