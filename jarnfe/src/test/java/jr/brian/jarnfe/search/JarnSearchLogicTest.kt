package jr.brian.jarnfe.search

import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.library.JarnLibrarySystem
import jr.brian.jarnfe.library.LibrarySelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnSearchLogicTest {
    private val snes = JarnLibrarySystem("snes", "Super Nintendo", 2)
    private val psx = JarnLibrarySystem("psx", "PlayStation", 1)
    private val systems = listOf(snes, psx)
    private val games = listOf(
        JarnLibraryGame("1", "snes", "Super Metroid"),
        JarnLibraryGame("2", "snes", "Metroid Fusion"),
        JarnLibraryGame("3", "psx", "Pokémon Stadium"),
    )

    private fun find(raw: String, scope: String? = null): List<String> {
        val query = parseJarnSearchQuery(raw)
        val allowed = allowedSystemIds(query, systems, scope)
        return searchGames(games, query, allowed, 10, JarnLibraryGame::systemId, JarnLibraryGame::title)
            .map { it.id }
    }

    @Test
    fun `prefix matches rank above later words`() {
        assertEquals(listOf("2", "1"), find("metroid"))
    }

    @Test
    fun `accents and case are ignored`() {
        assertEquals(listOf("3"), find("POKEMON"))
    }

    @Test
    fun `every term must match`() {
        assertEquals(listOf("1"), find("metroid super"))
        assertTrue(find("metroid zelda").isEmpty())
    }

    @Test
    fun `a platform command filters by system id or title`() {
        assertEquals(listOf("3"), find("@playstation"))
        assertEquals(listOf("2", "1"), find("@snes metroid"))
    }

    @Test
    fun `scope limits results and an empty query finds nothing`() {
        assertTrue(find("pokemon", scope = "snes").isEmpty())
        assertTrue(find("   ").isEmpty())
    }

    @Test
    fun `a command-only query has no title terms`() {
        val query = parseJarnSearchQuery("@snes")
        assertEquals(listOf("snes"), query.systemFilters)
        assertTrue(query.terms.isEmpty())
        assertNull(allowedSystemIds(parseJarnSearchQuery("mario"), systems, null))
    }

    @Test
    fun `an empty tab falls over to the one with matches`() {
        val onlySystems = JarnSearchResults(systems = listOf(snes))
        assertEquals(JarnSearchTab.Systems, onlySystems.visibleTab(JarnSearchTab.Games))
        assertEquals(LibrarySelection.System(snes), onlySystems.firstSelection(JarnSearchTab.Games))
    }
}
