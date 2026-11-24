package jr.brian.jarnfe.search

import jr.brian.jarnfe.apps.FakeJarnAppOptions
import jr.brian.jarnfe.apps.JarnAppOptions
import jr.brian.jarnfe.apps.JarnAppsCommands
import jr.brian.jarnfe.apps.JarnAppsSnapshot
import jr.brian.jarnfe.apps.jarnShownApps
import jr.brian.jarnfe.apps.launchOrAsk
import jr.brian.jarnfe.canvas.JarnAppDisplay
import jr.brian.jarnfe.dock.JarnDockApp
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.library.JarnLibrarySystem
import jr.brian.jarnfe.library.LibrarySelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnSearchAppsTest {
    private val snes = JarnLibrarySystem("snes", "Super Nintendo", 2)
    private val apps = listOf(
        JarnDockApp("a.yt", "YouTube Music"),
        JarnDockApp("a.music", "Music"),
        JarnDockApp("a.cafe", "Café Finder"),
        JarnDockApp("a.retro", "RetroArch"),
    )

    private fun find(raw: String, scope: String? = null, from: List<JarnDockApp> = apps): List<String> {
        val query = parseJarnSearchQuery(raw)
        val allowed = allowedSystemIds(query, listOf(snes), scope)
        return searchApps(from, query, allowed, 10, JarnDockApp::label).map { it.packageName }
    }

    @Test
    fun `apps match by label with the game rules`() {
        assertEquals(listOf("a.music", "a.yt"), find("music"))
        assertEquals(listOf("a.cafe"), find("CAFE"))
        assertEquals(listOf("a.retro"), find("arch"))
    }

    @Test
    fun `a scoped or system search leaves apps out`() {
        assertTrue(find("music", scope = "snes").isEmpty())
        assertTrue(find("@snes music").isEmpty())
        assertTrue(find("").isEmpty())
    }

    @Test
    fun `hidden apps stay out of search, pinned ones stay in`() {
        val snapshot = JarnAppsSnapshot(apps, pinned = listOf("a.retro"), hidden = setOf("a.yt"))
        assertEquals(listOf("a.music"), find("music", from = jarnShownApps(snapshot)))
        assertEquals(listOf("a.retro"), find("retro", from = jarnShownApps(snapshot)))
    }

    @Test
    fun `apps get their own tab, and empty tabs are skipped`() {
        val game = JarnLibraryGame("1", "snes", "Music Quest")
        val results = JarnSearchResults(games = listOf(game), apps = apps.take(1))
        assertEquals(JarnSearchTab.Apps, results.stepTab(JarnSearchTab.Games, 1))
        assertEquals(JarnSearchTab.Games, results.stepTab(JarnSearchTab.Apps, -1))
        assertEquals(JarnSearchTab.Apps, results.stepTab(JarnSearchTab.Apps, 1))

        val onlyApps = JarnSearchResults(apps = apps.take(1))
        assertEquals(JarnSearchTab.Apps, onlyApps.visibleTab(JarnSearchTab.Games))
        assertEquals(LibrarySelection.None, onlyApps.firstSelection(JarnSearchTab.Games))
        assertEquals(LibrarySelection.None, results.refreshed(LibrarySelection.Game(game), JarnSearchTab.Apps))
        assertEquals(LibrarySelection.Game(game), results.refreshed(LibrarySelection.Game(game), JarnSearchTab.Games))
    }

    @Test
    fun `A opens the app, or asks for a screen when the app is set to`() {
        val options = FakeJarnAppOptions()
        val opened = mutableListOf<String>()
        val asked = mutableListOf<String>()
        val commands = JarnAppsCommands({ opened += it }, options)

        commands.launchOrAsk("a.music") { asked += it }
        options.current = JarnAppOptions(JarnAppDisplay.Current, askOnLaunch = true, null, hasSecondDisplay = true)
        commands.launchOrAsk("a.yt") { asked += it }

        assertEquals(listOf("a.music"), opened)
        assertEquals(listOf("a.yt"), asked)
    }
}
