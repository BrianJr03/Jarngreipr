package jr.brian.jarnfe.transition

import android.app.Application
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import jr.brian.core.ui.theme.JarnTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnHeaderSwapTest {
    @get:Rule
    val rule = createComposeRule()

    private var route by mutableIntStateOf(TABS)

    private fun show(reducedMotion: Boolean = false) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            JarnTheme(reducedMotion = reducedMotion) {
                JarnHeaderSwap(target = route, levelOf = { it.takeIf { level -> level != OTHER } }) { shown ->
                    BasicText(labelOf(shown))
                }
            }
        }
        rule.mainClock.advanceTimeByFrame()
    }

    private fun labelOf(shown: Int) = when (shown) {
        TABS -> TABS_LABEL
        TITLE -> TITLE_LABEL
        else -> OTHER_LABEL
    }

    private fun advance(millis: Long) {
        rule.mainClock.advanceTimeBy(millis)
        rule.waitForIdle()
    }

    @Test
    fun `tabs crossfade to the title within 40ms plus 160ms`() {
        show()
        route = TITLE
        advance(MID_MILLIS)
        rule.onNodeWithText(TABS_LABEL).assertExists()
        rule.onNodeWithText(TITLE_LABEL).assertExists()
        advance(FULL_MILLIS - MID_MILLIS + SLACK_MILLIS)
        rule.onNodeWithText(TABS_LABEL).assertDoesNotExist()
    }

    @Test
    fun `reduced motion crossfades in 120ms`() {
        show(reducedMotion = true)
        route = TITLE
        advance(REDUCED_MILLIS + SLACK_MILLIS)
        rule.onNodeWithText(TABS_LABEL).assertDoesNotExist()
        rule.onNodeWithText(TITLE_LABEL).assertExists()
    }

    @Test
    fun `a route outside the push swaps at once`() {
        show()
        route = OTHER
        advance(SLACK_MILLIS)
        rule.onNodeWithText(TABS_LABEL).assertDoesNotExist()
        rule.onNodeWithText(OTHER_LABEL).assertExists()
    }

    private companion object {
        const val TABS = 0
        const val TITLE = 1
        const val OTHER = 2
        const val TABS_LABEL = "tabs"
        const val TITLE_LABEL = "title"
        const val OTHER_LABEL = "settings"
        const val FULL_MILLIS = 200L
        const val MID_MILLIS = 100L
        const val REDUCED_MILLIS = 120L
        const val SLACK_MILLIS = 48L
    }
}
