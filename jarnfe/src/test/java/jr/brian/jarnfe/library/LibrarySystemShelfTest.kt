package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.core.app.ApplicationProvider
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w1280dp-h720dp")
class LibrarySystemShelfTest {
    @get:Rule
    val rule = createComposeRule()

    private val allSystems = (1..6).map { JarnLibrarySystem("sys$it", "System $it", gameCount = 1) }
    private val played = JarnLibraryGame("g1", "sys1", "Played Game")

    private fun library(recent: List<JarnLibraryGame>) = JarnLibraryStore().apply {
        onPlay = {}
        update {
            copy(
                systems = JarnLibraryFeed.ready(allSystems),
                recent = JarnLibraryFeed.ready(recent),
                focusedSystemId = "sys3",
            )
        }
    }

    private fun show(library: JarnLibraryStore) {
        rule.setContent {
            JarnTheme {
                LibrarySystemShelf(
                    library = library,
                    onFocused = { id -> library.update { focusSystem(id) } },
                    onOpenSystem = {},
                )
            }
        }
        rule.waitForIdle()
    }

    private fun press(key: Key) {
        rule.onRoot().performKeyInput { pressKey(key) }
        rule.waitForIdle()
    }

    private val continueEyebrow: String
        get() = ApplicationProvider.getApplicationContext<Application>()
            .getString(R.string.jarnfe_library_continue_playing)

    @Test
    fun `up reaches the continue card and down returns to the centered system`() {
        val library = library(listOf(played))
        show(library)
        rule.onNodeWithText(continueEyebrow).assertExists()
        rule.onNodeWithContentDescription("System 3").assertIsFocused()
        press(Key.DirectionUp)
        assertTrue(library.heroFocused)
        press(Key.DirectionDown)
        assertFalse(library.heroFocused)
        rule.onNodeWithContentDescription("System 3").assertIsFocused()
    }

    @Test
    fun `no card when nothing has been played`() {
        show(library(emptyList()))
        rule.onNodeWithText(continueEyebrow).assertDoesNotExist()
        rule.onNodeWithContentDescription("System 3").assertIsFocused()
    }
}
