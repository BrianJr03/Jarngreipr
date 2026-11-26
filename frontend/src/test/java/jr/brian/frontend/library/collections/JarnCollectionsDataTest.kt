package jr.brian.frontend.library.collections

import android.app.Application
import androidx.room.Room
import jr.brian.frontend.library.art.LibraryVisibility
import jr.brian.frontend.library.art.gameKey
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.GameMerge
import jr.brian.frontend.library.db.LibraryDatabase
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.db.SystemEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnCollectionsDataTest {

    private lateinit var db: LibraryDatabase
    private lateinit var collections: JarnCollectionsData
    private val visibility = MutableStateFlow(LibraryVisibility())
    private var sourceId = 0L

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), LibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        collections = JarnCollectionsData(CollectionsRepository(db.collectionDao())) { visibility }
        db.systemDao().upsert(SystemEntity(id = "snes", displayName = "SNES"))
        db.systemDao().upsert(SystemEntity(id = "gba", displayName = "GBA"))
        sourceId = addSource("/roms")
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `games are listed A to Z ignoring case`() = runBlocking {
        val id = create("Mix")
        listOf("zelda.sfc" to "zelda", "Aladdin.sfc" to "Aladdin", "mario.sfc" to "mario").forEach { (path, name) ->
            collections.add(id, key(insert("snes", path, name)))
        }
        assertEquals(listOf("Aladdin", "mario", "zelda"), titles(id))
    }

    @Test
    fun `hidden games and hidden systems stay hidden and are not counted`() = runBlocking {
        val id = create("Mix")
        collections.add(id, key(insert("snes", "A.sfc", "A")))
        collections.add(id, key(insert("snes", "B.sfc", "B", hidden = true)))
        collections.add(id, key(insert("gba", "C.gba", "C")))
        assertEquals(listOf("A", "C"), titles(id))

        visibility.value = LibraryVisibility(hiddenSystems = setOf("gba"))
        assertEquals(listOf("A"), titles(id))
        assertEquals(1, collections.games(id).first().hiddenCount)
        assertEquals(1, collections.collections.first().single().gameCount)
    }

    @Test
    fun `a missing ROM stays a member and comes back after a rescan`() = runBlocking {
        val id = create("Mix")
        val keep = insert("snes", "A.sfc", "A")
        collections.add(id, key(keep))
        collections.add(id, key(insert("snes", "B.sfc", "B")))

        db.gameDao().reconcileSystemGames(sourceId, "snes", listOf(keep), removeMissing = true, keepImportedMetadata = true)
        assertEquals(listOf("A"), titles(id))
        assertEquals(1, collections.collections.first().single().gameCount)

        val back = keep.copy(id = 0L, relativePath = "B.sfc", displayName = "B")
        db.gameDao().reconcileSystemGames(sourceId, "snes", listOf(keep, back), removeMissing = true, keepImportedMetadata = true)
        assertEquals(listOf("A", "B"), titles(id))
    }

    @Test
    fun `membership survives an overlap merge and a game seen twice is listed once`() = runBlocking {
        val id = create("Mix")
        val outer = insert("snes", "A.sfc", "A")
        val inner = insert("snes", "A.sfc", "A", source = addSource("/roms/snes"))
        collections.add(id, key(outer))
        assertEquals(listOf("A"), titles(id))
        assertEquals(1, collections.collections.first().single().gameCount)

        db.gameDao().mergeDuplicates(listOf(GameMerge(keep = inner, dropIds = listOf(outer.id))))
        assertEquals(listOf("A"), titles(id))
        assertEquals(setOf(id), collections.collectionsOf(key(inner)).first())
    }

    @Test
    fun `deleting a collection removes its members`() = runBlocking {
        val id = create("Mix")
        collections.add(id, key(insert("snes", "A.sfc", "A")))
        assertTrue(collections.delete(id))
        assertTrue(db.collectionDao().getAllMembers().isEmpty())
        assertFalse(collections.delete(id))
    }

    @Test
    fun `names are unique ignoring case`() = runBlocking {
        create("Party")
        assertEquals(CollectionNameResult.Duplicate, collections.create("pARTY"))
        assertEquals(CollectionNameResult.Duplicate, collections.create("  party "))
        create("Ärger")
        assertEquals(CollectionNameResult.Duplicate, collections.create("ärger"))
    }

    @Test
    fun `a rename may change case but not take another name`() = runBlocking {
        val party = create("Party")
        create("RPGs")
        assertEquals(CollectionNameResult.Saved(party), collections.rename(party, "PARTY"))
        assertEquals(CollectionNameResult.Duplicate, collections.rename(party, "rpgs"))
        assertEquals(CollectionNameResult.NotFound, collections.rename(999L, "New"))
        assertEquals(listOf("PARTY", "RPGs"), collections.collections.first().map { it.name })
    }

    @Test
    fun `names are trimmed and limited to 40 characters`() = runBlocking {
        val longest = "a".repeat(COLLECTION_NAME_MAX_LENGTH)
        assertTrue(collections.create("  $longest  ") is CollectionNameResult.Saved)
        assertEquals(longest, collections.collections.first().single().name)
        assertEquals(CollectionNameResult.TooLong, collections.create("b".repeat(COLLECTION_NAME_MAX_LENGTH + 1)))
        assertEquals(CollectionNameResult.Blank, collections.create("   "))
    }

    @Test
    fun `one game can be in many collections`() = runBlocking {
        val game = key(insert("snes", "A.sfc", "A"))
        val first = create("One")
        val second = create("Two")
        assertTrue(collections.add(first, game))
        assertTrue(collections.add(second, game))
        assertTrue(collections.add(second, game))
        assertEquals(setOf(first, second), collections.collectionsOf(game).first())
        assertEquals(listOf(1, 1), collections.collections.first().map { it.gameCount })

        assertTrue(collections.remove(first, game))
        assertFalse(collections.remove(first, game))
        assertEquals(setOf(second), collections.collectionsOf(game).first())
    }

    @Test
    fun `adding to a deleted collection does nothing`() = runBlocking {
        val id = create("Gone")
        collections.delete(id)
        assertFalse(collections.add(id, key(insert("snes", "A.sfc", "A"))))
        assertFalse(collections.add(create("Here"), "not a key"))
    }

    @Test
    fun `collections keep the order they were made in`() = runBlocking {
        create("Zebra")
        create("Apple")
        assertEquals(listOf("Zebra", "Apple"), collections.collections.first().map { it.name })
    }

    private suspend fun create(name: String): Long =
        (collections.create(name) as CollectionNameResult.Saved).collectionId

    private suspend fun titles(id: Long) = collections.games(id).first().items.map { it.title }

    private suspend fun addSource(path: String) =
        db.sourceDao().insert(SourceEntity(treeUri = path, displayPath = path))

    private suspend fun insert(
        system: String,
        path: String,
        name: String,
        hidden: Boolean = false,
        source: Long = sourceId,
    ): GameEntity {
        val game = GameEntity(sourceId = source, systemId = system, relativePath = path, displayName = name, isHidden = hidden)
        return game.copy(id = db.gameDao().insert(game))
    }

    private fun key(game: GameEntity) = gameKey(game.sourceId, game.systemId, game.relativePath)
}
