package jr.brian.jarnfe.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import jr.brian.core.ui.focus.JarnTabSwitchFocus
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.look.JarnTabFocus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnTabFocusSettingTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `each choice maps to the switches that move focus`() {
        assertEquals(JarnTabSwitchFocus(onShoulder = true, onDpad = false), JarnTabFocus.Shoulders.switchFocus())
        assertEquals(JarnTabSwitchFocus(onShoulder = false, onDpad = true), JarnTabFocus.Dpad.switchFocus())
        assertEquals(JarnTabSwitchFocus(onShoulder = true, onDpad = true), JarnTabFocus.Both.switchFocus())
        assertEquals(JarnTabSwitchFocus(onShoulder = false, onDpad = false), JarnTabFocus.Neither.switchFocus())
    }

    @Test
    fun `A cycles through the four choices and shows each one`() {
        var choice by mutableStateOf(JarnTabFocus.Shoulders)
        rule.setContent { JarnTheme { JarnTabFocusRow(choice) { choice = choice.next() } } }
        rule.onNodeWithText("Shoulders").assertExists()
        listOf("D-pad", "Both", "Neither", "Shoulders").forEach { label ->
            rule.onNodeWithText("Focus after switching tabs").performClick()
            rule.waitForIdle()
            rule.onNodeWithText(label).assertExists()
        }
        assertEquals(JarnTabFocus.Shoulders, choice)
    }
}
