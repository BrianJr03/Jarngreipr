package jr.brian.core.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnHint
import jr.brian.core.ui.components.JarnHintBar
import jr.brian.core.ui.components.JarnKey
import jr.brian.core.ui.components.jarnHintsThatFit
import jr.brian.core.ui.theme.JarnTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w1600dp-h400dp")
class JarnHintFitTest {

    @get:Rule
    val rule = createComposeRule()

    /** A game list's hints, the longest set the Library shows. */
    private val gameHints = listOf(
        JarnHint(JarnKey.A, "Play"),
        JarnHint(JarnKey.B, "Back"),
        JarnHint(JarnKey.Select, "Options"),
        JarnHint(JarnKey.Y, "Search"),
        JarnHint(JarnKey.Start, "Settings"),
        JarnHint(JarnKey.X, "Switch Screen"),
        JarnHint(JarnKey.L1R1, "Tabs"),
    )
    private val keys = gameHints.map { it.key }

    @Test
    fun `everything stays when it fits`() {
        assertEquals(keys.indices.toSet(), jarnHintsThatFit(keys, List(7) { 100 }, available = 760, gap = 10))
    }

    @Test
    fun `tabs drop first, then options, then search, keeping play, back, settings and switch screen`() {
        val widths = List(7) { 100 }
        assertEquals(setOf(0, 1, 2, 3, 4, 5), jarnHintsThatFit(keys, widths, available = 650, gap = 10))
        assertEquals(setOf(0, 1, 3, 4, 5), jarnHintsThatFit(keys, widths, available = 540, gap = 10))
        assertEquals(setOf(0, 1, 4, 5), jarnHintsThatFit(keys, widths, available = 430, gap = 10))
    }

    @Test
    fun `among equal ranks the later hint drops first`() {
        val plain = listOf(JarnKey.A, JarnKey.B, JarnKey.X)
        assertEquals(setOf(0, 1), jarnHintsThatFit(plain, listOf(50, 50, 50), available = 110, gap = 10))
    }

    @Test
    fun `a narrow bar drops whole hints and never cuts a label`() {
        rule.setContent {
            JarnTheme {
                Box(Modifier.width(300.dp).testTag("bar")) { JarnHintBar(gameHints) }
            }
        }
        val bar = rule.onNodeWithTag("bar").getBoundsInRoot()
        rule.onNodeWithText("Play").assertIsDisplayed()
        rule.onNodeWithText("Tabs").assertIsNotDisplayed()
        gameHints.forEach { hint ->
            val node = rule.onNodeWithText(hint.label)
            if (runCatching { node.assertIsDisplayed() }.isSuccess) {
                assertTrue("${hint.label} runs past the bar", node.getBoundsInRoot().right <= bar.right)
            }
        }
    }

    @Test
    fun `a wide bar keeps every hint`() {
        rule.setContent {
            JarnTheme {
                Box(Modifier.width(1400.dp)) { JarnHintBar(gameHints.take(4), trailing = gameHints.drop(4)) }
            }
        }
        gameHints.forEach { rule.onNodeWithText(it.label).assertIsDisplayed() }
    }
}
