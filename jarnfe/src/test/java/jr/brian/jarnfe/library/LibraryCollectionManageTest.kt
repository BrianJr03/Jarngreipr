package jr.brian.jarnfe.library

import android.app.Application
import android.view.KeyEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnTabs
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.library.collections.CollectionNameResult
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnKeyInput
import jr.brian.jarnfe.input.JarnKeyPhase
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.ProvideJarnInput
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.session.LibrarySection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LibraryCollectionManageTest {
    @get:Rule
    val rule = createComposeRule()

    private val collections = FakeJarnCollectionSource()
    private val router = JarnInputRouter()
    private val store = JarnLibraryStore(CollectionTestLibrary(collections))
    private val session = JarnSessionState(librarySection = LibrarySection.Collections)

    private fun names(): List<String> = runBlocking { collections.observeCollections().first().items.map { it.name } }

    private val tabs = FocusRequester()

    /**
     * Like Android, which hands focus to the window's first focusable (the tab strip) when the
     * focused sheet row leaves. Armed by [closing] for the one close it wraps.
     */
    private var fallbackArmed = false
    private var fallbackFired = false

    private fun showTab(vararg names: String) {
        runBlocking { names.forEach { collections.createCollection(it) as CollectionNameResult.Saved } }
        rule.setContent {
            JarnTheme {
                ProvideJarnInput(router, JarnScreen.Stage) {
                    LaunchedEffect(Unit) { store.observe() }
                    JarnInputLayer(bindings = { manageCollectionBinding(session, store) }) {
                        Column(
                            Modifier.size(900.dp, 560.dp).onFocusChanged {
                                if (fallbackArmed && !it.hasFocus) {
                                    fallbackArmed = false
                                    fallbackFired = true
                                    tabs.requestFocus()
                                }
                            },
                        ) {
                            JarnTabs(
                                labels = listOf("Recent", "Collections"),
                                selectedIndex = 1,
                                onSelect = {},
                                modifier = Modifier.focusRequester(tabs),
                            )
                            LibraryHome(
                                section = LibrarySection.Collections,
                                library = store,
                                onOpenSystem = {},
                                emptyActions = {},
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    /** Runs [close] with the tab strip picking up focus as the sheet goes, as it does on device. */
    private fun closing(close: () -> Unit) {
        fallbackArmed = true
        fallbackFired = false
        close()
        assertTrue("the tab strip never picked up focus", fallbackFired)
    }

    private fun press(code: Int) {
        rule.runOnIdle {
            router.dispatch(JarnKeyInput(code, JarnKeyPhase.Down, 0))
            router.dispatch(JarnKeyInput(code, JarnKeyPhase.Up, 10))
        }
        rule.waitForIdle()
    }

    private fun openManage() = press(KeyEvent.KEYCODE_BUTTON_SELECT)

    private fun click(text: String) {
        rule.onNodeWithText(text).performClick()
        rule.waitForIdle()
    }

    /** On the keyboard, Select deletes a letter. */
    private fun rename(erase: Int, type: String, saves: Boolean = true) {
        openManage()
        click("Rename")
        repeat(erase) { press(KeyEvent.KEYCODE_BUTTON_SELECT) }
        type.forEach { click(it.toString()) }
        if (saves) closing { press(KeyEvent.KEYCODE_BUTTON_START) } else press(KeyEvent.KEYCODE_BUTTON_START)
    }

    @Test
    fun `Select on a tile opens its Manage sheet on Rename`() {
        showTab("racing", "party")
        rule.onNodeWithText("racing").assertIsFocused()
        openManage()
        rule.onNodeWithText("Rename").assertIsFocused()
        rule.onNodeWithText("Move earlier").assertIsNotEnabled()
        rule.onNodeWithText("Move later").assertIsEnabled()
        rule.onNodeWithText("Delete").assertExists()
    }

    @Test
    fun `B closes the sheet and takes focus back from the tab strip to the tile`() {
        showTab("racing", "party")
        openManage()
        closing { press(KeyEvent.KEYCODE_BUTTON_B) }
        rule.onNodeWithText("Rename").assertDoesNotExist()
        rule.onNodeWithText("racing").assertIsFocused()
        rule.onNodeWithText("Collections").assertIsNotFocused()
    }

    @Test
    fun `Rename opens the keyboard with the name and saves a new one`() {
        showTab("racing", "party")
        openManage()
        click("Rename")
        rule.onNodeWithText("1").assertIsFocused()
        press(KeyEvent.KEYCODE_BUTTON_B)
        rule.onNodeWithText("Rename").assertExists()
        press(KeyEvent.KEYCODE_BUTTON_B)
        rename(erase = 3, type = "ers")
        assertEquals(listOf("racers", "party"), names())
        rule.onNodeWithText("racers").assertIsFocused()
    }

    @Test
    fun `a rename to a name in use keeps the keyboard up and says why`() {
        showTab("racing", "party")
        rename(erase = 6, type = "party", saves = false)
        rule.onNodeWithText("You already have a collection with that name.").assertExists()
        assertEquals(listOf("racing", "party"), names())
    }

    @Test
    fun `Move later reorders the grid and focus follows the tile`() {
        showTab("racing", "party", "puzzle")
        openManage()
        click("Move later")
        assertEquals(listOf("party", "racing", "puzzle"), names())
        rule.onNodeWithText("Move earlier").assertIsEnabled()
        click("Move later")
        assertEquals(listOf("party", "puzzle", "racing"), names())
        rule.onNodeWithText("Move later").assertIsNotEnabled()
        closing { press(KeyEvent.KEYCODE_BUTTON_B) }
        rule.onNodeWithText("racing").assertIsFocused()
    }

    @Test
    fun `Delete asks first, then focus moves to the next tile and finally the empty state`() {
        showTab("racing", "party")
        openManage()
        click("Delete")
        rule.onNodeWithText("Delete racing?").assertExists()
        rule.onNodeWithText("The games stay in your library.").assertExists()
        click("Cancel")
        rule.onNodeWithText("Rename").assertExists()
        click("Delete")
        closing { click("Delete") }
        assertEquals(listOf("party"), names())
        rule.onNodeWithText("party").assertIsFocused()

        openManage()
        click("Delete")
        click("Delete")
        assertEquals(emptyList<String>(), names())
        rule.onNodeWithText("No collections yet").assertExists()
    }
}
