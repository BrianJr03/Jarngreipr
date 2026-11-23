package jr.brian.jarnfe.keyboard

import android.app.Application
import android.view.KeyEvent
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.requestFocus
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnKeyInput
import jr.brian.jarnfe.input.JarnKeyPhase
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.ProvideJarnInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnTextKeyboardTest {

    @get:Rule
    val rule = createComposeRule()

    private val router = JarnInputRouter()

    private fun tap(code: Int) {
        router.dispatch(JarnKeyInput(code, JarnKeyPhase.Down, 0))
        router.dispatch(JarnKeyInput(code, JarnKeyPhase.Up, 10))
    }

    private fun showKeyboard(initialText: String = "", maxLength: Int = 10, onDone: (String) -> Unit = {}) {
        rule.setContent {
            JarnTheme {
                ProvideJarnInput(router, JarnScreen.Stage) {
                    JarnTextKeyboard(
                        label = "Name",
                        masked = false,
                        onDone = onDone,
                        onCancel = {},
                        initialText = initialText,
                        maxLength = maxLength,
                    )
                }
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun `focus stays on the keyboard when symbols has a shorter row`() {
        showKeyboard()
        rule.onNodeWithText("m").requestFocus()
        rule.waitForIdle()
        rule.onNodeWithText("m").assertIsFocused()
        rule.runOnIdle { tap(KeyEvent.KEYCODE_BUTTON_L1) }
        rule.waitForIdle()
        rule.onNodeWithText("~").assertIsFocused()
    }

    @Test
    fun `buttons edit prefilled text and start finishes`() {
        var result: String? = null
        showKeyboard(initialText = "ab", onDone = { result = it })
        rule.runOnIdle { tap(KeyEvent.KEYCODE_BUTTON_X) }
        rule.runOnIdle { tap(KeyEvent.KEYCODE_BUTTON_R1) }
        rule.runOnIdle { tap(KeyEvent.KEYCODE_BUTTON_START) }
        rule.runOnIdle { assertEquals("a ", result) }
    }

    @Test
    fun `length limit also applies to prefilled text`() {
        val state = JarnKeyboardState("abcdef", maxLength = 3)
        assertEquals("abc", state.text)
        assertEquals("abc", state.press(JarnKeyboardKey.Letter('z')).text)
        assertEquals("", state.press(JarnKeyboardKey.Clear).text)
    }

    @Test
    fun `position clamps to the row that exists`() {
        val rows = listOf(listOf(1, 2, 3), listOf(1))
        assertEquals(JarnKeyPosition(1, 0), JarnKeyPosition(1, 2).clampedTo(rows))
        assertEquals(JarnKeyPosition(0, 2), JarnKeyPosition(0, 2).clampedTo(rows))
    }
}
