package jr.brian.jarnfe.themekit

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import jr.brian.core.model.theme.GameTextField
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeText
import jr.brian.core.model.theme.ViewSpec
import jr.brian.jarnfe.library.JarnGameMetadata
import jr.brian.jarnfe.library.JarnLibraryGame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnThemeDecorTest {
    @get:Rule
    val rule = createComposeRule()

    private val view = ViewSpec(
        primary = ThemePrimary.Shelf(ThemeRect(0.1f, 0.5f, 0.8f, 0.3f)),
        layers = listOf(
            ThemeLayer.Text(id = "title", rect = ThemeRect(0.1f, 0.1f, 0.8f, 0.1f), z = 1, content = ThemeText.Game(GameTextField.Title)),
            ThemeLayer.Text(id = "dev", rect = ThemeRect(0.1f, 0.2f, 0.8f, 0.1f), z = 1, content = ThemeText.Game(GameTextField.Developer)),
        ),
    )

    @Test
    fun `text layers follow focus and hide when the field is missing`() {
        var focused by mutableStateOf<FocusedItem>(game("a", "Chrono Trigger", developer = "Square"))
        rule.setContent {
            JarnThemeDecorView(
                view = view,
                focused = focused,
                primary = { Box(Modifier) },
                modifier = Modifier.size(400.dp, 300.dp),
                insets = ThemeInsets(),
            )
        }
        rule.onNodeWithTag(THEME_TEXT_TAG + "title").assertTextEquals("Chrono Trigger")
        rule.onNodeWithTag(THEME_TEXT_TAG + "dev").assertTextEquals("Square")

        focused = game("b", "EarthBound")
        rule.waitForIdle()
        rule.onNodeWithTag(THEME_TEXT_TAG + "title").assertTextEquals("EarthBound")
        rule.onNodeWithTag(THEME_TEXT_TAG + "dev").assertDoesNotExist()

        focused = FocusedItem.None
        rule.waitForIdle()
        rule.onNodeWithTag(THEME_TEXT_TAG + "title").assertDoesNotExist()
    }

    private fun game(id: String, title: String, developer: String? = null) =
        FocusedItem.Game(JarnLibraryGame(id = id, systemId = "snes", title = title, metadata = JarnGameMetadata(developer = developer)))
}
