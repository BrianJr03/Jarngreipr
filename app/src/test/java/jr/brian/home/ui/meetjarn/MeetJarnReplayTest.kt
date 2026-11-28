package jr.brian.home.ui.meetjarn

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.core.app.ApplicationProvider
import jr.brian.frontend.arrival.JarnArrival
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val DIALOG_LOAD_TIMEOUT_MS = 5_000L

/**
 * Meet Jarn shown from inside Jarn. The test app has no Hilt, so any reach for the preview gate
 * would fail here: closing cleanly proves the replay never touches it.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w960dp-h540dp")
class MeetJarnReplayTest {
    @get:Rule
    val rule = createComposeRule()

    private val log = mutableListOf<String>()
    private val colors = MeetJarnColors(primary = Color(0xFF7C4DFF), background = Color.Black)

    private fun press(key: Key) {
        rule.onRoot().performKeyInput { pressKey(key) }
        rule.waitForIdle()
    }

    /** The dialog loads its content on the IO dispatcher, which waitForIdle doesn't track. */
    private fun waitForDialogContent() {
        rule.waitUntil(DIALOG_LOAD_TIMEOUT_MS) {
            rule.onAllNodesWithTag(MEET_JARN_CLOSE_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        rule.waitForIdle()
    }

    @Test
    fun `replay shows a single close, focused, and no offer buttons`() {
        rule.setContent {
            MeetJarnScreen(
                MeetJarnContent(),
                colors,
                onTry = { log += "try" },
                onNotNow = { log += "close" },
                mode = MeetJarnMode.Replay,
            )
        }
        rule.waitForIdle()
        rule.onNodeWithTag(MEET_JARN_CLOSE_TAG).assertIsFocused()
        rule.onNodeWithTag(MEET_JARN_TRY_TAG).assertDoesNotExist()
        rule.onNodeWithTag(MEET_JARN_NOT_NOW_TAG).assertDoesNotExist()
        press(Key.ButtonB)
        assertEquals(listOf("close"), log)
    }

    @Test
    fun `b closes the replay without the gate, the arrival flag or a switch`() {
        var closed = false
        rule.setContent { MeetJarnReplayDialog(onClose = { closed = true }) }
        waitForDialogContent()
        rule.onNodeWithTag(MEET_JARN_CLOSE_TAG).assertIsFocused()
        rule.onNodeWithTag(MEET_JARN_CLOSE_TAG).performKeyInput { pressKey(Key.ButtonB) }
        rule.waitForIdle()
        assertTrue(closed)
        assertFalse(JarnArrival.takeFromClassic(ApplicationProvider.getApplicationContext()))
    }
}
