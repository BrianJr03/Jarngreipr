package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.look.JarnBrowseView
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w1280dp-h720dp")
class LibraryShelfTest {
    @get:Rule
    val rule = createComposeRule()

    private var reported: String? = null
    private val content = LibraryShelfContent<String>(
        itemKey = { it },
        itemTitle = { it },
        onOpen = {},
        itemModifier = { Modifier.testTag(it) },
        art = {},
    )

    @Composable
    private fun Shelf(ids: List<String>, focusedKey: String?) {
        LibraryShelf(
            items = ids,
            kind = LibraryShelfKind.Games,
            content = content,
            focusedKey = focusedKey,
            onFocused = { reported = it },
        )
    }

    private fun press(key: Key) {
        rule.onRoot().performKeyInput { pressKey(key) }
        rule.waitForIdle()
    }

    @Test
    fun `left from the first item wraps to the last with four or more`() {
        val ids = (1..6).map { "game$it" }
        rule.setContent { JarnTheme { Shelf(ids, "game1") } }
        rule.waitForIdle()
        rule.onNodeWithTag("game1").assertIsFocused()
        press(Key.DirectionLeft)
        rule.onNodeWithTag("game6").assertIsFocused()
        assertEquals("game6", reported)
        press(Key.DirectionRight)
        press(Key.DirectionRight)
        rule.onNodeWithTag("game2").assertIsFocused()
    }

    @Test
    fun `three items stop at the ends`() {
        val ids = listOf("a", "b", "c")
        rule.setContent { JarnTheme { Shelf(ids, "a") } }
        rule.waitForIdle()
        press(Key.DirectionLeft)
        rule.onNodeWithTag("a").assertIsFocused()
        press(Key.DirectionRight)
        press(Key.DirectionRight)
        press(Key.DirectionRight)
        rule.onNodeWithTag("c").assertIsFocused()
    }

    @Test
    fun `down is reserved and keeps the centered item`() {
        val ids = (1..5).map { "game$it" }
        rule.setContent {
            JarnTheme {
                Column {
                    Box(Modifier.size(10.dp).testTag("above").focusable())
                    Shelf(ids, "game3")
                }
            }
        }
        rule.waitForIdle()
        press(Key.DirectionDown)
        rule.onNodeWithTag("game3").assertIsFocused()
    }

    @Test
    fun `coming back to systems centers the system you came from`() {
        val ids = (1..8).map { "system$it" }
        var showing by mutableStateOf(true)
        var focused by mutableStateOf("system2")
        rule.setContent { JarnTheme { if (showing) Shelf(ids, focused) } }
        rule.waitForIdle()
        rule.runOnIdle { showing = false }
        rule.waitForIdle()
        rule.runOnIdle {
            focused = "system7"
            showing = true
        }
        rule.waitForIdle()
        rule.onNodeWithTag("system7").assertIsFocused()
    }

    @Test
    fun `a focus change made elsewhere recenters the row`() {
        val ids = (1..8).map { "system$it" }
        var focused by mutableStateOf("system2")
        rule.setContent { JarnTheme { Shelf(ids, focused) } }
        rule.waitForIdle()
        rule.runOnIdle { focused = "system5" }
        rule.waitForIdle()
        rule.onNodeWithTag("system5").assertIsFocused()
    }

    @Test
    fun `flipping between grid and carousel keeps focus on the same item`() {
        val ids = (1..12).map { "game$it" }
        var view by mutableStateOf(JarnBrowseView.Grid)
        var focused by mutableStateOf("game4")
        rule.setContent {
            JarnTheme {
                LibraryBrowseSwitch(
                    view = view,
                    grid = {
                        LibraryFocusGrid(
                            items = ids,
                            itemKey = { it },
                            focusedKey = focused,
                            onFocused = { focused = it },
                            cellMinSize = 80.dp,
                        ) { id, modifier -> Box(modifier.size(80.dp).testTag("grid-$id").focusable()) }
                    },
                    shelf = {
                        LibraryShelf(
                            items = ids,
                            kind = LibraryShelfKind.Games,
                            content = content,
                            focusedKey = focused,
                            onFocused = { focused = it },
                        )
                    },
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("grid-game4").assertIsFocused()
        rule.runOnIdle { view = JarnBrowseView.Carousel }
        rule.waitForIdle()
        rule.onNodeWithTag("game4").assertIsFocused()
        press(Key.DirectionRight)
        rule.runOnIdle { view = JarnBrowseView.Grid }
        rule.waitForIdle()
        rule.onNodeWithTag("grid-game5").assertIsFocused()
    }

    @Test
    fun `up from the tabs and back down lands on the centered item`() {
        val ids = (1..5).map { "game$it" }
        rule.setContent {
            JarnTheme {
                Column {
                    Box(Modifier.size(10.dp).testTag("tabs").focusable())
                    Shelf(ids, "game4")
                }
            }
        }
        rule.waitForIdle()
        press(Key.DirectionUp)
        rule.onNodeWithTag("tabs").assertIsFocused()
        press(Key.DirectionDown)
        rule.onNodeWithTag("game4").assertIsFocused()
    }
}
