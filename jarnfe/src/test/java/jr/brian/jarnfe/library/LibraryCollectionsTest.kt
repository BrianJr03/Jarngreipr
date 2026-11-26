package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.library.collections.CollectionNameResult
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.ProvideJarnInput
import jr.brian.jarnfe.session.LibrarySection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LibraryCollectionsTest {
    @get:Rule
    val rule = createComposeRule()

    private val games = listOf(
        collectionGame("Excitebike", playtimeMinutes = 30),
        collectionGame("F-Zero", playtimeMinutes = 45),
        collectionGame("Kart", playtimeMinutes = 60),
        collectionGame("Wave Race"),
    )
    private val collections = FakeJarnCollectionSource(games)
    private val router = JarnInputRouter()

    private fun storeWith(items: List<JarnCollection>) = JarnLibraryStore(CollectionTestLibrary(collections)).apply {
        update { copy(collections = JarnLibraryFeed.ready(items)) }
    }

    private fun collect(name: String, vararg ids: String): JarnCollection = runBlocking {
        val id = (collections.createCollection(name) as CollectionNameResult.Saved).collectionId
        ids.forEach { collections.addToCollection(id, it) }
        JarnCollection(id, name, ids.size)
    }

    private fun showTab(store: JarnLibraryStore, onOpen: (Long) -> Unit = {}) {
        rule.setContent {
            JarnTheme {
                ProvideJarnInput(router, JarnScreen.Stage) {
                    Box(Modifier.size(900.dp, 500.dp)) {
                        LibraryHome(
                            section = LibrarySection.Collections,
                            library = store,
                            onOpenSystem = {},
                            emptyActions = {},
                            onOpenCollection = onOpen,
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun `with no collections the tab says to press Select on a game`() {
        showTab(storeWith(emptyList()))
        rule.onNodeWithText("No collections yet").assertIsDisplayed()
        rule.onNodeWithText("Press Select on a game, then choose Collections to make one.").assertIsDisplayed()
    }

    @Test
    fun `a tile shows its name and count and A opens it`() {
        val racing = collect("Racing", "Kart", "F-Zero")
        var opened: Long? = null
        showTab(storeWith(listOf(racing)), onOpen = { opened = it })
        rule.onNodeWithText("Racing").assertIsDisplayed()
        rule.onNodeWithText("2 games").assertIsDisplayed()
        rule.onNodeWithText("Racing").performClick()
        assertEquals(racing.id, opened)
    }

    @Test
    fun `four covers make a mosaic and fewer show the collection icon`() {
        val full = collect("Racing", "Excitebike", "F-Zero", "Kart", "Wave Race")
        val small = collect("Party", "Kart")
        showTab(storeWith(listOf(full, small)))
        rule.onNodeWithTag(COLLECTION_MOSAIC_TAG, useUnmergedTree = true).assertExists()
        rule.onNodeWithTag(COLLECTION_ICON_TAG, useUnmergedTree = true).assertExists()
    }

    @Test
    fun `the Deck shows a focused collection's mosaic, game count and play time`() {
        val racing = collect("Racing", "Excitebike", "F-Zero", "Kart", "Wave Race")
        rule.setContent {
            JarnTheme {
                Box(Modifier.size(538.dp, 420.dp)) { NowCollectionView(racing, collections) }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag(NOW_COLLECTION_TAG).assertExists()
        rule.onNodeWithTag(COLLECTION_MOSAIC_TAG).assertExists()
        rule.onNodeWithText("Racing").assertExists()
        rule.onNodeWithText("4").assertExists()
        rule.onNodeWithText("2 h").assertExists()
    }

    @Test
    fun `play time adds up every listed game`() {
        assertEquals(135L, collectionPlaytimeMinutes(games))
    }
}
