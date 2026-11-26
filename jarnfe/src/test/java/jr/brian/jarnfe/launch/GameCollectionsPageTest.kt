package jr.brian.jarnfe.launch

import android.app.Application
import android.view.KeyEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.semantics.SemanticsActions
import jr.brian.core.ui.components.JarnTabs
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.library.collections.COLLECTION_NAME_MAX_LENGTH
import jr.brian.frontend.library.collections.CollectionNameResult
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnKeyInput
import jr.brian.jarnfe.input.JarnKeyPhase
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.ProvideJarnInput
import jr.brian.jarnfe.library.FakeJarnCollectionSource
import jr.brian.jarnfe.library.JarnLibraryGame
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class GameCollectionsPageTest {
    @get:Rule
    val rule = createComposeRule()

    private val router = JarnInputRouter()
    private val game = JarnLibraryGame(id = "kart", systemId = "n64", title = "Kart")
    private val source = FakeJarnCollectionSource(listOf(game))

    @After
    fun tearDown() = JarnGameActions.returnHandled()

    private fun memberships(): Set<Long> = runBlocking { source.observeGameCollections(game.id).first() }

    private fun collectionNames(): List<String> = runBlocking { source.observeCollections().first().items.map { it.name } }

    private fun showPicker(withTabs: Boolean = false) {
        rule.setContent {
            JarnTheme {
                ProvideJarnInput(router, JarnScreen.Stage) {
                    Column {
                        if (withTabs) JarnTabs(labels = listOf("Now", "Apps"), selectedIndex = 0, onSelect = {})
                        GameCollectionsPage(game, remember { FocusRequester() }, source)
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun openNewCollectionWithoutTouch() {
        rule.onNodeWithText("New collection…").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
    }

    @Test
    fun `opening New collection puts focus on the first key`() {
        showPicker(withTabs = true)
        rule.onNodeWithText("New collection…").assertIsFocused()
        openNewCollectionWithoutTouch()
        rule.onNodeWithText("1").assertIsFocused()
    }

    @Test
    fun `the keyboard takes focus back when the tab strip picked it up`() {
        showPicker(withTabs = true)
        rule.onNodeWithText("Now").requestFocus()
        rule.waitForIdle()
        openNewCollectionWithoutTouch()
        rule.onNodeWithText("1").assertIsFocused()
    }

    private fun press(code: Int) {
        rule.runOnIdle {
            router.dispatch(JarnKeyInput(code, JarnKeyPhase.Down, 0))
            router.dispatch(JarnKeyInput(code, JarnKeyPhase.Up, 10))
        }
        rule.waitForIdle()
    }

    private fun typeName(name: String) {
        rule.onNodeWithText("New collection…").performClick()
        rule.waitForIdle()
        name.forEach { letter -> rule.onNodeWithText(letter.toString()).performClick() }
        press(KeyEvent.KEYCODE_BUTTON_START)
    }

    @Test
    fun `A on a collection adds the game and A again takes it out`() {
        val id = runBlocking { (source.createCollection("Racing") as CollectionNameResult.Saved).collectionId }
        showPicker()
        rule.onNodeWithText("Racing").performClick()
        rule.waitForIdle()
        assertEquals(setOf(id), memberships())
        rule.onNodeWithText("Racing").performClick()
        rule.waitForIdle()
        assertTrue(memberships().isEmpty())
    }

    @Test
    fun `New collection names one with the keyboard and puts the game in it`() {
        showPicker()
        typeName("racing")
        assertEquals(listOf("racing"), collectionNames())
        assertEquals(1, memberships().size)
        rule.onNodeWithText("New collection…").assertExists()
    }

    @Test
    fun `a name already in use keeps the keyboard up and says why`() {
        runBlocking { source.createCollection("Racing") }
        showPicker()
        typeName("racing")
        rule.onNodeWithText("You already have a collection with that name.").assertExists()
        rule.onNodeWithText("Collection name").assertExists()
        assertEquals(listOf("Racing"), collectionNames())
        assertTrue(memberships().isEmpty())
    }

    @Test
    fun `B on the keyboard goes back to the list without saving`() {
        showPicker()
        rule.onNodeWithText("New collection…").performClick()
        rule.waitForIdle()
        press(KeyEvent.KEYCODE_BUTTON_B)
        rule.onNodeWithText("New collection…").assertExists()
        assertTrue(collectionNames().isEmpty())
    }

    @Test
    fun `a name that is too long or blank is refused with its own message`() = runBlocking {
        val tooLong = source.createWith("x".repeat(COLLECTION_NAME_MAX_LENGTH + 1), game.id)
        assertEquals(CollectionNameResult.TooLong, tooLong)
        assertEquals(R.string.jarnfe_collection_name_too_long, tooLong.problemRes())
        assertEquals(R.string.jarnfe_collection_name_blank, source.createWith("   ", game.id).problemRes())
        assertEquals(R.string.jarnfe_collection_name_duplicate, CollectionNameResult.Duplicate.problemRes())
        assertNull(CollectionNameResult.Saved(1).problemRes())
        assertTrue(memberships().isEmpty())
    }

    @Test
    fun `coming back from the picker puts focus on the Collections row`() {
        rule.setContent {
            JarnTheme {
                ProvideJarnInput(router, JarnScreen.Stage) {
                    Column { GameCollectionsRow(game, onOpen = {}, source = source) }
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle { JarnGameActions.returnTo(GameActionsPage.Collections) }
        rule.waitForIdle()
        rule.onNodeWithText("Collections").assertIsFocused()
        assertNull(JarnGameActions.returnFrom)
        assertEquals(GameActionsPage.Actions, JarnGameActions.page)
    }

    @Test
    fun `the row counts the collections that hold the game`() {
        runBlocking {
            listOf("Racing", "Party").forEach { name ->
                val id = (source.createCollection(name) as CollectionNameResult.Saved).collectionId
                source.addToCollection(id, game.id)
            }
        }
        rule.setContent {
            JarnTheme {
                ProvideJarnInput(router, JarnScreen.Stage) {
                    Column { GameCollectionsRow(game, onOpen = {}, source = source) }
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("2").assertExists()
    }
}
