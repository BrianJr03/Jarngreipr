package jr.brian.core.ui.focus

import android.app.Application
import android.os.SystemClock
import android.os.Vibrator
import android.os.VibratorManager
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
import org.junit.Assert.assertNotNull
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
    private var enabled = true
    private var hasVibrator = true
    private val vibrated = mutableListOf<JarnHapticEffect>()
    private val viewFeedback = mutableListOf<JarnHapticEffect>()
    private val defaultVibratorOf = JarnHaptics.vibratorOf
    private val defaultVibrate = JarnHaptics.vibrate
    private val defaultViewFeedback = JarnHaptics.viewFeedback

    private val vibrator: Vibrator?
        get() = RuntimeEnvironment.getApplication().getSystemService(VibratorManager::class.java)?.defaultVibrator

    @Before
    fun setUp() {
        JarnHaptics.gate = JarnHapticGate()
        JarnHaptics.clock = { now }
        JarnHaptics.isEnabled = { enabled }
        JarnHaptics.vibratorOf = { if (hasVibrator) vibrator else null }
        JarnHaptics.vibrate = { _, effect -> vibrated += effect }
        JarnHaptics.viewFeedback = { _, effect -> viewFeedback += effect }
    }

    @After
    fun tearDown() {
        JarnHaptics.isEnabled = { true }
        JarnHaptics.gate = JarnHapticGate()
        JarnHaptics.clock = SystemClock::uptimeMillis
        JarnHaptics.vibratorOf = defaultVibratorOf
        JarnHaptics.vibrate = defaultVibrate
        JarnHaptics.viewFeedback = defaultViewFeedback
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
    fun `a press ticks through the vibrator, not the view`() {
        assertNotNull("Robolectric has a vibrator", vibrator)
        JarnHaptics.tick(view)
        assertEquals(listOf(JarnHapticEffect.Tick), vibrated)
        assertEquals(emptyList<JarnHapticEffect>(), viewFeedback)
    }

    @Test
    fun `quick presses share the rate limit`() {
        JarnHaptics.tick(view)
        now += 30
        JarnHaptics.tick(view)
        now += 40
        JarnHaptics.tick(view)
        assertEquals(listOf(JarnHapticEffect.Tick, JarnHapticEffect.Tick), vibrated)
    }

    @Test
    fun `button vibration off silences every tick and deliberate effect`() {
        enabled = false
        JarnHaptics.tick(view)
        JarnHaptics.perform(view, JarnHapticEffect.Grab)
        assertEquals(emptyList<JarnHapticEffect>(), vibrated)
        assertEquals(emptyList<JarnHapticEffect>(), viewFeedback)
    }

    @Test
    fun `with no vibrator the view's haptic stands in`() {
        hasVibrator = false
        JarnHaptics.tick(view)
        assertEquals(emptyList<JarnHapticEffect>(), vibrated)
        assertEquals(listOf(JarnHapticEffect.Tick), viewFeedback)
    }

    @Test
    fun `canvas grab and drag keep their own effects and skip the rate limit`() {
        JarnHaptics.perform(view, JarnHapticEffect.Grab)
        JarnHaptics.perform(view, JarnHapticEffect.Tick)
        assertEquals(listOf(JarnHapticEffect.Grab, JarnHapticEffect.Tick), vibrated)
    }

    @Test
    fun `focus arriving after a d-pad press ticks once, a restore never does`() {
        JarnHaptics.onFocusArrived(view)
        assertEquals(emptyList<JarnHapticEffect>(), vibrated)

        JarnHaptics.armNavigation()
        now += 16
        JarnHaptics.onFocusArrived(view)
        now += 100
        JarnHaptics.onFocusArrived(view)
        assertEquals(listOf(JarnHapticEffect.Tick), vibrated)
    }

    @Test
    fun `a vibration builds for every effect on this vibrator`() {
        val shaker = vibrator
        assertNotNull(shaker)
        assertNotNull(jarnVibrationFor(shaker!!, JarnHapticEffect.Tick))
        assertNotNull(jarnVibrationFor(shaker, JarnHapticEffect.Grab))
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
        assertEquals(emptyList<JarnHapticEffect>(), vibrated)

        rule.runOnIdle {
            JarnHaptics.armNavigation()
            second.requestFocus()
        }
        rule.waitForIdle()
        assertEquals(listOf(JarnHapticEffect.Tick), vibrated)
    }
}
