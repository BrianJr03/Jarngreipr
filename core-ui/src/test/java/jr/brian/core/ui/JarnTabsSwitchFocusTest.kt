package jr.brian.core.ui

import android.app.Application
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnTabs
import jr.brian.core.ui.focus.JARN_TAB_DPAD_SETTLE_MS
import jr.brian.core.ui.focus.JarnTabSwitchFocus
import jr.brian.core.ui.focus.JarnTabSwitchSource
import jr.brian.core.ui.focus.LocalJarnTabSwitchFocus
import jr.brian.core.ui.focus.LocalJarnTabsFocus
import jr.brian.core.ui.focus.jarnTabSwitchTakesFocus
import jr.brian.core.ui.theme.JarnTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val SLACK_MS = 200L
private const val FAST_STEP_MS = 120L

/**
 * A strip over content shaped like Jarn's: each tab restores its remembered item on entry unless
 * the strip holds focus, and asks again on each handoff. The last tab is empty.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnTabsSwitchFocusTest {
    @get:Rule
    val rule = createComposeRule()

    private val tabs = listOf("Alpha", "Beta", "Gamma", "Empty")
    private var selected by mutableStateOf(0)
    private val remembered = mapOf("Alpha" to 1, "Beta" to 2, "Gamma" to 0)

    private val shoulders = JarnTabSwitchFocus.Shoulders
    private val dpad = JarnTabSwitchFocus(onShoulder = false, onDpad = true)
    private val both = JarnTabSwitchFocus(onShoulder = true, onDpad = true)
    private val neither = JarnTabSwitchFocus(onShoulder = false, onDpad = false)

    private fun show(rule: JarnTabSwitchFocus) {
        this.rule.setContent {
            JarnTheme {
                CompositionLocalProvider(LocalJarnTabSwitchFocus provides rule) {
                    Column {
                        JarnTabs(tabs, selected, onSelect = { selected = it })
                        key(selected) { TabContent(tabs[selected]) }
                    }
                }
            }
        }
        this.rule.waitForIdle()
    }

    @Composable
    private fun TabContent(tab: String) {
        val target = remembered[tab] ?: return
        val requesters = remember { List(3) { FocusRequester() } }
        val tabsFocus = LocalJarnTabsFocus.current
        LaunchedEffect(tabsFocus.handoffs) {
            withFrameNanos { }
            if (!tabsFocus.hasFocus) runCatching { requesters[target].requestFocus() }
        }
        Row {
            requesters.forEachIndexed { index, requester ->
                Box(
                    Modifier
                        .size(60.dp)
                        .testTag("$tab$index")
                        .focusRequester(requester)
                        .focusable(),
                )
            }
        }
    }

    private fun focusStrip() {
        rule.onNodeWithText("Alpha").requestFocus()
        rule.waitForIdle()
        rule.onNodeWithText("Alpha").assertIsFocused()
    }

    private fun press(key: Key) {
        rule.onRoot().performKeyInput { pressKey(key) }
        rule.waitForIdle()
    }

    private fun rest(millis: Long = JARN_TAB_DPAD_SETTLE_MS + SLACK_MS) {
        rule.mainClock.advanceTimeBy(millis)
        rule.waitForIdle()
    }

    private fun focused(tag: String) = runCatching { rule.onNodeWithTag(tag).assertIsFocused() }.isSuccess

    private fun stripFocused(label: String) = runCatching { rule.onNodeWithText(label).assertIsFocused() }.isSuccess

    private fun switchBy(source: JarnTabSwitchSource) {
        press(if (source == JarnTabSwitchSource.Dpad) Key.DirectionRight else Key.ButtonR1)
        rest()
    }

    private fun assertLands(rule: JarnTabSwitchFocus, source: JarnTabSwitchSource, inContent: Boolean) {
        show(rule)
        focusStrip()
        switchBy(source)
        assertEquals(1, selected)
        if (inContent) {
            assertTrue("$rule $source should drop into the tab", focused("Beta2"))
        } else {
            assertTrue("$rule $source should stay on the strip", stripFocused("Beta"))
        }
    }

    @Test
    fun `shoulders mode drops in on a shoulder`() = assertLands(shoulders, JarnTabSwitchSource.Shoulder, inContent = true)

    @Test
    fun `shoulders mode stays on the strip on the d-pad`() = assertLands(shoulders, JarnTabSwitchSource.Dpad, inContent = false)

    @Test
    fun `d-pad mode drops in on the d-pad`() = assertLands(dpad, JarnTabSwitchSource.Dpad, inContent = true)

    @Test
    fun `d-pad mode stays on the strip on a shoulder`() = assertLands(dpad, JarnTabSwitchSource.Shoulder, inContent = false)

    @Test
    fun `both mode drops in on a shoulder`() = assertLands(both, JarnTabSwitchSource.Shoulder, inContent = true)

    @Test
    fun `both mode drops in on the d-pad`() = assertLands(both, JarnTabSwitchSource.Dpad, inContent = true)

    @Test
    fun `neither mode stays on the strip on a shoulder`() = assertLands(neither, JarnTabSwitchSource.Shoulder, inContent = false)

    @Test
    fun `neither mode stays on the strip on the d-pad`() = assertLands(neither, JarnTabSwitchSource.Dpad, inContent = false)

    @Test
    fun `fast d-pad steps across three tabs stay on the strip, and resting drops in`() {
        show(dpad)
        focusStrip()
        press(Key.DirectionRight)
        rest(FAST_STEP_MS)
        assertTrue(stripFocused("Beta"))
        press(Key.DirectionRight)
        rest(FAST_STEP_MS)
        assertTrue(stripFocused("Gamma"))
        press(Key.DirectionLeft)
        rest(FAST_STEP_MS)
        assertTrue(stripFocused("Beta"))
        assertFalse(focused("Beta2"))
        rest()
        assertTrue(focused("Beta2"))
    }

    @Test
    fun `an empty tab leaves focus on the strip`() {
        selected = 2
        show(both)
        rule.onNodeWithText("Gamma").requestFocus()
        rule.waitForIdle()
        press(Key.ButtonR1)
        rest()
        assertEquals(3, selected)
        assertTrue(stripFocused("Empty"))
    }

    @Test
    fun `the new tab's remembered item takes focus, not its first`() {
        show(shoulders)
        focusStrip()
        press(Key.ButtonR1)
        rest()
        assertTrue(focused("Beta2"))
        assertFalse(focused("Beta0"))
    }

    @Test
    fun `a switch from the content lands in the new content even with neither, after the strip catches the fallback`() {
        show(neither)
        assertTrue(focused("Alpha1"))
        rule.runOnIdle { selected = 1 }
        rule.waitForIdle()
        rest()
        assertTrue(focused("Beta2"))
    }

    @Test
    fun `the first selection never moves focus`() {
        show(both)
        focusStrip()
        rest()
        assertTrue(stripFocused("Alpha"))
    }

    @Test
    fun `focus already in the content always follows, from the strip only the rule decides`() {
        assertTrue(jarnTabSwitchTakesFocus(neither, JarnTabSwitchSource.Shoulder, stripHadFocus = false))
        assertFalse(jarnTabSwitchTakesFocus(neither, JarnTabSwitchSource.Dpad, stripHadFocus = true))
        assertTrue(jarnTabSwitchTakesFocus(dpad, JarnTabSwitchSource.Dpad, stripHadFocus = true))
        assertFalse(jarnTabSwitchTakesFocus(dpad, JarnTabSwitchSource.Shoulder, stripHadFocus = true))
    }
}
