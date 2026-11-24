package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.ProvideJarnInput
import jr.brian.jarnfe.launch.GameActionsPage
import jr.brian.jarnfe.launch.JarnGameActions
import jr.brian.jarnfe.session.JarnSessionStore
import jr.brian.jarnfe.session.StageRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NowSystemViewTest {
    @get:Rule
    val rule = createComposeRule()

    private val system = JarnLibrarySystem(id = "snes", title = "SNES", gameCount = 3)
    private val games = listOf("one", "two", "three").map { JarnLibraryGame(it, "snes", it) }
    private val shelf = games.map { JarnShelfGame(it, JarnShelfGroup.Recent) }

    @Test
    fun `deck starts on Random and moves through the shelf and back`() {
        lateinit var focusManager: FocusManager
        rule.setContent {
            focusManager = LocalFocusManager.current
            ProvideJarnInput(JarnInputRouter(), JarnScreen.Deck) {
                JarnTheme {
                    Box(Modifier.size(540.dp, 400.dp)) {
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
        rule.waitForIdle()
        rule.onNodeWithText("Random").assertIsFocused()
        move(focusManager, FocusDirection.Down)
        rule.onNodeWithContentDescription("one").assertIsFocused()
        move(focusManager, FocusDirection.Right)
        rule.onNodeWithContentDescription("two").assertIsFocused()
        move(focusManager, FocusDirection.Up)
        rule.onNodeWithText("Random").assertIsFocused()
        move(focusManager, FocusDirection.Down)
        rule.onNodeWithContentDescription("two").assertIsFocused()
    }

    @Test
    fun `a pending cover takes focus and is reported restored`() {
        var restored = false
        rule.setContent {
            ProvideJarnInput(JarnInputRouter(), JarnScreen.Deck) {
                JarnTheme {
                    Box(Modifier.size(540.dp, 400.dp)) {
                        NowSystemContent(
                            system = system,
                            shelf = shelf,
                            isLoaded = true,
                            actions = NowSystemActions({}, {}, {}, {}),
                            pendingCoverId = "three",
                            onCoverRestored = { restored = true },
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("three").assertIsFocused()
        assertTrue(restored)
    }

    private fun move(focusManager: FocusManager, direction: FocusDirection) {
        rule.runOnIdle { focusManager.moveFocus(direction) }
        rule.waitForIdle()
    }

    @Test
    fun `shelf layout covers empty, single and full systems`() {
        assertEquals(NowShelfLayout.Empty, nowShelfLayout(0, 0))
        assertEquals(NowShelfLayout.Hidden, nowShelfLayout(4, 0))
        assertEquals(NowShelfLayout.Single, nowShelfLayout(1, 1))
        assertEquals(NowShelfLayout.Shelf, nowShelfLayout(4, 1))
    }

    @Test
    fun `shelf entry keeps the last cover only while it is on the shelf`() {
        assertEquals("b", nowShelfEntryId(listOf("a", "b"), "b"))
        assertEquals("a", nowShelfEntryId(listOf("a", "b"), "gone"))
        assertNull(nowShelfEntryId(emptyList(), "b"))
    }

    @Test
    fun `entry focus leaves a user who stayed on the tabs alone`() {
        assertTrue(nowTakesEntryFocus(activated = true, holdsFocus = false, tabsHaveFocus = true))
        assertFalse(nowTakesEntryFocus(activated = false, holdsFocus = false, tabsHaveFocus = true))
        assertTrue(nowTakesEntryFocus(activated = false, holdsFocus = false, tabsHaveFocus = false))
        assertFalse(nowTakesEntryFocus(activated = true, holdsFocus = true, tabsHaveFocus = true))
    }

    @Test
    fun `cover height shrinks when the dock takes room`() {
        assertEquals(104.dp, nowCoverHeight(380.dp))
        assertEquals(80.dp, nowCoverHeight(312.dp))
    }

    @Test
    fun `scene holds the old art until the new art is known`() {
        val bare = system.copy(artPath = null)
        assertNull(nowSystemPaths(bare, JarnSystemNow("snes")))
        assertEquals(emptyList<String>(), nowSystemPaths(bare, JarnSystemNow("snes", isLoaded = true)))
        assertEquals(
            listOf("bg", "scene"),
            nowSystemPaths(bare.copy(artPath = "bg"), JarnSystemNow("snes", sceneArtPath = "scene")),
        )
        assertNull(nowGamePaths(games[0], null))
        assertNull(nowGamePaths(games[0], NowSceneGameArt("two", "wide")))
        assertEquals(emptyList<String>(), nowGamePaths(games[0], NowSceneGameArt("one", null)))
        assertEquals(listOf("wide"), nowGamePaths(games[0], NowSceneGameArt("one", "wide")))
    }

    @Test
    fun `details open the game and B returns to the same cover`() {
        val session = JarnSessionStore()
        val library = JarnLibraryStore()
        val store = JarnNowShelfStore(session, library)
        store.openDetails(games[1])
        assertEquals(StageRoute.GameList, session.state.stageRoute)
        assertEquals(JarnScreen.Deck, session.state.activeScreen)
        assertEquals("snes", library.state.openSystemId)
        assertEquals("two", library.state.focusedGameBySystem["snes"])
        store.returnToCover()
        assertEquals(StageRoute.Library, session.state.stageRoute)
        assertEquals(JarnScreen.Deck, session.state.activeScreen)
        assertEquals("two", store.pendingCoverId)
        assertNull(store.returnTo)
    }

    @Test
    fun `a cover launch that needs a picker opens that game, and a clean launch stays put`() {
        val session = JarnSessionStore()
        val library = JarnLibraryStore().apply { onPlay = {} }
        val store = JarnNowShelfStore(session, library)
        try {
            JarnGameActions.gameId = "two"
            JarnGameActions.page = GameActionsPage.Emulators
            store.play(games[1])
            assertEquals(GameActionsPage.Actions, JarnGameActions.page)
            store.followLaunchedCover()
            assertEquals(StageRoute.Library, session.state.stageRoute)
            JarnGameActions.page = GameActionsPage.Emulators
            store.followLaunchedCover()
            assertEquals(StageRoute.GameList, session.state.stageRoute)
            assertEquals("two", store.returnTo?.gameId)
        } finally {
            JarnGameActions.gameId = null
            JarnGameActions.page = GameActionsPage.Actions
        }
    }

    @Test
    fun `return is forgotten once the stage moves to another game`() {
        val store = JarnNowShelfStore(JarnSessionStore(), JarnLibraryStore())
        store.openDetails(games[1])
        store.forgetIfLeft(StageRoute.GameList, LibrarySelection.System(system))
        assertEquals("two", store.returnTo?.gameId)
        store.forgetIfLeft(StageRoute.GameList, LibrarySelection.Game(games[2]))
        assertNull(store.returnTo)
    }
}
