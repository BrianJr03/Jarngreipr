package jr.brian.jarnfe.library

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JarnLibraryStoreTest {
    private val game = JarnLibraryGame("g1", "snes", "One")

    private class FakeSource(
        val systems: Flow<JarnLibraryFeed<JarnLibrarySystem>> = MutableStateFlow(JarnLibraryFeed.ready(emptyList())),
        val games: Flow<JarnLibraryFeed<JarnLibraryGame>> = MutableStateFlow(JarnLibraryFeed.ready(emptyList())),
    ) : JarnLibrarySource {
        var refreshes = 0
        val requested = mutableListOf<String>()
        override fun observeSystems() = systems
        override fun observeGames(systemId: String): Flow<JarnLibraryFeed<JarnLibraryGame>> {
            requested += systemId
            return games
        }
        override fun observeFavorites() = MutableStateFlow(JarnLibraryFeed.ready(emptyList<JarnLibraryGame>()))
        override fun observeRecent() = MutableStateFlow(JarnLibraryFeed.ready(emptyList<JarnLibraryGame>()))
        override fun refresh() { refreshes++ }
    }

    @Test
    fun `the default source never produces games`() = runTest(UnconfinedTestDispatcher()) {
        val store = JarnLibraryStore()
        val job = launch { store.observe() }
        assertEquals(JarnLibraryStatus.Ready, store.state.systems.status)
        assertTrue(store.state.systems.items.isEmpty())
        assertTrue(store.state.recent.items.isEmpty())
        job.cancelAndJoin()
    }

    @Test
    fun `games load for the open system`() = runTest(UnconfinedTestDispatcher()) {
        val source = FakeSource(games = MutableStateFlow(JarnLibraryFeed.ready(listOf(game))))
        val store = JarnLibraryStore(source)
        val job = launch { store.observe() }
        store.update { openSystem("snes") }
        assertEquals(listOf("snes"), source.requested)
        assertEquals(listOf(game), store.state.openGames.items)
        job.cancelAndJoin()
    }

    @Test
    fun `a failing feed keeps cached items and reports unavailable`() = runTest(UnconfinedTestDispatcher()) {
        val cached = JarnLibrarySystem("snes", "SNES", 1)
        val failing = flow {
            emit(JarnLibraryFeed.ready(listOf(cached)))
            error("storage gone")
        }
        val store = JarnLibraryStore(FakeSource(systems = failing))
        val job = launch { store.observe() }
        assertEquals(JarnLibraryStatus.Unavailable, store.state.systems.status)
        assertEquals(listOf(cached), store.state.systems.items)
        job.cancelAndJoin()
    }

    @Test
    fun `retry resubscribes to a feed that failed`() = runTest(UnconfinedTestDispatcher()) {
        val recovered = JarnLibrarySystem("snes", "SNES", 1)
        var attempts = 0
        val flaky = flow {
            if (attempts++ == 0) error("storage gone")
            emit(JarnLibraryFeed.ready(listOf(recovered)))
        }
        val store = JarnLibraryStore(FakeSource(systems = flaky))
        val job = launch { store.observe() }
        assertEquals(JarnLibraryStatus.Unavailable, store.state.systems.status)
        store.retry()
        assertEquals(JarnLibraryStatus.Ready, store.state.systems.status)
        assertEquals(listOf(recovered), store.state.systems.items)
        job.cancelAndJoin()
    }

    @Test
    fun `play is a no-op until connected`() {
        val store = JarnLibraryStore()
        assertFalse(store.canPlay)
        store.play(game)
        val played = mutableListOf<JarnLibraryGame>()
        store.onPlay = { played += it }
        store.play(game)
        assertEquals(listOf(game), played)
    }

    @Test
    fun `restore applies only to a fresh process`() {
        val fresh = JarnLibraryStore()
        fresh.restoreIfFresh("snes")
        assertEquals("snes", fresh.state.openSystemId)
        val live = JarnLibraryStore().apply { update { openSystem("gba") } }
        live.restoreIfFresh("snes")
        assertEquals("gba", live.state.openSystemId)
    }

    @Test
    fun `retry asks the source to refresh`() {
        val source = FakeSource()
        JarnLibraryStore(source).retry()
        assertEquals(1, source.refreshes)
    }
}
