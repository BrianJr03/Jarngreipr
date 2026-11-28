package jr.brian.jarnfe.themekit

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeText
import jr.brian.core.model.theme.ViewSpec
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ThemeStackLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    private fun stacked(id: String, y: Float) = ThemeLayer.Text(
        id = id,
        rect = ThemeRect(0.5f, y, 0.4f, 0.2f),
        z = 2,
        content = ThemeText.Static(id),
        stack = "info",
    )

    @Test
    fun `a stacked layer with nothing to show leaves no hole`() {
        val view = ViewSpec(
            primary = ThemePrimary.Grid(ThemeRect(0f, 0f, 0.4f, 1f)),
            layers = listOf(stacked("title", 0.1f), stacked("rating", 0.35f), stacked("description", 0.6f)),
        )
        rule.setContent {
            Box(Modifier.size(400.dp)) {
                JarnThemeLayout(view = view, primary = {}, layer = { placed ->
                    if (placed.layer.id != "rating") {
                        Box(Modifier.fillMaxWidth().height(30.dp).testTag(placed.layer.id))
                    }
                }, insets = ThemeInsets())
            }
        }
        val title = rule.onNodeWithTag("title").getBoundsInRoot()
        val description = rule.onNodeWithTag("description").getBoundsInRoot()
        assertEquals(40.dp, title.top)
        assertEquals(title.bottom + 20.dp, description.top)
    }
}
