package jr.brian.jarnfe.settings

import android.app.Application
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.focus.JarnRevealScrollTop
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Walks a page of groups with the D-pad and checks each heading shows with its first row. The
 * page itself takes focus first, so the first Down lands on the first row.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SettingsGroupTest {
    @get:Rule
    val rule = createComposeRule()

    private val headings = listOf(
        R.string.jarnfe_settings_tiles_title,
        R.string.jarnfe_settings_feedback_title,
        R.string.jarnfe_settings_two_screens_title,
        R.string.jarnfe_launcher_tours_title,
    )

    private fun string(id: Int) = ApplicationProvider.getApplicationContext<Application>().getString(id)

    private fun show() {
        rule.setContent {
            JarnTheme {
                Box(Modifier.size(320.dp, 260.dp).testTag(PAGE)) { Page() }
            }
        }
        rule.waitForIdle()
    }

    @Composable
    private fun Page() {
        val first = remember { FocusRequester() }
        val scroll = rememberScrollState()
        LaunchedEffect(Unit) { first.requestFocus() }
        JarnRevealScrollTop(scroll) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(first)
                    .focusGroup()
                    .verticalScroll(scroll)
                    .padding(vertical = JarnTheme.space.focusEdge),
                verticalArrangement = Arrangement.spacedBy(JarnTheme.space.lg),
            ) {
                headings.forEachIndexed { group, heading ->
                    SettingsGroup(heading) {
                        repeat(ROWS) { row ->
                            JarnNavigateRow(
                                label = "Row $group.$row",
                                onClick = {},
                                modifier = Modifier.fillMaxWidth().testTag(rowTag(group, row)),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun press(key: Key) {
        rule.onRoot().performKeyInput { pressKey(key) }
        rule.waitForIdle()
    }

    private fun assertHeadingVisible(group: Int) {
        val page = rule.onNodeWithTag(PAGE).getBoundsInRoot()
        val title = rule.onNodeWithText(string(headings[group])).getUnclippedBoundsInRoot()
        assertTrue("heading $group top ${title.top} vs ${page.top}", title.top >= page.top)
        assertTrue("heading $group bottom ${title.bottom} vs ${page.bottom}", title.bottom <= page.bottom)
    }

    @Test
    fun `walking up shows every heading with its first row`() {
        show()
        val total = headings.size * ROWS
        repeat(total) { press(Key.DirectionDown) }
        rule.onNodeWithTag(rowTag(headings.lastIndex, ROWS - 1)).assertIsFocused()
        for (index in total - 1 downTo 0) {
            val group = index / ROWS
            val row = index % ROWS
            rule.onNodeWithTag(rowTag(group, row)).assertIsFocused()
            if (row == 0) assertHeadingVisible(group)
            if (index > 0) press(Key.DirectionUp)
        }
    }

    @Test
    fun `walking down shows every heading with its first row`() {
        show()
        for (index in 0 until headings.size * ROWS) {
            press(Key.DirectionDown)
            rule.onNodeWithTag(rowTag(index / ROWS, index % ROWS)).assertIsFocused()
            if (index % ROWS == 0) assertHeadingVisible(index / ROWS)
        }
    }

    @Test
    fun `only the first row asks to reveal the heading`() {
        assertEquals(90f, settingsHeadingReveal(28f, 90f, headingBottom = 20f, gap = 8f))
        assertNull(settingsHeadingReveal(98f, 160f, headingBottom = 20f, gap = 8f))
    }

    private companion object {
        const val PAGE = "page"
        const val ROWS = 3

        fun rowTag(group: Int, row: Int) = "row-$group-$row"
    }
}
