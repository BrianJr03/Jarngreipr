package jr.brian.jarnfe.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.look.JarnCoverGlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnCoverGlowSettingTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `A cycles off and full screen and shows each one`() {
        var choice by mutableStateOf(JarnCoverGlow.Off)
        rule.setContent { JarnTheme { JarnCoverGlowRow(choice) { choice = choice.next() } } }
        rule.onNodeWithText("Off").assertExists()
        listOf("Full screen" to JarnCoverGlow.Full, "Off" to JarnCoverGlow.Off).forEach { (label, expected) ->
            rule.onNodeWithText("Cover glow").performClick()
            rule.waitForIdle()
            rule.onNodeWithText(label).assertExists()
            assertEquals(expected, choice)
        }
    }
}
