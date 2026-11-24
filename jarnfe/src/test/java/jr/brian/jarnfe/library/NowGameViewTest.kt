package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NowGameViewTest {
    @get:Rule
    val rule = createComposeRule()

    private val bare = JarnLibraryGame(id = "g", systemId = "s", title = "Lantern Reach")

    @Test
    fun `a game with no art or metadata shows its plain title`() {
        rule.setContent {
            JarnTheme { Box(Modifier.size(538.dp, 380.dp)) { NowGameView(bare) } }
        }
        rule.onNodeWithText("Lantern Reach").assertIsDisplayed()
    }

    @Test
    fun `it survives being placed inside a vertical scroller`() {
        rule.setContent {
            JarnTheme {
                Box(Modifier.size(538.dp, 380.dp)) {
                    Column(Modifier.verticalScroll(rememberScrollState())) { NowGameView(bare) }
                }
            }
        }
        rule.onNodeWithText("Lantern Reach").assertIsDisplayed()
    }

    private fun posterBounds(height: Int): Pair<DpRect, DpRect> {
        rule.setContent {
            JarnTheme { Box(Modifier.size(THOR_DECK_WIDTH.dp, height.dp)) { NowGameView(bare) } }
        }
        rule.waitForIdle()
        val case = rule.onNodeWithTag(NOW_CASE_TAG).getUnclippedBoundsInRoot()
        val text = rule.onNodeWithTag(NOW_POSTER_TEXT_TAG).getUnclippedBoundsInRoot()
        return case to text
    }

    @Test
    fun `the regular case grows while the text column keeps its place`() {
        val (case, text) = posterBounds(height = 420)
        assertEquals(146f, (case.right - case.left).value, 0.5f)
        assertEquals(165f, (case.bottom - case.top).value, 0.5f)
        assertEquals(170f, text.left.value, 0.5f)
        assertTrue(case.right < text.left)
        assertTrue(case.bottom.value <= 420f)
    }

    @Test
    fun `the compact case grows while the text column keeps its place`() {
        val (case, text) = posterBounds(height = 330)
        assertEquals(120f, (case.right - case.left).value, 0.5f)
        assertEquals(137f, (case.bottom - case.top).value, 0.5f)
        assertEquals(144f, text.left.value, 0.5f)
        assertTrue(case.right < text.left)
        assertTrue(case.bottom.value <= 330f)
    }

    @Test
    fun `description and metadata are shown below the poster`() {
        val game = bare.copy(
            metadata = JarnGameMetadata(description = "A short trip home.", developer = "Paper Crane"),
        )
        rule.setContent {
            JarnTheme { Box(Modifier.size(538.dp, 380.dp)) { NowGameView(game) } }
        }
        rule.onNodeWithText("A short trip home.").assertExists()
        rule.onNodeWithText("Paper Crane").assertExists()
    }

    private companion object {
        /** The Thor's bottom screen, 1240 by 1080 pixels at 369 dpi. */
        const val THOR_DECK_WIDTH = 538
    }
}
