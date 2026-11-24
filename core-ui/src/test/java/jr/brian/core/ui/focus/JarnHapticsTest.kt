package jr.brian.core.ui.focus

import android.app.Application
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnHapticsTest {
    @get:Rule
    val rule = createComposeRule()

    private var now = 1_000L
    private val effects = mutableListOf<Int>()
    private var enabled = true

    @Before
    fun setUp() {
        JarnHaptics.gate = JarnHapticGate()
        JarnHaptics.clock = { now }
        JarnHaptics.performer = { _, effect -> effects += effect }
        JarnHaptics.isEnabled = { enabled }
    }

    @After
    fun tearDown() {
        JarnHaptics.isEnabled = { true }
        JarnHaptics.gate = JarnHapticGate()
        JarnHaptics.clock = SystemClock::uptimeMillis
        JarnHaptics.performer = { target, effect -> target.performHapticFeedback(effect) }
    }

    private val view: View get() = View(RuntimeEnvironment.getApplication())

    @Test
    fun `ticks closer than 60ms are dropped so a held direction purrs`() {
        val gate = JarnHapticGate()
        assertTrue(gate.allows(0, enabled = true))
        assertFalse(gate.allows(30, enabled = true))
        assertFalse(gate.allows(59, enabled = true))
        assertTrue(gate.allows(60, enabled = true))
        assertTrue(gate.allows(125, enabled = true))
    }

    @Test
    fun `button vibration off silences every tick and deliberate effect`() {
        enabled = false
        JarnHaptics.tick(view)
        JarnHaptics.perform(view, HapticFeedbackConstants.LONG_PRESS)
        assertEquals(emptyList<Int>(), effects)
    }

    @Test
    fun `a button press ticks once with the light effect`() {
        JarnHaptics.tick(view)
        assertEquals(listOf(HapticFeedbackConstants.CLOCK_TICK), effects)
    }

    @Test
    fun `focus arriving after a d-pad press ticks once, a restore never does`() {
        JarnHaptics.onFocusArrived(view)
        assertEquals(emptyList<Int>(), effects)

        JarnHaptics.armNavigation()
        now += 16
        JarnHaptics.onFocusArrived(view)
        now += 100
        JarnHaptics.onFocusArrived(view)
        assertEquals(listOf(HapticFeedbackConstants.CLOCK_TICK), effects)
    }

    @Test
    fun `deliberate effects keep their own strength and skip the rate limit`() {
        JarnHaptics.perform(view, HapticFeedbackConstants.LONG_PRESS)
        JarnHaptics.perform(view, HapticFeedbackConstants.CLOCK_TICK)
        assertEquals(listOf(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.CLOCK_TICK), effects)
    }

    @Test
    fun `a focus move onto a jarn item calls the helper once`() {
        val first = FocusRequester()
        val second = FocusRequester()
        rule.setContent {
            Column {
                listOf(first, second).forEachIndexed { index, requester ->
                    Box(
                        Modifier.size(20.dp).testTag("item$index").focusRequester(requester).jarnNavHaptic().focusable(),
                    )
                }
            }
        }
        rule.runOnIdle { first.requestFocus() }
        rule.waitForIdle()
        assertEquals(emptyList<Int>(), effects)

        rule.runOnIdle {
            JarnHaptics.armNavigation()
            second.requestFocus()
        }
        rule.waitForIdle()
        assertEquals(listOf(HapticFeedbackConstants.CLOCK_TICK), effects)
    }
}
