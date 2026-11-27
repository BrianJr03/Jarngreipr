package jr.brian.jarnfe.themekit

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ViewSpec
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnThemeLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    private val view = ViewSpec(
        primary = ThemePrimary.Shelf(ThemeRect(0.1f, 0.4f, 0.8f, 0.3f)),
        layers = listOf(
            ThemeLayer.Panel(id = "empty", rect = ThemeRect(0f, 0.8f, 0.2f, 0.1f)),
            ThemeLayer.Panel(id = "double", rect = ThemeRect(0f, 0f, 0.2f, 0.1f)),
            ThemeLayer.Panel(id = "single", rect = ThemeRect(0.5f, 0f, 0.2f, 0.1f)),
        ),
    )

    @Test
    fun `children land on their own rect when a layer emits nothing or two nodes`() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                JarnThemeLayout(
                    view = view,
                    primary = { Box(Modifier.testTag("primary")) },
                    layer = { placed -> TestLayer(placed.layer.id) },
                    modifier = Modifier.size(300.dp, 400.dp),
                    insets = ThemeInsets(),
                )
            }
        }
        assertEquals(Rect(0f, 0f, 60f, 40f), bounds("double-1"))
        assertEquals(Rect(0f, 0f, 60f, 40f), bounds("double-2"))
        assertEquals(Rect(150f, 0f, 210f, 40f), bounds("single"))
        assertEquals(Rect(30f, 160f, 270f, 280f), bounds("primary"))
    }

    private fun bounds(tag: String): Rect = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
}

@Composable
private fun TestLayer(id: String) {
    when (id) {
        "empty" -> Unit
        "double" -> {
            Box(Modifier.testTag("double-1"))
            Box(Modifier.testTag("double-2"))
        }
        else -> Box(Modifier.testTag(id))
    }
}
