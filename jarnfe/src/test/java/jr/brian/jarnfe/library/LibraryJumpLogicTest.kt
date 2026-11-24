package jr.brian.jarnfe.library

import jr.brian.core.ui.theme.JarnEasingSpin
import jr.brian.jarnfe.session.JarnSessionStore
import jr.brian.jarnfe.session.StageRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LibraryJumpLogicTest {
    private val ids = (1..10).map { "g$it" }
    private fun game(id: String) = JarnLibraryGame(id, "snes", id)

    @Test
    fun `a seeded pick is repeatable and never the focused game`() {
        val first = libraryRandomGame(ids, avoid = "g3", Random(7))
        assertEquals(first, libraryRandomGame(ids, avoid = "g3", Random(7)))
        repeat(200) { seed -> assertNotEquals("g3", libraryRandomGame(ids, avoid = "g3", Random(seed))) }
    }

    @Test
    fun `a lone game is picked even when focused, and nothing comes from nothing`() {
        assertEquals("g1", libraryRandomGame(listOf("g1"), avoid = "g1", Random(1)))
        assertNull(libraryRandomGame(emptyList(), avoid = null, Random(1)))
    }

    @Test
    fun `last played is the newest game the system was played with`() {
        val shelf = listOf(
            JarnShelfGame(game("new"), JarnShelfGroup.Recent),
            JarnShelfGame(game("old"), JarnShelfGroup.Recent),
            JarnShelfGame(game("most"), JarnShelfGroup.MostPlayed),
        )
        assertEquals("new", libraryLastPlayed(shelf)?.id)
        assertNull(libraryLastPlayed(listOf(JarnShelfGame(game("a"), JarnShelfGroup.InSystem))))
    }

    @Test
    fun `Last Played shows only once something was played, and each toggle hides its button`() {
        val played = game("a")
        assertEquals(listOf(NowFocusTarget.Random, NowFocusTarget.LastPlayed), nowShortcuts(5, played, true, true))
        assertEquals(listOf(NowFocusTarget.Random), nowShortcuts(5, null, true, true))
        assertEquals(listOf(NowFocusTarget.LastPlayed), nowShortcuts(5, played, false, true))
        assertEquals(listOf(NowFocusTarget.Random), nowShortcuts(5, played, true, false))
        assertEquals(emptyList<NowFocusTarget>(), nowShortcuts(5, played, false, false))
        assertEquals(emptyList<NowFocusTarget>(), nowShortcuts(0, null, true, true))
    }

    @Test
    fun `focus returns to the last button only while it is shown`() {
        val both = listOf(NowFocusTarget.Random, NowFocusTarget.LastPlayed)
        assertEquals(NowFocusTarget.LastPlayed, nowActionEntry(both, NowFocusTarget.LastPlayed))
        assertEquals(NowFocusTarget.Random, nowActionEntry(listOf(NowFocusTarget.Random), NowFocusTarget.LastPlayed))
        assertNull(nowActionEntry(emptyList(), NowFocusTarget.Random))
    }

    @Test
    fun `a grid spin always travels a good stretch of rows`() {
        assertEquals(0, librarySpinStartRow(currentRow = 0, targetRow = 40, lastRow = 60))
        assertEquals(28, librarySpinStartRow(currentRow = 30, targetRow = 40, lastRow = 60))
        assertEquals(15, librarySpinStartRow(currentRow = 2, targetRow = 3, lastRow = 60))
        assertEquals(10, librarySpinStartRow(currentRow = 0, targetRow = 3, lastRow = 10))
    }

    @Test
    fun `a carousel spin loops round to pass enough cards and lands on the target`() {
        val steps = libraryShelfSpinSteps(center = 5, target = 7, size = 40)
        assertEquals(42, steps)
        assertEquals(7, (5 + steps) % 40)
        assertEquals(20, libraryShelfSpinSteps(center = 0, target = 20, size = 40))
        assertEquals(-1, libraryShelfSpinSteps(center = 2, target = 1, size = 3))
    }

    @Test
    fun `carousel steps bunch up early and spread out into the landing`() {
        val times = libraryShelfSpinTimes(steps = 20, totalMillis = 1050, easing = JarnEasingSpin)
        assertEquals(20, times.size)
        assertTrue(times.zipWithNext().all { (a, b) -> b >= a })
        assertTrue(times.last() <= 1050)
        assertTrue(times[1] - times[0] < times.last() - times[times.size - 2])
    }

    @Test
    fun `a jump opens the system once and asks the list to move`() {
        val session = JarnSessionStore()
        val library = JarnLibraryStore()
        val store = JarnNowShelfStore(session, library)
        store.jumpTo("snes", "g4", spin = false)
        assertEquals(StageRoute.GameList, session.state.stageRoute)
        assertEquals("snes", library.state.openSystemId)
        assertEquals(LibraryJump("snes", "g4", spin = false, id = 1), library.jump)
        store.jumpTo("snes", "g5", spin = true)
        assertEquals(StageRoute.GameList, session.state.stageRoute)
        assertEquals("g5", library.jump?.gameId)
        library.landJump("g5")
        assertNull(library.jump)
        assertEquals("g5", library.state.focusedGameBySystem["snes"])
    }
}
