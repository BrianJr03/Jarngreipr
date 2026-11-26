package jr.brian.jarnfe.library

import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.session.LibrarySection
import jr.brian.jarnfe.session.StageRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryAllGamesTest {
    private fun game(title: String, systemId: String = "snes", id: String = "$systemId:$title") =
        JarnLibraryGame(id = id, systemId = systemId, title = title)

    @Test
    fun `all games are in name order and marked for letter jump`() {
        val feed = JarnLibraryFeed.ready(
            listOf(game("Theme Hospital", "psx"), game("The Legend of Zelda"), game("1942", "arcade"), game("Aladdin")),
        ).inNameOrder()

        assertEquals(listOf("1942", "Aladdin", "The Legend of Zelda", "Theme Hospital"), feed.items.map { it.title })
        assertEquals(JarnGameSort.Name, feed.sort)
        assertTrue(feed.letterJumpEnabled())
    }

    @Test
    fun `the same game in two systems stays as two entries`() {
        val feed = JarnLibraryFeed.ready(listOf(game("Tetris", "gb"), game("Tetris", "nes"))).inNameOrder()

        assertEquals(listOf("gb:Tetris", "nes:Tetris"), feed.items.map { it.id })
    }

    @Test
    fun `a large library sorts in name order quickly`() {
        val titles = (0 until 20_000).map { "Game ${(it * 7919) % 20_000}" }
        val start = System.nanoTime()
        val feed = JarnLibraryFeed.ready(titles.mapIndexed { i, t -> game(t, "sys${i % 30}") }).inNameOrder()
        val millis = (System.nanoTime() - start) / 1_000_000

        assertEquals(20_000, feed.items.size)
        val keys = feed.items.map { nameSortKey(it.title) }
        assertEquals(keys.sorted(), keys)
        assertTrue("sorting took $millis ms", millis < 2_000)
    }

    @Test
    fun `the All games tab selects, confirms and follows hides like the other tabs`() {
        val games = listOf(game("A"), game("B"), game("C"))
        val state = JarnLibraryState(allGames = JarnLibraryFeed.ready(games), focusedAllGameId = "snes:B")

        assertEquals(LibrarySelection.Game(games[1]), state.selection(StageRoute.Library, LibrarySection.AllGames))
        assertEquals(LibraryConfirm.Play, state.confirmFor(StageRoute.Library, LibrarySection.AllGames, canPlay = true))
        assertEquals("snes:C", state.focusAfterHiding("snes:B").focusedAllGameId)
    }

    @Test
    fun `the focus line names the focused game's system and its place`() {
        val state = JarnLibraryState(
            systems = JarnLibraryFeed.ready(listOf(JarnLibrarySystem("gba", "Game Boy Advance", 2))),
            allGames = JarnLibraryFeed.ready(listOf(game("A", "gba"), game("B", "gba"))),
            focusedAllGameId = "gba:B",
        )

        assertEquals(AllGamesFocus("Game Boy Advance", 2, 2), allGamesFocusLine(state))
        assertNull(allGamesFocusLine(JarnLibraryState()))
    }

    @Test
    fun `older saved sessions still restore their tab, and All games round trips`() {
        assertEquals(LibrarySection.Recent, JarnSessionState.decode("Stage|Library|Recent|Now")?.librarySection)
        val state = JarnSessionState(librarySection = LibrarySection.AllGames)
        assertEquals(LibrarySection.AllGames, JarnSessionState.decode(state.encode())?.librarySection)
    }

    @Test
    fun `tabs go Systems, All games, Favorites, Recent, Collections`() {
        assertEquals(
            listOf(
                LibrarySection.Systems,
                LibrarySection.AllGames,
                LibrarySection.Favorites,
                LibrarySection.Recent,
                LibrarySection.Collections,
            ),
            LibrarySection.entries,
        )
    }

    @Test
    fun `all games only load once the tab is wanted`() = runTest(UnconfinedTestDispatcher()) {
        var subscriptions = 0
        val all = MutableStateFlow(JarnLibraryFeed.ready(listOf(game("A")), sort = JarnGameSort.Name))
        val source = object : JarnLibrarySource {
            override fun observeSystems() = MutableStateFlow(JarnLibraryFeed.ready(emptyList<JarnLibrarySystem>()))
            override fun observeGames(systemId: String) = MutableStateFlow(JarnLibraryFeed.ready(emptyList<JarnLibraryGame>()))
            override fun observeFavorites() = MutableStateFlow(JarnLibraryFeed.ready(emptyList<JarnLibraryGame>()))
            override fun observeRecent() = MutableStateFlow(JarnLibraryFeed.ready(emptyList<JarnLibraryGame>()))
            override fun observeAllGames(): Flow<JarnLibraryFeed<JarnLibraryGame>> {
                subscriptions++
                return all
            }
            override fun refresh() = Unit
        }
        val store = JarnLibraryStore(source)
        val job = launch { store.observe() }

        assertEquals(0, subscriptions)
        store.wantAllGames()
        assertEquals(1, subscriptions)
        assertEquals(listOf("A"), store.state.allGames.items.map { it.title })
        job.cancelAndJoin()
    }
}
