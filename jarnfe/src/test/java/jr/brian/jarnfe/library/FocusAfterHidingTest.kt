package jr.brian.jarnfe.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusAfterHidingTest {
    private fun game(id: String) = JarnLibraryGame(id = id, systemId = "gba", title = id)

    private fun feed(vararg ids: String) = JarnLibraryFeed.ready(ids.map(::game))

    @Test
    fun `the next game takes the hidden one's place`() {
        assertEquals("b", neighbourOf(listOf("a", "b", "c"), "a"))
        assertEquals("c", neighbourOf(listOf("a", "b", "c"), "b"))
    }

    @Test
    fun `the last game falls back to the one before it`() {
        assertEquals("b", neighbourOf(listOf("a", "b", "c"), "c"))
    }

    @Test
    fun `no neighbour for the only game or a game not in the list`() {
        assertNull(neighbourOf(listOf("a"), "a"))
        assertNull(neighbourOf(listOf("a", "b"), "z"))
    }

    @Test
    fun `hiding moves the open system, favorites and recent focus to the neighbour`() {
        val state = JarnLibraryState(
            gamesBySystem = mapOf("gba" to feed("balatro", "bomberman", "kirby"), "snes" to feed("zelda")),
            favorites = feed("kirby", "balatro"),
            recent = feed("zelda"),
            openSystemId = "gba",
            focusedGameBySystem = mapOf("gba" to "balatro", "snes" to "zelda"),
            focusedFavoriteId = "balatro",
            focusedRecentId = "zelda",
        )

        val after = state.focusAfterHiding("balatro")

        assertEquals("bomberman", after.focusedGameBySystem["gba"])
        assertEquals("zelda", after.focusedGameBySystem["snes"])
        assertEquals("kirby", after.focusedFavoriteId)
        assertEquals("zelda", after.focusedRecentId)
    }
}
