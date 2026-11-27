package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import jr.brian.jarnfe.themekit.FocusedItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LibraryThemeListTest {
    @get:Rule
    val rule = createComposeRule()

    private val ids = (1..30).map { "item$it" }
    private var focused: String? = null
    private var opened: String? = null

    @Test
    fun `D-pad moves focus row by row and A opens the focused row`() {
        showList(focusedKey = "item1")
        rule.onNodeWithTag("item1").assertIsFocused()

        press(Key.DirectionDown)
        press(Key.DirectionDown)
        rule.onNodeWithTag("item3").assertIsFocused()
        assertEquals("item3", focused)

        press(Key.DirectionUp)
        rule.onNodeWithTag("item2").assertIsFocused()

        press(Key.Enter)
        assertEquals("item2", opened)
    }

    @Test
    fun `focus starts on the remembered row`() {
        showList(focusedKey = "item20")
        rule.onNodeWithTag("item20").assertIsFocused()
    }

    private fun showList(focusedKey: String) {
        rule.setContent {
            JarnTheme {
                Box(Modifier.size(width = 400.dp, height = 300.dp)) {
                    LibraryThemeList(
                        items = ids,
                        content = LibraryListContent(
                            itemKey = { it },
                            itemTitle = { it },
                            itemFocus = { FocusedItem.None },
                            onOpen = { opened = it },
                            itemModifier = { Modifier.testTag(it) },
                        ),
                        geometry = LibraryListGeometry(rows = 5),
                        focusedKey = focusedKey,
                        onFocused = { focused = it },
                    )
                }
            }
        }
        rule.waitForIdle()
    }

    private fun press(key: Key) {
        rule.onRoot().performKeyInput { pressKey(key) }
        rule.waitForIdle()
    }
}
