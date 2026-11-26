package jr.brian.jarnfe.library

import jr.brian.frontend.library.collections.CollectionNameResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnCollectionSourceTest {
    private val games = listOf(JarnLibraryGame("b", "snes", "Bravo"), JarnLibraryGame("a", "snes", "alpha"))

    @Test
    fun `a library source without collections exposes none and saves nothing`() = runTest {
        val collections = EmptyJarnLibrarySource.collections
        assertTrue(collections.observeCollections().first().items.isEmpty())
        assertFalse(collections.addToCollection(1L, "a"))
    }

    @Test
    fun `the fake follows the name rules and lists games A to Z`() = runTest {
        val source = FakeJarnCollectionSource(games)
        val id = (source.createCollection(" Party ") as CollectionNameResult.Saved).collectionId
        assertEquals(CollectionNameResult.Duplicate, source.createCollection("party"))
        source.addToCollection(id, "b")
        source.addToCollection(id, "a")
        assertEquals(listOf("alpha", "Bravo"), source.observeCollectionGames(id).first().items.map { it.title })
        assertEquals(listOf(JarnCollection(id, "Party", 2)), source.observeCollections().first().items)
        assertEquals(setOf(id), source.observeGameCollections("a").first())
    }
}
