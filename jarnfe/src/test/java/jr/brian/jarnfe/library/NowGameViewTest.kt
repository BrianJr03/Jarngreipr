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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme
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
}
