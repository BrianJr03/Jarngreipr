package jr.brian.jarnfe.library

import android.app.Application
import jr.brian.jarnfe.session.DeckTab
import jr.brian.jarnfe.deck.showsNowShortcuts
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isFocused
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
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
import org.junit.Before
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

    private var showSystem by mutableStateOf(true)

    @Before
    fun clearBoard() {
        NowShortcutBoard.status = null
    }
    private var clicked = 0

    private fun showWithHeader(
        actions: NowSystemActions = NowSystemActions({ clicked++ }, {}, {}, {}),
        parts: NowSystemParts = NowSystemParts(),
        shown: JarnLibrarySystem = system,
    ): () -> FocusManager {
        lateinit var focusManager: FocusManager
        rule.setContent {
            focusManager = LocalFocusManager.current
            ProvideJarnInput(JarnInputRouter(), JarnScreen.Deck) {
                JarnTheme {
                    Column(Modifier.size(540.dp, 460.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Box(Modifier.weight(1f))
                            NowShortcutControls()
                        }
                        if (showSystem) {
                            NowSystemContent(
                                system = shown,
                                shelf = shelf,
                                isLoaded = true,
                                actions = actions,
                                pendingCoverId = null,
                                onCoverRestored = {},
                                parts = parts,
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        return { focusManager }
    }

    private val played = system.copy(lastPlayedAtMs = 1_764_000_000_000L)

    private fun showPlate(parts: NowSystemParts): () -> FocusManager =
        showWithHeader(parts = parts, shown = played)

    private fun assertShelfStays(focusManager: () -> FocusManager) {
        rule.onNodeWithText("Recently played").assertExists()
        rule.onNodeWithContentDescription("one").assertIsFocused()
        move(focusManager(), FocusDirection.Up)
        rule.onNodeWithContentDescription("Random").assertIsFocused()
    }

    @Test
    fun `hiding the game row leaves no covers and focus starts on the first header icon`() {
        val focusManager = showWithHeader(parts = NowSystemParts(showsGameRow = false))
        rule.onNodeWithContentDescription("one").assertDoesNotExist()
        rule.onNodeWithText("Recently played").assertDoesNotExist()
        rule.onNodeWithContentDescription("Random").assertIsFocused()
        move(focusManager(), FocusDirection.Down)
        rule.onNodeWithContentDescription("Random").assertIsFocused()
        rule.onNodeWithText("SNES").assertExists()
    }

    @Test
    fun `with the game row and Random hidden, focus starts on Last Played`() {
        showWithHeader(NowSystemActions(null, {}, {}, {}), NowSystemParts(showsGameRow = false))
        rule.onNodeWithContentDescription("Last Played").assertIsFocused()
    }

    @Test
    fun `with the game row and both icons hidden, nothing takes focus`() {
        showWithHeader(NowSystemActions(null, null, {}, {}), NowSystemParts(showsGameRow = false))
        rule.onNodeWithContentDescription("one").assertDoesNotExist()
        rule.onAllNodes(isFocused()).assertCountEquals(0)
    }

    @Test
    fun `everything shows by default`() {
        showPlate(NowSystemParts())
        rule.onNodeWithText("SNES").assertExists()
        rule.onNodeWithText("games").assertExists()
        rule.onNodeWithText("Last played", substring = true).assertExists()
    }

    @Test
    fun `hiding the logo keeps the count and last played`() {
        val focusManager = showPlate(NowSystemParts(showsLogo = false))
        rule.onNodeWithText("SNES").assertDoesNotExist()
        rule.onNodeWithText("games").assertExists()
        rule.onNodeWithText("Last played", substring = true).assertExists()
        assertShelfStays(focusManager)
    }

    @Test
    fun `hiding the game count leaves last played on the line alone`() {
        val focusManager = showPlate(NowSystemParts(showsCount = false))
        rule.onNodeWithText("games").assertDoesNotExist()
        rule.onNodeWithText("SNES").assertExists()
        rule.onNodeWithText("Last played", substring = true).assertExists()
        assertShelfStays(focusManager)
    }

    @Test
    fun `hiding last played leaves the count on the line alone`() {
        val focusManager = showPlate(NowSystemParts(showsLastPlayed = false))
        rule.onNodeWithText("Last played", substring = true).assertDoesNotExist()
        rule.onNodeWithText("games").assertExists()
        rule.onNodeWithText("SNES").assertExists()
        assertShelfStays(focusManager)
    }

    @Test
    fun `with all three hidden the plate is gone and the shelf still leads`() {
        val focusManager = showPlate(NowSystemParts(showsLogo = false, showsCount = false, showsLastPlayed = false))
        rule.onNodeWithText("SNES").assertDoesNotExist()
        rule.onNodeWithText("games").assertDoesNotExist()
        rule.onNodeWithText("Last played", substring = true).assertDoesNotExist()
        assertShelfStays(focusManager)
    }

    @Test
    fun `the plate is skipped only when nothing in it is left`() {
        val none = NowSystemParts(showsLogo = false, showsCount = false, showsLastPlayed = false)
        assertFalse(nowPlateHasContent(played, none))
        assertTrue(nowPlateHasContent(played.copy(favoriteCount = 2), none))
        assertTrue(nowPlateHasContent(played, none.copy(showsLastPlayed = true)))
        assertFalse(nowPlateHasContent(system, none.copy(showsLastPlayed = true)))
    }

    @Test
    fun `the deck starts on the shelf, Up reaches the header shortcuts and Down comes back`() {
        val focusManager = showWithHeader()
        rule.onNodeWithContentDescription("one").assertIsFocused()
        move(focusManager(), FocusDirection.Right)
        move(focusManager(), FocusDirection.Up)
        rule.onNodeWithContentDescription("Random").assertIsFocused()
        move(focusManager(), FocusDirection.Right)
        rule.onNodeWithContentDescription("Last Played").assertIsFocused()
        move(focusManager(), FocusDirection.Down)
        rule.onNodeWithContentDescription("two").assertIsFocused()
    }

    @Test
    fun `the header shows the shortcuts for a system, Random then Last Played, apart`() {
        showWithHeader()
        val random = rule.onNodeWithContentDescription("Random").getUnclippedBoundsInRoot()
        val last = rule.onNodeWithContentDescription("Last Played").getUnclippedBoundsInRoot()
        assertTrue(random.right.value + nowShortcutGap(12f) - 1f <= last.left.value)
        assertEquals(random.top.value, last.top.value, 1f)
    }

    @Test
    fun `pressing Random runs its jump`() {
        showWithHeader()
        rule.onNodeWithContentDescription("Random").performClick()
        assertEquals(1, clicked)
    }

    @Test
    fun `leaving the system hides the shortcuts and they let go of focus`() {
        val focusManager = showWithHeader()
        move(focusManager(), FocusDirection.Up)
        rule.onNodeWithContentDescription("Random").assertIsFocused()
        rule.runOnIdle { showSystem = false }
        rule.waitForIdle()
        rule.onNodeWithTag(NOW_SHORTCUTS_TAG).assertDoesNotExist()
        rule.onNodeWithContentDescription("Random").assertDoesNotExist()
        assertNull(NowShortcutBoard.status)
    }

    @Test
    fun `each toggle hides its shortcut, and with both off nothing is shown`() {
        showWithHeader(NowSystemActions(null, {}, {}, {}))
        rule.onNodeWithContentDescription("Random").assertDoesNotExist()
        rule.onNodeWithContentDescription("Last Played").assertExists()
        rule.runOnIdle { NowShortcutBoard.status = null }
        rule.waitForIdle()
        rule.onNodeWithTag(NOW_SHORTCUTS_TAG).assertDoesNotExist()
    }

    @Test
    fun `with both shortcuts off the header stays empty`() {
        showWithHeader(NowSystemActions(null, null, {}, {}))
        rule.onNodeWithTag(NOW_SHORTCUTS_TAG).assertDoesNotExist()
    }

    @Test
    fun `only the Now tab carries the shortcuts`() {
        assertTrue(DeckTab.Now.showsNowShortcuts())
        assertFalse(DeckTab.Apps.showsNowShortcuts())
        assertFalse(DeckTab.Canvas.showsNowShortcuts())
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
