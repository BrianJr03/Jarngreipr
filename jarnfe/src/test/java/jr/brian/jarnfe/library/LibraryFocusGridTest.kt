package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LibraryFocusGridTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `focus returns to the remembered item when the grid comes back`() {
        val ids = (1..40).map { "item$it" }
        var showing by mutableStateOf(true)
        rule.setContent {
            JarnTheme {
                Box(Modifier.size(400.dp)) {
                    if (showing) {
                        LibraryFocusGrid(
                            items = ids,
                            itemKey = { it },
                            focusedKey = "item30",
                            onFocused = {},
                            cellMinSize = 80.dp,
                        ) { id, modifier ->
                            Box(modifier.size(80.dp).testTag(id).focusable())
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("item30").assertIsFocused()
        rule.runOnIdle { showing = false }
        rule.waitForIdle()
        rule.runOnIdle { showing = true }
        rule.waitForIdle()
        rule.onNodeWithTag("item30").assertIsFocused()
    }
}
