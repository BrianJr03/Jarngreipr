package jr.brian.jarnfe.transition

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import jr.brian.core.ui.theme.JarnTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnDepthPushTest {
    @get:Rule
    val rule = createComposeRule()

    private var level by mutableStateOf(SYSTEMS)
    private var gamesClicks by mutableIntStateOf(0)

    private fun show(reducedMotion: Boolean = false) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            JarnTheme(reducedMotion = reducedMotion) {
                JarnDepthPush(
                    target = level,
                    levelOf = { it },
                    anchor = { null },
                    modifier = Modifier.fillMaxSize(),
                ) { shown -> Level(shown) }
            }
        }
        rule.mainClock.advanceTimeByFrame()
    }

    @Composable
    private fun Level(shown: Int) {
        val label = if (shown == SYSTEMS) SYSTEMS_LABEL else GAMES_LABEL
        Box(Modifier.fillMaxSize().clickable { if (shown == GAMES) gamesClicks++ }) { BasicText(label) }
    }

    private fun advance(millis: Long) {
        rule.mainClock.advanceTimeBy(millis)
        rule.waitForIdle()
    }

    @Test
    fun `the open finishes inside 280ms`() {
        show()
        level = GAMES
        advance(OPEN_MILLIS + FRAME_SLACK_MILLIS)
        rule.onNodeWithText(SYSTEMS_LABEL).assertDoesNotExist()
        rule.onNodeWithText(GAMES_LABEL).assertExists()
    }

    @Test
    fun `the games take presses while the push is still running`() {
        show()
        level = GAMES
        advance(MID_WAY_MILLIS)
        rule.onNodeWithText(SYSTEMS_LABEL).assertExists()
        rule.onNodeWithText(GAMES_LABEL).performClick()
        assertEquals(1, gamesClicks)
    }

    @Test
    fun `going back mid-way reverses without dropping the systems`() {
        show()
        level = GAMES
        advance(MID_WAY_MILLIS)
        level = SYSTEMS
        advance(FRAME_SLACK_MILLIS)
        rule.onNodeWithText(SYSTEMS_LABEL).assertExists()
        advance(BACK_MILLIS + FRAME_SLACK_MILLIS)
        rule.onNodeWithText(GAMES_LABEL).assertDoesNotExist()
        rule.onNodeWithText(SYSTEMS_LABEL).assertExists()
    }

    @Test
    fun `reduced motion crossfades in 120ms`() {
        show(reducedMotion = true)
        level = GAMES
        advance(REDUCED_MILLIS + FRAME_SLACK_MILLIS)
        rule.onNodeWithText(SYSTEMS_LABEL).assertDoesNotExist()
        rule.onNodeWithText(GAMES_LABEL).assertExists()
    }

    private companion object {
        const val SYSTEMS = 0
        const val GAMES = 1
        const val SYSTEMS_LABEL = "systems"
        const val GAMES_LABEL = "games"
        const val OPEN_MILLIS = 280L
        const val BACK_MILLIS = 260L
        const val REDUCED_MILLIS = 120L
        const val MID_WAY_MILLIS = 100L
        const val FRAME_SLACK_MILLIS = 48L
    }
}
