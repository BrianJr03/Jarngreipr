package jr.brian.core.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnTabs
import jr.brian.core.ui.components.JarnTabsStyle
import jr.brian.core.ui.theme.JarnTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnTabsScrollTest {

    @get:Rule
    val rule = createComposeRule()

    private val labels = List(12) { "Tab number $it" }

    private fun selectLastWithoutFocus(style: JarnTabsStyle) {
        var selected by mutableIntStateOf(0)
        rule.setContent {
            JarnTheme {
                val size = if (style == JarnTabsStyle.Top) Modifier.width(240.dp) else Modifier.size(240.dp, 160.dp)
                Box(size) { JarnTabs(labels, selected, { selected = it }, style = style) }
            }
        }
        rule.onNodeWithText(labels.last()).assertIsNotDisplayed()
        rule.runOnIdle { selected = labels.lastIndex }
        rule.waitForIdle()
        rule.onNodeWithText(labels.last()).assertIsDisplayed()
    }

    @Test
    fun `a top row scrolls to a tab selected without focus`() = selectLastWithoutFocus(JarnTabsStyle.Top)

    @Test
    fun `a rail scrolls to a tab selected without focus`() = selectLastWithoutFocus(JarnTabsStyle.Rail)
}
