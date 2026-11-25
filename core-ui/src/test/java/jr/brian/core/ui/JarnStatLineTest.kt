package jr.brian.core.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JARN_STAT_SEPARATOR
import jr.brian.core.ui.components.JarnStatLine
import jr.brian.core.ui.components.jarnStatsStack
import jr.brian.core.ui.theme.JarnTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val PLAYED = "23 min played"
private const val LAST = "Last played Nov 25, 2026"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class JarnStatLineTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `stats stay on one line while the joined line fits`() {
        assertFalse(jarnStatsStack(count = 2, joinedWidth = 300, maxWidth = 300))
        assertFalse(jarnStatsStack(count = 3, joinedWidth = 120, maxWidth = 400))
    }

    @Test
    fun `stats stack once the joined line runs past the width`() {
        assertTrue(jarnStatsStack(count = 2, joinedWidth = 301, maxWidth = 300))
    }

    @Test
    fun `a single stat never stacks`() {
        assertFalse(jarnStatsStack(count = 1, joinedWidth = 900, maxWidth = 300))
    }

    @Test
    fun `an unbounded width always fits`() {
        assertFalse(jarnStatsStack(count = 2, joinedWidth = 900, maxWidth = Constraints.Infinity))
    }

    @Test
    fun `a wide box shows the joined line`() {
        setStats(width = 1000.dp)
        rule.onNodeWithText(PLAYED + JARN_STAT_SEPARATOR + LAST).assertExists()
        rule.onAllNodesWithText(PLAYED).assertCountEquals(0)
    }

    @Test
    fun `a narrow box shows one stat per line`() {
        setStats(width = 160.dp)
        rule.onNodeWithText(PLAYED).assertExists()
        rule.onNodeWithText(LAST).assertExists()
        rule.onAllNodesWithText(PLAYED + JARN_STAT_SEPARATOR + LAST).assertCountEquals(0)
    }

    private fun setStats(width: Dp) {
        rule.setContent {
            JarnTheme {
                Box(Modifier.width(width)) {
                    JarnStatLine(listOf(PLAYED, LAST), JarnTheme.type.label, JarnTheme.colors.textHigh)
                }
            }
        }
    }
}
