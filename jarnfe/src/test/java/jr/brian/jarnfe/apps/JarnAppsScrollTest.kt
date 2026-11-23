package jr.brian.jarnfe.apps

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import jr.brian.core.ui.theme.JarnSpacing
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.dock.JarnDockApp
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.ProvideJarnInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Drives the real drawer with D-pad keys and measures what the shared scroll rule leaves visible. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnAppsScrollTest {
    @get:Rule
    val rule = createComposeRule()

    private val edge: Dp = JarnSpacing().focusEdge
    private val apps = (10..49).map { JarnDockApp("pkg.$it", "App $it") }
    private val pinned = apps.take(3).map { it.packageName }
    private val hidden = setOf("pkg.48", "pkg.49")
    private var snapshot by mutableStateOf(JarnAppsSnapshot(apps, pinned, hidden))
    private val options = FakeJarnAppOptions()
    private val opened = mutableListOf<String>()

    private fun string(id: Int) = ApplicationProvider.getApplicationContext<Application>().getString(id)

    private fun show() {
        rule.setContent {
            JarnTheme {
                ProvideJarnInput(JarnInputRouter(), JarnScreen.Stage) {
                    Box(Modifier.size(320.dp, 300.dp)) {
                        JarnAppsDrawer(snapshot, JarnAppsCommands({ opened += it }, options))
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun press(key: Key, times: Int = 1) = repeat(times) {
        rule.onRoot().performKeyInput { pressKey(key) }
        rule.waitForIdle()
    }

    private fun assertClearOfEdges(node: SemanticsNodeInteraction) {
        val list = rule.onNodeWithTag(JARN_APPS_LIST_TAG).getBoundsInRoot()
        val item = node.getBoundsInRoot()
        assertTrue("top ${item.top} vs ${list.top + edge}", item.top >= list.top + edge - FocusGrowth)
        assertTrue("bottom ${item.bottom} vs ${list.bottom - edge}", item.bottom <= list.bottom - edge + FocusGrowth)
    }

    private fun assertVisible(text: String) {
        val list = rule.onNodeWithTag(JARN_APPS_LIST_TAG).getBoundsInRoot()
        val title = rule.onNodeWithText(text).getBoundsInRoot()
        assertTrue("$text top ${title.top}", title.top >= list.top)
        assertTrue("$text bottom ${title.bottom}", title.bottom <= list.bottom)
    }

    private companion object {
        const val MAX_PRESSES = 40

        /** The focused tile scales up by at most this much per side, inside the edge margin. */
        val FocusGrowth = 6.dp
    }

    @Test
    fun `the first item is focused with its title showing`() {
        show()
        rule.onNodeWithTag("pkg.10").assertIsFocused()
        assertVisible(string(R.string.jarnfe_apps_section_pinned))
        assertClearOfEdges(rule.onNodeWithTag("pkg.10"))
    }

    @Test
    fun `the next section title comes into view with its first row`() {
        show()
        press(Key.DirectionDown)
        assertVisible(string(R.string.jarnfe_apps_section_all))
    }

    private fun walkToHiddenHeader(): Int {
        var presses = 0
        while (rule.onAllNodes(hasTestTag(JARN_APPS_HIDDEN_KEY) and isFocused()).fetchSemanticsNodes().isEmpty()) {
            assertTrue("never reached the accordion", presses++ < MAX_PRESSES)
            press(Key.DirectionDown)
            assertClearOfEdges(rule.onNode(isFocused()))
        }
        return presses
    }

    @Test
    fun `walking to the bottom and back never touches an edge`() {
        show()
        val presses = walkToHiddenHeader()
        repeat(presses) {
            press(Key.DirectionUp)
            assertClearOfEdges(rule.onNode(isFocused()))
        }
        rule.onNodeWithTag("pkg.10").assertIsFocused()
        assertVisible(string(R.string.jarnfe_apps_section_pinned))
    }

    @Test
    fun `the accordion expands and its last row stays clear of the bottom`() {
        show()
        walkToHiddenHeader()
        rule.onNodeWithTag(JARN_APPS_HIDDEN_KEY).performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        press(Key.DirectionDown)
        rule.onNodeWithTag("pkg.48").assertIsFocused()
        assertClearOfEdges(rule.onNodeWithTag("pkg.48"))
    }

    @Test
    fun `a plain tile opens directly when ask on launch is off`() {
        show()
        rule.onNodeWithTag("pkg.10").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        assertEquals(listOf("pkg.10"), opened)
    }

    @Test
    fun `a plain tile asks which display when ask on launch is on`() {
        options.current = options.current.copy(askOnLaunch = true, hasSecondDisplay = true)
        show()
        rule.onNodeWithTag("pkg.10").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        assertTrue(opened.isEmpty())
        rule.onNodeWithText(string(R.string.jarnfe_folder_which_display)).assertExists()
    }

    @Test
    fun `focus moves to a neighbor when its app is uninstalled`() {
        show()
        rule.onNodeWithTag("pkg.10").assertIsFocused()
        rule.runOnIdle { snapshot = snapshot.copy(apps = apps.drop(1)) }
        rule.waitForIdle()
        rule.onNodeWithTag("pkg.11").assertIsFocused()
    }
}
