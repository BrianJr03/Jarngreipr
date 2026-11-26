package jr.brian.jarnfe.library

import android.app.Application
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import jr.brian.core.ui.components.JarnKey
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputHint
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnKeyInput
import jr.brian.jarnfe.input.JarnKeyPhase
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.ProvideJarnInput
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NowHideUiTest {
    @get:Rule
    val rule = createComposeRule()

    private val router = JarnInputRouter()
    private var showing by mutableStateOf(true)
    private var options = 0
    private var moves = 0
    private var confirms = 0
    private var clock = 0L

    @Before
    fun setUp() = NowVideoVisibility.report(false)

    @After
    fun tearDown() = NowVideoVisibility.report(false)

    private fun showNow() {
        rule.setContent {
            ProvideJarnInput(router, JarnScreen.Stage) {
                JarnTheme {
                    NowVideoVisibilityReport(showing)
                    NowHiddenUiInput()
                    JarnInputLayer(
                        bindings = {
                            optionsOrHideUi(R.string.jarnfe_hint_context) { options++ }
                            on(JarnAction.MoveDown) { moves++ }
                            on(JarnAction.Confirm, R.string.jarnfe_hint_play) { confirms++ }
                        },
                    )
                }
            }
        }
        rule.waitForIdle()
    }

    private fun key(code: Int, phase: JarnKeyPhase, repeat: Int = 0) {
        rule.runOnUiThread { router.dispatch(JarnKeyInput(code, phase, clock, repeat)) }
        rule.waitForIdle()
    }

    private fun tap(code: Int) {
        key(code, JarnKeyPhase.Down)
        clock += TAP_MILLIS
        key(code, JarnKeyPhase.Up)
    }

    private fun holdSelect() {
        key(KeyEvent.KEYCODE_BUTTON_SELECT, JarnKeyPhase.Down)
        clock += HOLD_MILLIS
        key(KeyEvent.KEYCODE_BUTTON_SELECT, JarnKeyPhase.Down, repeat = 1)
        clock += TAP_MILLIS
        key(KeyEvent.KEYCODE_BUTTON_SELECT, JarnKeyPhase.Up)
    }

    @Test
    fun `holding Select over a showing preview hides the UI without opening Options`() {
        showNow()
        holdSelect()
        assertTrue(NowVideoVisibility.uiHidden)
        assertEquals(0, options)
    }

    @Test
    fun `holding Select again brings the UI back`() {
        showNow()
        holdSelect()
        holdSelect()
        assertFalse(NowVideoVisibility.uiHidden)
        assertEquals(0, options)
    }

    @Test
    fun `a D-pad press only brings the UI back`() {
        showNow()
        holdSelect()
        tap(KeyEvent.KEYCODE_DPAD_DOWN)
        assertFalse(NowVideoVisibility.uiHidden)
        assertEquals(0, moves)
        tap(KeyEvent.KEYCODE_DPAD_DOWN)
        assertEquals(1, moves)
    }

    @Test
    fun `A only brings the UI back and never launches`() {
        showNow()
        holdSelect()
        tap(KeyEvent.KEYCODE_BUTTON_A)
        assertFalse(NowVideoVisibility.uiHidden)
        assertEquals(0, confirms)
    }

    @Test
    fun `B brings the UI back`() {
        showNow()
        holdSelect()
        tap(KeyEvent.KEYCODE_BUTTON_B)
        assertFalse(NowVideoVisibility.uiHidden)
    }

    @Test
    fun `a tap of Select still opens Options`() {
        showNow()
        tap(KeyEvent.KEYCODE_BUTTON_SELECT)
        assertEquals(1, options)
        assertFalse(NowVideoVisibility.uiHidden)
    }

    @Test
    fun `with no preview a hold does nothing new`() {
        showing = false
        showNow()
        holdSelect()
        assertFalse(NowVideoVisibility.uiHidden)
        assertEquals(1, options)
        assertFalse(holdHint() in router.hintsFor(JarnScreen.Stage))
    }

    @Test
    fun `the preview going away, as on a game change, brings the UI back`() {
        showNow()
        holdSelect()
        rule.runOnUiThread { showing = false }
        rule.waitForIdle()
        assertFalse(NowVideoVisibility.uiHidden)
        tap(KeyEvent.KEYCODE_DPAD_DOWN)
        assertEquals(1, moves)
    }

    @Test
    fun `the bar shows the hold hint beside Options only while a preview shows`() {
        showNow()
        val hints = router.hintsFor(JarnScreen.Stage)
        assertTrue(JarnInputHint(JarnKey.Select, R.string.jarnfe_hint_context) in hints)
        assertTrue(holdHint() in hints)
        holdSelect()
        assertTrue(
            JarnInputHint(JarnKey.Select, R.string.jarnfe_hint_hold_show_ui) in router.hintsFor(JarnScreen.Stage),
        )
    }

    private fun holdHint() = JarnInputHint(JarnKey.Select, R.string.jarnfe_hint_hold_hide_ui)

    private companion object {
        const val TAP_MILLIS = 10L
        const val HOLD_MILLIS = 600L
    }
}
