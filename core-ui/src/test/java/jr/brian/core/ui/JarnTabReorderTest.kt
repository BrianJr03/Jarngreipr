package jr.brian.core.ui

import android.app.Application
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JARN_TAB_HOLD_MS
import jr.brian.core.ui.components.JarnHoldPress
import jr.brian.core.ui.components.JarnTabReorder
import jr.brian.core.ui.components.JarnTabs
import jr.brian.core.ui.components.jarnMovedOrder
import jr.brian.core.ui.components.jarnNormalizedOrder
import jr.brian.core.ui.components.rememberJarnTabReorder
import jr.brian.core.ui.focus.JarnTabSwitchFocus
import jr.brian.core.ui.focus.LocalJarnTabSwitchFocus
import jr.brian.core.ui.focus.LocalJarnTabsFocus
import jr.brian.core.ui.theme.JarnTheme
import androidx.compose.runtime.CompositionLocalProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val CONTENT_TAG = "content"

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnTabReorderTest {
    @get:Rule
    val rule = createComposeRule()

    private var order by mutableStateOf(listOf("Alpha", "Beta", "Gamma"))
    private var selected by mutableStateOf("Alpha")
    private var commits = 0
    private lateinit var reorder: JarnTabReorder

    /** [first] is selected and focused, as if the player had come to rest on it. */
    private fun show(first: String = "Alpha", rule: JarnTabSwitchFocus = JarnTabSwitchFocus(onShoulder = false, onDpad = false)) {
        selected = first
        this.rule.setContent {
            JarnTheme {
                CompositionLocalProvider(LocalJarnTabSwitchFocus provides rule) {
                    reorder = rememberJarnTabReorder { permutation ->
                        commits++
                        order = permutation.map { order[it] }
                    }
                    Column {
                        JarnTabs(
                            labels = order,
                            selectedIndex = order.indexOf(selected),
                            onSelect = { selected = order[it] },
                            reorder = reorder,
                        )
                        Content()
                    }
                }
            }
        }
        this.rule.waitForIdle()
        focusTab(first)
    }

    @androidx.compose.runtime.Composable
    private fun Content() {
        val requester = remember { FocusRequester() }
        val tabsFocus = LocalJarnTabsFocus.current
        LaunchedEffect(tabsFocus.handoffs) {
            withFrameNanos { }
            if (!tabsFocus.hasFocus) runCatching { requester.requestFocus() }
        }
        Box(Modifier.size(60.dp).testTag(CONTENT_TAG).focusRequester(requester).focusable())
    }

    private fun focusTab(label: String) {
        rule.onNodeWithText(label).requestFocus()
        rule.waitForIdle()
    }

    private fun holdA() {
        rule.onRoot().performKeyInput {
            keyDown(Key.ButtonA)
            advanceEventTime(JARN_TAB_HOLD_MS + 100)
            keyUp(Key.ButtonA)
        }
        rule.waitForIdle()
    }

    private fun press(key: Key) {
        rule.onRoot().performKeyInput { pressKey(key) }
        rule.waitForIdle()
    }

    @Test
    fun `a move steps one neighbour at a time and stops at the ends`() {
        assertEquals(listOf(1, 0, 2), jarnMovedOrder(listOf(0, 1, 2), 0, 1))
        assertEquals(listOf(0, 2, 1), jarnMovedOrder(listOf(0, 1, 2), 2, -1))
        assertEquals(listOf(0, 1, 2), jarnMovedOrder(listOf(0, 1, 2), 2, 1))
        assertEquals(listOf(0, 1, 2), jarnMovedOrder(listOf(0, 1, 2), 0, -1))
    }

    @Test
    fun `a saved order drops unknown ids, ignores repeats and adds missing tabs at the end`() {
        val known = listOf("a", "b", "c", "d")
        assertEquals(listOf("c", "a", "b", "d"), jarnNormalizedOrder(listOf("c", "gone", "a", "c"), known) { it })
        assertEquals(known, jarnNormalizedOrder(emptyList(), known) { it })
    }

    @Test
    fun `a press is a tap until it is held long enough`() {
        val press = JarnHoldPress()
        press.down(0, 0)
        assertEquals(JarnHoldPress.Release.Tap, press.up(JARN_TAB_HOLD_MS - 1))
        press.down(0, 0)
        assertTrue(press.down(JARN_TAB_HOLD_MS, 1))
        assertFalse(press.down(JARN_TAB_HOLD_MS + 50, 2))
        assertEquals(JarnHoldPress.Release.None, press.up(JARN_TAB_HOLD_MS + 60))
        press.down(0, 0)
        assertEquals(JarnHoldPress.Release.Hold, press.up(JARN_TAB_HOLD_MS + 1))
    }

    @Test
    fun `holding A lifts the tab, the D-pad moves it and A saves`() {
        show()
        holdA()
        assertTrue(reorder.isEditing)
        press(Key.DirectionRight)
        press(Key.DirectionRight)
        assertTrue("still on the strip", runCatching { rule.onNodeWithText("Alpha").assertIsFocused() }.isSuccess)
        press(Key.ButtonA)
        assertFalse(reorder.isEditing)
        assertEquals(listOf("Beta", "Gamma", "Alpha"), order)
        assertEquals(1, commits)
    }

    @Test
    fun `B puts the order back`() {
        show()
        holdA()
        press(Key.DirectionRight)
        press(Key.ButtonB)
        assertFalse(reorder.isEditing)
        assertEquals(listOf("Alpha", "Beta", "Gamma"), order)
        assertEquals(0, commits)
    }

    @Test
    fun `up and down stay on the strip and shoulders do nothing while lifted`() {
        show(first = "Beta")
        holdA()
        press(Key.DirectionDown)
        press(Key.ButtonR1)
        assertTrue(reorder.isEditing)
        rule.onNodeWithText("Beta").assertIsFocused()
        assertEquals("Beta", selected)
    }

    @Test
    fun `a tap of A on a tab keeps today's behaviour`() {
        show(first = "Beta")
        press(Key.ButtonA)
        assertFalse(reorder.isEditing)
        assertNull(reorder.held)
        assertEquals("Beta", selected)
        assertEquals(listOf("Alpha", "Beta", "Gamma"), order)
    }

    @Test
    fun `saving a new order is not a tab switch, so focus stays on the strip`() {
        show(first = "Beta", rule = JarnTabSwitchFocus.Both)
        holdA()
        press(Key.DirectionLeft)
        press(Key.ButtonA)
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        assertEquals(listOf("Beta", "Alpha", "Gamma"), order)
        rule.onNodeWithText("Beta").assertIsFocused()
        rule.onNodeWithTag(CONTENT_TAG).assertIsNotFocused()
    }

    @Test
    fun `long-press and drag a tab sideways drops it into its new place`() {
        show()
        rule.onNodeWithText("Alpha").performTouchInput {
            down(center)
            advanceEventTime(1_000)
            moveBy(Offset(width * 0.6f, 0f))
            moveBy(Offset(width * 0.6f, 0f))
            up()
        }
        rule.waitForIdle()
        assertFalse(reorder.isEditing)
        assertEquals("Alpha", order[1])
        assertEquals(1, commits)
    }
}
