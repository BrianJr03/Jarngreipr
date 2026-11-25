package jr.brian.home.esde.launch

import jr.brian.frontend.library.db.GameEntity
import jr.brian.jarnfe.systems.JarnHiddenGame
import org.junit.Assert.assertEquals
import org.junit.Test

class HiddenGamesInTest {
    private fun game(path: String, title: String, absolute: String? = null) =
        GameEntity(sourceId = 1, systemId = "gba", relativePath = path, absolutePath = absolute, displayName = title)

    @Test
    fun `lists only hidden games, by title`() {
        val games = listOf(game("z.gba", "Zelda"), game("a.gba", "Advance Wars"), game("m.gba", "Metroid"))

        val hidden = hiddenGamesIn(games, setOf("gba/z.gba", "gba/a.gba"))

        assertEquals(listOf(JarnHiddenGame("gba/a.gba", "Advance Wars"), JarnHiddenGame("gba/z.gba", "Zelda")), hidden)
    }

    @Test
    fun `matches the full path when the game has one, like Classic`() {
        val games = listOf(game("m.gba", "Metroid", absolute = "/roms/gba/m.gba"))

        assertEquals(emptyList<JarnHiddenGame>(), hiddenGamesIn(games, setOf("gba/m.gba")))
        assertEquals(1, hiddenGamesIn(games, setOf("gba//roms/gba/m.gba")).size)
    }

    @Test
    fun `counts one per matched game by system`() {
        assertEquals(mapOf("gba" to 2, "snes" to 1), countBySystem(listOf("gba", "snes", "gba")))
        assertEquals(emptyMap<String, Int>(), countBySystem(emptyList()))
    }
}
