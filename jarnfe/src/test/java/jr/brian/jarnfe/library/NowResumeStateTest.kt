package jr.brian.jarnfe.library

import jr.brian.jarnfe.launch.JarnGameFileStatus
import jr.brian.jarnfe.session.LibrarySection
import jr.brian.jarnfe.session.StageRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class NowResumeStateTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val system = JarnLibrarySystem("snes", "Super Nintendo", gameCount = 3)
    private fun game(id: String) = JarnLibraryGame(id, "snes", "Game $id")

    private fun played(vararg ids: String) = JarnLibraryState(
        systems = JarnLibraryFeed.ready(listOf(system)),
        recent = JarnLibraryFeed.ready(ids.map(::game)),
    )

    @Test
    fun `the focused banner shows the last game as a resume poster`() {
        val selection = played("a", "b").resumeSelection(StageRoute.Library, LibrarySection.Systems, heroFocused = true)
        assertEquals(LibrarySelection.Game(game("a"), resume = true), selection)
    }

    @Test
    fun `without banner focus Now follows the focused system`() {
        val state = played("a")
        assertNull(state.resumeSelection(StageRoute.Library, LibrarySection.Systems, heroFocused = false))
        assertNull(state.resumeSelection(StageRoute.Library, LibrarySection.Recent, heroFocused = true))
        assertNull(state.resumeSelection(StageRoute.GameList, LibrarySection.Systems, heroFocused = true))
    }

    @Test
    fun `never played has no card and Now shows the normal system view`() {
        val state = played()
        assertEquals(LibraryHero.FocusedSystem(system), state.hero)
        assertNull(state.resumeSelection(StageRoute.Library, LibrarySection.Systems, heroFocused = true))
        assertEquals(LibrarySelection.System(system), state.selection(StageRoute.Library, LibrarySection.Systems))
    }

    @Test
    fun `also recent skips the card and missing games and keeps four`() {
        val state = played("a", "b", "c", "d", "e", "f", "g")
        assertEquals(listOf("b", "d", "e", "f"), state.alsoRecent(missing = setOf("c")).map { it.id })
        assertTrue(played("a").alsoRecent().isEmpty())
    }

    @Test
    fun `a missing file opens the notice and never launches`() {
        assertEquals(ResumeAction.ShowMissing, resumeAction(JarnGameFileStatus.Missing))
        assertEquals(ResumeAction.Play, resumeAction(JarnGameFileStatus.Present))
        assertEquals(ResumeAction.Play, resumeAction(JarnGameFileStatus.Unknown))
        assertEquals(ResumeAction.Play, resumeAction(null))
    }

    @Test
    fun `last played counts calendar days`() {
        val now = 1_700_000_000_000L
        val hour = 3_600_000L
        assertEquals(0, nowDaysAgo(now - hour, now, utc))
        assertEquals(1, nowDaysAgo(now - 24 * hour, now, utc))
        assertEquals(3, nowDaysAgo(now - 72 * hour, now, utc))
        assertEquals(0, nowDaysAgo(now + hour, now, utc))
    }
}
