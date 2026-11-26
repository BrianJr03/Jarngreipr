package jr.brian.core.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnLoadingGate
import jr.brian.core.ui.theme.JarnTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val LABEL = "Loading apps"
private const val CONTENT = "loaded"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnLoadingGateTest {
    @get:Rule
    val rule = createComposeRule()

    private var value by mutableStateOf<String?>(null)

    private fun setGate(initial: String? = null, reducedMotion: Boolean = false) {
        value = initial
        rule.mainClock.autoAdvance = false
        rule.setContent {
            JarnTheme(reducedMotion = reducedMotion) {
                Box(Modifier.size(400.dp)) {
                    JarnLoadingGate(value, LABEL) { Box(Modifier.fillMaxSize().testTag(CONTENT)) }
                }
            }
        }
    }

    private fun arrive() {
        rule.runOnIdle {
            value = "apps"
            Snapshot.sendApplyNotifications()
        }
    }

    private fun advance(millis: Long) {
        rule.mainClock.advanceTimeBy(millis)
        rule.waitForIdle()
    }

    @Test
    fun `the spinner stays hidden before the delay`() {
        setGate()
        advance(150)
        rule.onNodeWithText(LABEL).assertDoesNotExist()
    }

    @Test
    fun `the spinner shows after the delay while nothing has loaded`() {
        setGate()
        advance(400)
        rule.onNodeWithText(LABEL).assertExists()
        rule.onNodeWithTag(CONTENT).assertDoesNotExist()
    }

    @Test
    fun `the content replaces the spinner once it loads`() {
        setGate()
        advance(400)
        arrive()
        advance(600)
        rule.onNodeWithText(LABEL).assertDoesNotExist()
        rule.onNodeWithTag(CONTENT).assertExists()
    }

    @Test
    fun `a load inside the delay never shows the spinner`() {
        setGate()
        advance(100)
        arrive()
        repeat(40) {
            advance(16)
            rule.onNodeWithText(LABEL).assertDoesNotExist()
        }
        rule.onNodeWithTag(CONTENT).assertExists()
    }

    @Test
    fun `content already loaded shows on the first frame`() {
        setGate(initial = "apps")
        rule.onNodeWithTag(CONTENT).assertExists()
        rule.onNodeWithText(LABEL).assertDoesNotExist()
    }

    @Test
    fun `reduced motion swaps the spinner for the content without a fade`() {
        setGate(reducedMotion = true)
        advance(400)
        arrive()
        advance(64)
        rule.onNodeWithText(LABEL).assertDoesNotExist()
        rule.onNodeWithTag(CONTENT).assertExists()
    }
}
