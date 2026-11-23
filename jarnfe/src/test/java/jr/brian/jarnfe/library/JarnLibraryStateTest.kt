package jr.brian.jarnfe.library

import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.session.LibrarySection
import jr.brian.jarnfe.session.StageRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnLibraryStateTest {
    private val snes = JarnLibrarySystem("snes", "SNES", 2)
    private val gba = JarnLibrarySystem("gba", "GBA", 1)
    private val one = JarnLibraryGame("g1", "snes", "One")
    private val two = JarnLibraryGame("g2", "snes", "Two")
    private val base = JarnLibraryState(
        systems = JarnLibraryFeed.ready(listOf(snes, gba)),
        gamesBySystem = mapOf("snes" to JarnLibraryFeed.ready(listOf(one, two))),
    )

    @Test
    fun `hero prefers the most recent game`() {
        val state = base.copy(recent = JarnLibraryFeed.ready(listOf(two, one)))
        assertEquals(LibraryHero.ContinuePlaying(two), state.hero)
    }

    @Test
    fun `hero falls back to the focused system`() {
        assertEquals(LibraryHero.FocusedSystem(gba), base.focusSystem("gba").hero)
    }

    @Test
    fun `hero is absent with an empty library`() {
        assertNull(JarnLibraryState(systems = JarnLibraryFeed.ready(emptyList())).hero)
    }

    @Test
    fun `selection follows the stage`() {
        val state = base.focusSystem("gba")
        assertEquals(LibrarySelection.System(gba), state.selection(StageRoute.Library, LibrarySection.Systems))
        val open = state.openSystem("snes").focusOpenGame("g2")
        assertEquals(LibrarySelection.Game(two), open.selection(StageRoute.GameList, LibrarySection.Systems))
        assertEquals(LibrarySelection.None, open.selection(StageRoute.Settings, LibrarySection.Systems))
    }

    @Test
    fun `a removed focus falls back to the first item`() {
        assertEquals(snes, base.focusSystem("gone").focusedSystem)
    }

    @Test
    fun `system and game focus are restored after leaving a game list`() {
        val session = JarnSessionState().openRoute(StageRoute.GameList)
        val browsed = base.openSystem("snes").focusOpenGame("g2").stepOpenSystem(1)
        assertEquals("gba", browsed.focusedSystem?.id)
        assertEquals(StageRoute.Library, session.back().stageRoute)
        assertEquals(two, browsed.stepOpenSystem(-1).focusedOpenGame)
    }

    @Test
    fun `stepping stops at both ends`() {
        val first = base.openSystem("snes")
        assertEquals("snes", first.stepOpenSystem(-1).openSystemId)
        assertEquals("gba", first.stepOpenSystem(1).stepOpenSystem(1).openSystemId)
    }

    @Test
    fun `a game list without its system is orphaned once systems are known`() {
        assertTrue(base.gameListIsOrphaned(StageRoute.GameList))
        assertTrue(base.copy(openSystemId = "gone").gameListIsOrphaned(StageRoute.GameList))
        assertFalse(base.openSystem("snes").gameListIsOrphaned(StageRoute.GameList))
        val loading = JarnLibraryState(openSystemId = "snes")
        assertFalse(loading.gameListIsOrphaned(StageRoute.GameList))
        assertFalse(base.gameListIsOrphaned(StageRoute.Library))
    }

    @Test
    fun `confirm reflects what a press does`() {
        val open = base.openSystem("snes")
        assertEquals(LibraryConfirm.Open, base.confirmFor(StageRoute.Library, LibrarySection.Systems, false))
        assertEquals(LibraryConfirm.None, open.confirmFor(StageRoute.GameList, LibrarySection.Systems, false))
        assertEquals(LibraryConfirm.Play, open.confirmFor(StageRoute.GameList, LibrarySection.Systems, true))
        assertEquals(
            LibraryConfirm.None,
            JarnLibraryState().confirmFor(StageRoute.Library, LibrarySection.Systems, true),
        )
    }

    @Test
    fun `only real metadata is listed`() {
        val fields = JarnGameMetadata(developer = " Studio ", genre = "  ", rating = null).textFields()
        assertEquals(listOf("Studio"), fields.map { it.value })
    }
}
