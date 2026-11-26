package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnTabs
import jr.brian.core.ui.focus.JARN_TAB_DPAD_SETTLE_MS
import jr.brian.core.ui.focus.JarnTabSwitchFocus
import jr.brian.core.ui.focus.LocalJarnTabSwitchFocus
import androidx.compose.runtime.CompositionLocalProvider
import jr.brian.core.ui.theme.JarnMotion
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.ProvideJarnInput
import jr.brian.core.ui.theme.JarnTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.delay

private const val LOAD_SLACK_MS = 200L

/** More Up presses than the test grid has rows, to reach the tab row from any item. */
private const val MAX_ROWS_UP = 5

@OptIn(ExperimentalTestApi::class)
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

    /** Each tab remembers its 9th item, unless [remembered] says otherwise. */
    @Composable
    private fun Grid(remembered: (String) -> String = { "${it}9" }) {
        LibraryTabReveal(tab) {
            Box(Modifier.size(400.dp)) {
                LibraryFocusGrid(
                    items = ids(tab),
                    itemKey = { it },
                    focusedKey = remembered(tab),
                    onFocused = {},
                    cellMinSize = 80.dp,
                ) { id, modifier -> Box(modifier.size(80.dp).recordFocus(id).focusable()) }
            }
        }
    }

    private val router = JarnInputRouter()
    private val tabs = listOf("a", "b", "c")

    private var screenActive by mutableStateOf(true)
    private var switchFocus by mutableStateOf(JarnTabSwitchFocus.Both)

    private fun show(reducedMotion: Boolean = false, content: @Composable () -> Unit) {
        rule.setContent {
            JarnTheme(reducedMotion = reducedMotion, screenActive = screenActive) {
                CompositionLocalProvider(LocalJarnTabSwitchFocus provides switchFocus) {
                    ProvideJarnInput(router, JarnScreen.Stage) { Column { content() } }
                }
            }
        }
        rule.waitForIdle()
    }

    @Composable
    private fun TabsAndGrid() {
        JarnTabs(
            labels = tabs,
            selectedIndex = tabs.indexOf(tab),
            onSelect = { index ->
                switchedAt = rule.mainClock.currentTime
                tab = tabs[index]
            },
        )
        Grid()
    }

    private fun press(key: Key) {
        rule.onRoot().performKeyInput { pressKey(key) }
        rule.waitForIdle()
    }

    private fun focusTabRow() {
        rule.onNodeWithText("a").requestFocus()
        rule.waitForIdle()
        rule.onNodeWithText("a").assertIsFocused()
    }

    private fun switchTab(to: String) {
        rule.runOnIdle {
            switchedAt = rule.mainClock.currentTime
            tab = to
        }
        rule.waitForIdle()
    }

    private fun tabFocused(label: String): Boolean = runCatching { rule.onNodeWithText(label).assertIsFocused() }.isSuccess

    private fun delayOf(id: String) = focusedAt.getValue(id) - switchedAt

    @Test
    fun `the grid focuses the new tab's remembered item once the tab has slid open`() {
        show { Grid() }
        rule.onNodeWithTag("a9").assertIsFocused()
        switchTab("b")
        rule.onNodeWithTag("b9").assertIsFocused()
        assertTrue("focused after ${delayOf("b9")}ms", delayOf("b9") >= revealMillis)
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
        rule.onNodeWithTag("b9").assertIsFocused()
        assertTrue("focused after ${delayOf("b9")}ms", delayOf("b9") < revealMillis)
    }

    @Test
    fun `a d-pad switch on the tab row moves focus into the new tab's content once it rests`() {
        switchFocus = JarnTabSwitchFocus(onShoulder = false, onDpad = true)
        show { TabsAndGrid() }
        rule.onNodeWithTag("a9").assertIsFocused()
        focusTabRow()
        press(Key.DirectionRight)
        rule.onNodeWithText("b").assertIsFocused()
        restPastSettle()
        rule.onNodeWithTag("b9").assertIsFocused()
        assertTrue("focused after ${delayOf("b9")}ms", delayOf("b9") >= JARN_TAB_DPAD_SETTLE_MS)
    }

    private fun restPastSettle() {
        rule.mainClock.advanceTimeBy(JARN_TAB_DPAD_SETTLE_MS + LOAD_SLACK_MS)
        rule.waitForIdle()
    }

    @Test
    fun `by default a d-pad switch on the tab row drops in after the rest`() {
        show { TabsAndGrid() }
        focusTabRow()
        press(Key.DirectionRight)
        rule.onNodeWithText("b").assertIsFocused()
        restPastSettle()
        rule.onNodeWithTag("b9").assertIsFocused()
        assertTrue("focused after ${delayOf("b9")}ms", delayOf("b9") >= JARN_TAB_DPAD_SETTLE_MS)
    }

    @Test
    fun `in shoulders mode a d-pad switch on the tab row stays on the tabs`() {
        switchFocus = JarnTabSwitchFocus.Shoulders
        show { TabsAndGrid() }
        focusTabRow()
        press(Key.DirectionRight)
        restPastSettle()
        rule.onNodeWithText("b").assertIsFocused()
        rule.onNodeWithTag("b9").assertIsNotFocused()
    }

    @Test
    fun `an r1 switch on the tab row moves focus into the new tab's content`() {
        show { TabsAndGrid() }
        focusTabRow()
        press(Key.ButtonR1)
        rule.onNodeWithTag("b9").assertIsFocused()
        assertTrue("focused after ${delayOf("b9")}ms", delayOf("b9") >= revealMillis)
    }

    @Test
    fun `up from the content still returns to the tabs`() {
        show { TabsAndGrid() }
        focusTabRow()
        press(Key.ButtonR1)
        rule.onNodeWithTag("b9").assertIsFocused()
        repeat(MAX_ROWS_UP) { if (!tabFocused("b")) press(Key.DirectionUp) }
        rule.onNodeWithText("b").assertIsFocused()
    }

    @Test
    fun `a switch from the other screen changes the tab but never takes focus`() {
        show { TabsAndGrid() }
        focusTabRow()
        rule.runOnIdle {
            router.activate(JarnScreen.Deck)
            screenActive = false
        }
        switchTab("b")
        assertFalse("content took focus", focusedAt.keys.any { it.startsWith("b") })
        rule.onNodeWithTag("b9").assertIsNotFocused()
    }

    @Test
    fun `the first tab shown keeps its remembered item`() {
        show { Grid() }
        rule.onNodeWithTag("a9").assertIsFocused()
    }

    /** Like a section whose feed is still loading: the grid only appears [loadMillis] after the switch. */
    @Composable
    private fun LateGrid(loadMillis: Long) {
        LibraryTabReveal(tab) {
            var loaded by remember(tab) { mutableStateOf(tab == "a") }
            LaunchedEffect(tab) {
                delay(loadMillis)
                loaded = true
            }
            Box(Modifier.size(400.dp)) {
                if (loaded) {
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
    }

    /** Delays with no frame pending don't advance on their own, so step the clock past the load. */
    private fun letLoad(loadMillis: Long) {
        rule.mainClock.advanceTimeBy(loadMillis + LOAD_SLACK_MS)
        rule.waitForIdle()
    }

    @Test
    fun `reduced motion still focuses content that appears a few frames late`() {
        show(reducedMotion = true) { LateGrid(loadMillis = 100) }
        rule.onNodeWithTag("a9").assertIsFocused()
        switchTab("b")
        letLoad(100)
        rule.onNodeWithTag("b9").assertIsFocused()
    }

    @Test
    fun `content that loads after the slide still takes focus`() {
        show { LateGrid(loadMillis = revealMillis + 300) }
        rule.onNodeWithTag("a9").assertIsFocused()
        switchTab("b")
        letLoad(revealMillis + 300)
        rule.onNodeWithTag("b9").assertIsFocused()
    }

    @Test
    fun `an l1 step from the content keeps focus in content that loads late, with the tab row on screen`() {
        show(reducedMotion = true) {
            JarnTabs(labels = tabs, selectedIndex = tabs.indexOf(tab), onSelect = { tab = tabs[it] })
            LateGrid(loadMillis = 100)
        }
        rule.onNodeWithTag("a9").assertIsFocused()
        switchTab("b")
        letLoad(100)
        rule.onNodeWithTag("b9").assertIsFocused()
    }

    @Test
    fun `an l1 step from the content keeps focus in content that loads after the slide, with the tab row on screen`() {
        show {
            JarnTabs(labels = tabs, selectedIndex = tabs.indexOf(tab), onSelect = { tab = tabs[it] })
            LateGrid(loadMillis = revealMillis + 300)
        }
        rule.onNodeWithTag("a9").assertIsFocused()
        switchTab("b")
        letLoad(revealMillis + 300)
        rule.onNodeWithTag("b9").assertIsFocused()
    }

    @Test
    fun `content that never registers lets the gate open after the wait`() {
        val gate = LibraryRevealGate(closed = true, takesFocus = true)
        assertFalse(revealGateOpensNow(gate.takesFocus, gate.hasPendingFocus, waitedOut = false))
        assertTrue(revealGateOpensNow(gate.takesFocus, gate.hasPendingFocus, waitedOut = true))
        gate.whenOpen { true }
        assertTrue(revealGateOpensNow(gate.takesFocus, gate.hasPendingFocus, waitedOut = false))
        assertTrue("a switch from the other screen never waits", revealGateOpensNow(takesFocus = false, focusWaiting = false, waitedOut = false))
    }

    @Test
    fun `a switch falls back to the first item when the remembered one is gone`() {
        show { Grid(remembered = { if (it == "a") "a9" else "hidden" }) }
        rule.onNodeWithTag("a9").assertIsFocused()
        switchTab("b")
        rule.onNodeWithTag("b1").assertIsFocused()
    }

    @Test
    fun `the remembered item wins while it is still in the list`() {
        val keys = setOf("g1", "g2", "g6")
        assertEquals("g6", rememberedOrFirst("g6", keys, "g1"))
        assertEquals("g1", rememberedOrFirst("gone", keys, "g1"))
        assertEquals("g1", rememberedOrFirst(null, keys, "g1"))
        assertEquals(null, rememberedOrFirst(null, emptySet(), null))
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
