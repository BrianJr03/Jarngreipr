package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnMotion
import jr.brian.core.ui.theme.JarnTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w1280dp-h720dp")
class LibraryTabRevealTest {
    @get:Rule
    val rule = createComposeRule()

    private var tab by mutableStateOf("a")
    private var switchedAt = 0L
    private val focusedAt = HashMap<String, Long>()
    private val revealMillis = JarnMotion().let { it.tabReveal.durationMillis + it.tabFocusSettleMillis }

    private fun ids(prefix: String) = (1..12).map { "$prefix$it" }

    private fun Modifier.recordFocus(id: String) = testTag(id).onFocusChanged {
        if (it.isFocused) focusedAt.putIfAbsent(id, rule.mainClock.currentTime)
    }

    @Composable
    private fun Grid() {
        LibraryTabReveal(tab) {
            Box(Modifier.size(400.dp)) {
                LibraryFocusGrid(
                    items = ids(tab),
                    itemKey = { it },
                    focusedKey = "${tab}9",
                    onFocused = {},
                    cellMinSize = 80.dp,
                ) { id, modifier -> Box(modifier.size(80.dp).recordFocus(id).focusable()) }
            }
        }
    }

    private fun show(reducedMotion: Boolean = false, content: @Composable () -> Unit) {
        rule.setContent { JarnTheme(reducedMotion = reducedMotion) { Column { content() } } }
        rule.waitForIdle()
    }

    private fun switchTab(to: String) {
        rule.runOnIdle {
            switchedAt = rule.mainClock.currentTime
            tab = to
        }
        rule.waitForIdle()
    }

    private fun delayOf(id: String) = focusedAt.getValue(id) - switchedAt

    @Test
    fun `the grid focuses its first on-screen item once the tab has slid open`() {
        show { Grid() }
        rule.onNodeWithTag("a9").assertIsFocused()
        switchTab("b")
        rule.onNodeWithTag("b1").assertIsFocused()
        assertTrue("focused after ${delayOf("b1")}ms", delayOf("b1") >= revealMillis)
    }

    @Test
    fun `the carousel focuses its middle item once the tab has slid open`() {
        val content = LibraryShelfContent<String>(
            itemKey = { it },
            itemTitle = { it },
            onOpen = {},
            itemModifier = { Modifier.recordFocus(it) },
            art = {},
        )
        show {
            LibraryTabReveal(tab) {
                LibraryShelf(ids(tab), LibraryShelfKind.Games, content, focusedKey = "${tab}3", onFocused = {})
            }
        }
        rule.onNodeWithTag("a3").assertIsFocused()
        switchTab("b")
        rule.onNodeWithTag("b3").assertIsFocused()
        assertTrue("focused after ${delayOf("b3")}ms", delayOf("b3") >= revealMillis)
    }

    @Test
    fun `reduced motion focuses on the next frame`() {
        show(reducedMotion = true) { Grid() }
        switchTab("b")
        rule.onNodeWithTag("b1").assertIsFocused()
        assertTrue("focused after ${delayOf("b1")}ms", delayOf("b1") < revealMillis)
    }

    @Test
    fun `the first tab shown keeps its remembered item`() {
        show { Grid() }
        rule.onNodeWithTag("a9").assertIsFocused()
    }

    @Test
    fun `an early press opens the gate and focuses the new tab at once`() {
        val gate = LibraryRevealGate(closed = true)
        var focused = 0
        gate.whenOpen {
            focused++
            true
        }
        assertEquals(0, focused)
        assertTrue(gate.open())
        assertEquals(1, focused)
        assertFalse("a second open does nothing", gate.open())
        gate.whenOpen {
            focused++
            true
        }
        assertEquals("once open, focus requests run at once", 2, focused)
    }
}
