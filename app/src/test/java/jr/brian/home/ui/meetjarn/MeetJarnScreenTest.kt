package jr.brian.home.ui.meetjarn

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w960dp-h540dp")
class MeetJarnScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val log = mutableListOf<String>()
    private val colors = MeetJarnColors(primary = Color(0xFF7C4DFF), background = Color.Black)

    private fun show(content: MeetJarnContent = MeetJarnContent()) {
        rule.setContent {
            MeetJarnScreen(content, colors, onTry = { log += "try" }, onNotNow = { log += "not now" })
        }
        rule.waitForIdle()
    }

    private fun press(key: Key) {
        rule.onRoot().performKeyInput { pressKey(key) }
        rule.waitForIdle()
    }

    @Test
    fun `try jarn takes focus first`() {
        show()
        rule.onNodeWithTag(MEET_JARN_TRY_TAG).assertIsFocused()
    }

    @Test
    fun `right moves to not now and left comes back`() {
        show()
        press(Key.DirectionRight)
        rule.onNodeWithTag(MEET_JARN_NOT_NOW_TAG).assertIsFocused()
        press(Key.DirectionLeft)
        rule.onNodeWithTag(MEET_JARN_TRY_TAG).assertIsFocused()
    }

    @Test
    fun `b means not now`() {
        show()
        press(Key.ButtonB)
        assertEquals(listOf("not now"), log)
    }

    @Test
    fun `try jarn and not now do what they say`() {
        show()
        rule.onNodeWithTag(MEET_JARN_TRY_TAG).performClick()
        rule.onNodeWithTag(MEET_JARN_NOT_NOW_TAG).performClick()
        assertEquals(listOf("try", "not now"), log)
    }

    @Test
    fun `the first line follows the library and single screens drop both screens`() {
        show(MeetJarnContent(library = MeetJarnLibrary.NoGames, twoScreens = false))
        rule.onNodeWithText("Point Jarn at your games folder in one step.").assertExists()
        rule.onNodeWithText("Built for the controller.").assertExists()
        rule.onNodeWithText("A controller-first home for your games and apps.").assertExists()
    }
}
