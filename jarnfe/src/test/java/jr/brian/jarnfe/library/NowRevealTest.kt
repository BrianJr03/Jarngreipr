package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import jr.brian.core.ui.theme.JarnTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NowRevealTest {
    @get:Rule
    val rule = createComposeRule()

    private var value by mutableStateOf("old")
    private var partReady by mutableStateOf(false)
    private lateinit var gate: NowRevealGate

    private fun show(followsRoute: Boolean = false) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            JarnTheme {
                val reveal = rememberNowReveal(value, followsRoute = { _, _ -> followsRoute }) { it }
                gate = reveal.gate
                CompositionLocalProvider(LocalNowRevealGate provides reveal.gate) {
                    NowRevealBox(reveal) {
                        BasicText(reveal.shown)
                        if (reveal.shown == "new") NowRevealWhen(partReady)
                    }
                }
            }
        }
        rule.mainClock.advanceTimeBy(500)
    }

    @Test
    fun `the old value stays until it has faded out`() {
        show()
        rule.runOnIdle { value = "new" }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("old").assertExists()
        rule.mainClock.advanceTimeBy(200)
        rule.onNodeWithText("new").assertExists()
    }

    @Test
    fun `a route change trails the Stage by 40ms before fading`() {
        show(followsRoute = true)
        partReady = true
        rule.runOnIdle { value = "new" }
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(120)
        rule.onNodeWithText("old").assertExists()
        rule.mainClock.advanceTimeBy(80)
        rule.onNodeWithText("new").assertExists()
    }

    @Test
    fun `the gate waits for every part`() {
        show()
        rule.runOnIdle { value = "new" }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(200)
        rule.waitForIdle()
        assertFalse(gate.isReady)
        rule.runOnIdle { partReady = true }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        assertTrue(gate.isReady)
    }

    @Test
    fun `a part that never reports ready cannot hide the view forever`() {
        show()
        rule.runOnIdle { value = "new" }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(NOW_REVEAL_MAX_WAIT_MILLIS + 800)
        rule.waitForIdle()
        rule.onNodeWithText("new").assertExists()
        assertFalse(gate.isReady)
    }
}
