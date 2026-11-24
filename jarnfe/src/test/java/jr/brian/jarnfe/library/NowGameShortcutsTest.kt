package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.ProvideJarnInput
import jr.brian.jarnfe.session.JarnSessionStore
import jr.brian.jarnfe.session.StageRoute
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NowGameShortcutsTest {
    @get:Rule
    val rule = createComposeRule()

    private val system = JarnLibrarySystem(id = "snes", title = "SNES", gameCount = 3)
    private val games = listOf("one", "two", "three").map { JarnLibraryGame(it, "snes", it) }
    private val shelf = games.map { JarnShelfGame(it, JarnShelfGroup.Recent) }
    private val systemNow = JarnSystemNow("snes", shelf, isLoaded = true)
    private var showGame by mutableStateOf(false)

    @Before
    fun clearBoard() {
        NowShortcutBoard.status = null
    }

    private fun show(selection: LibrarySelection.Game, startOnGame: Boolean = true, onStatus: (NowShortcutStatus?) -> Unit = {}): () -> FocusManager {
        showGame = startOnGame
        lateinit var focusManager: FocusManager
        rule.setContent {
            focusManager = LocalFocusManager.current
            val status = NowShortcutBoard.status
            SideEffect { onStatus(status) }
            ProvideJarnInput(JarnInputRouter(), JarnScreen.Deck) {
                JarnTheme {
                    Column(Modifier.size(540.dp, 460.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Box(Modifier.weight(1f))
                            NowShortcutControls()
                        }
                        if (showGame) {
                            NowGameShortcuts(selection, systemNow) {
                                Box(Modifier.size(80.dp).testTag("play").focusable())
                            }
                        } else {
                            NowSystemContent(
                                system = system,
                                shelf = shelf,
                                isLoaded = true,
                                actions = NowSystemActions({}, {}, {}, {}),
                                pendingCoverId = null,
                                onCoverRestored = {},
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        return { focusManager }
    }

    @Test
    fun `a browsed game shows Random and Last Played for its system`() {
        show(LibrarySelection.Game(games[1]))
        rule.onNodeWithContentDescription("Random").assertExists()
        rule.onNodeWithContentDescription("Last Played").assertExists()
    }

    @Test
    fun `Last Played hides when the shown game is the last one played`() {
        show(LibrarySelection.Game(games[0]))
        rule.onNodeWithContentDescription("Random").assertExists()
        rule.onNodeWithContentDescription("Last Played").assertDoesNotExist()
        assertNull(nowGameLastPlayed(games[0], systemNow))
        assertEquals("one", nowGameLastPlayed(games[2], systemNow)?.id)
        assertNull(nowGameLastPlayed(games[2], JarnSystemNow("snes", isLoaded = true)))
    }

    @Test
    fun `Continue playing shows no shortcuts`() {
        show(LibrarySelection.Game(games[1], resume = true))
        rule.onNodeWithTag(NOW_SHORTCUTS_TAG).assertDoesNotExist()
    }

    @Test
    fun `Search results and Continue playing never offer them`() {
        val browsed = LibrarySelection.Game(games[1])
        assertTrue(nowGameShowsShortcuts(browsed, StageRoute.GameList))
        assertTrue(nowGameShowsShortcuts(browsed, StageRoute.Library))
        assertFalse(nowGameShowsShortcuts(browsed, StageRoute.Search))
        assertFalse(nowGameShowsShortcuts(LibrarySelection.Game(games[1], resume = true), StageRoute.Library))
    }

    @Test
    fun `opening a system's game keeps the icons shown and focused`() {
        val seen = mutableListOf<NowShortcutStatus?>()
        val focusManager = show(LibrarySelection.Game(games[1]), startOnGame = false) { seen += it }
        rule.onNodeWithContentDescription("one").assertIsFocused()
        rule.runOnIdle { focusManager().moveFocus(FocusDirection.Up) }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Random").assertIsFocused()
        seen.clear()
        rule.runOnIdle { showGame = true }
        rule.waitForIdle()
        assertTrue(seen.isNotEmpty())
        assertTrue(seen.none { it == null })
        rule.onNodeWithTag(NOW_SHORTCUTS_TAG).assertExists()
        rule.onNodeWithContentDescription("Random").assertIsFocused()
    }

    @Test
    fun `Random from a game picks another game in the same system`() = runTest {
        val source = object : JarnLibrarySource {
            override fun observeSystems() = flowOf(JarnLibraryFeed.ready(listOf(system)))
            override fun observeGames(systemId: String) = flowOf(JarnLibraryFeed.ready(games))
            override fun observeFavorites() = flowOf(JarnLibraryFeed.ready(emptyList<JarnLibraryGame>()))
            override fun observeRecent() = flowOf(JarnLibraryFeed.ready(emptyList<JarnLibraryGame>()))
            override fun refresh() = Unit
        }
        val library = JarnLibraryStore(source)
        val store = JarnNowShelfStore(JarnSessionStore(), library)
        repeat(30) { seed ->
            store.spin("snes", avoid = "two", random = Random(seed))
            val jump = library.jump
            assertEquals("snes", jump?.systemId)
            assertNotEquals("two", jump?.gameId)
            assertEquals(true, jump?.spin)
        }
    }
}
