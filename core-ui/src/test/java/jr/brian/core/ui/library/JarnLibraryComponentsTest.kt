package jr.brian.core.ui.library

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val LONG_TITLE =
    "An Extremely Long Game Title That Cannot Possibly Fit Inside A Single Library Tile Caption"

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class JarnLibraryComponentsTest {

    @get:Rule
    val rule = createComposeRule()

    private fun setScaledContent(fontScale: Float = 1f, content: @Composable () -> Unit) {
        rule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                JarnTheme(content = content)
            }
        }
    }

    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
        return results.single()
    }

    private fun assertInside(inner: SemanticsNodeInteraction, outer: SemanticsNodeInteraction) {
        val child = inner.fetchSemanticsNode().boundsInRoot
        val parent = outer.fetchSemanticsNode().boundsInRoot
        assertTrue("$child escapes $parent", child.top >= parent.top && child.bottom <= parent.bottom)
        assertTrue("$child escapes $parent", child.left >= parent.left && child.right <= parent.right)
    }

    @Test
    fun `game tile takes focus and gamepad A clicks once`() {
        var clicks = 0
        val requester = FocusRequester()
        setScaledContent {
            JarnGameTile("game", { clicks++ }, Modifier.width(120.dp).testTag("t").focusRequester(requester)) {
                JarnArtPlaceholder("game")
            }
        }
        rule.runOnIdle { requester.requestFocus() }
        rule.onNodeWithTag("t").assertIsFocused()
        rule.onNodeWithTag("t").performKeyInput { pressKey(Key.ButtonA) }
        rule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun `system tile takes focus and gamepad A clicks once`() {
        var clicks = 0
        val requester = FocusRequester()
        setScaledContent {
            JarnSystemTile("system", "12", { clicks++ }, Modifier.width(136.dp).testTag("t").focusRequester(requester)) {}
        }
        rule.runOnIdle { requester.requestFocus() }
        rule.onNodeWithTag("t").assertIsFocused()
        rule.onNodeWithTag("t").performKeyInput { pressKey(Key.ButtonA) }
        rule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun `game tile exposes its title once to accessibility`() {
        setScaledContent {
            JarnGameTile("game", {}, Modifier.width(120.dp)) { JarnArtPlaceholder("game") }
        }
        rule.onNodeWithContentDescription("game").assertExists()
        rule.onNodeWithText("game").assertDoesNotExist()
    }

    @Test
    fun `system tile title truncates to one line inside the tile at font scale 1_3`() {
        setScaledContent(fontScale = 1.3f) {
            JarnSystemTile(LONG_TITLE, "214", {}, Modifier.width(136.dp).testTag("t")) {}
        }
        val title = rule.onNodeWithText(LONG_TITLE, useUnmergedTree = true)
        val layout = title.textLayout()
        assertEquals(1, layout.lineCount)
        assertTrue(layout.isLineEllipsized(0))
        assertInside(title, rule.onNodeWithTag("t"))
        assertInside(rule.onNodeWithText("214", useUnmergedTree = true), rule.onNodeWithTag("t"))
    }

    @Test
    fun `placeholder shows a capped readable title`() {
        setScaledContent(fontScale = 1.3f) {
            Box(Modifier.size(112.dp, 140.dp).testTag("p")) { JarnArtPlaceholder(LONG_TITLE) }
        }
        val title = rule.onNodeWithText(LONG_TITLE)
        assertTrue(title.textLayout().lineCount <= 3)
        assertInside(title, rule.onNodeWithTag("p"))
    }

    @Test
    fun `placeholder hides its title when the surface already captions it`() {
        setScaledContent {
            CompositionLocalProvider(LocalJarnArtCaptioned provides true) {
                JarnArtPlaceholder("game")
            }
        }
        rule.onNodeWithText("game").assertDoesNotExist()
    }

    @Test
    fun `hero keeps all text inside the panel at font scale 1_3`() {
        setScaledContent(fontScale = 1.3f) {
            JarnHeroPanel(
                eyebrow = "eyebrow",
                title = LONG_TITLE,
                body = LONG_TITLE.repeat(6) + "body",
                modifier = Modifier.size(833.dp, 468.dp).testTag("h"),
                art = { Box(Modifier.fillMaxSize()) },
                footer = { Box(Modifier.size(100.dp, 40.dp).testTag("f")) },
            )
        }
        val panel = rule.onNodeWithTag("h")
        val title = rule.onNodeWithText(LONG_TITLE)
        assertEquals(2, title.textLayout().lineCount)
        assertInside(title, panel)
        assertInside(rule.onNodeWithText("eyebrow"), panel)
        assertInside(rule.onNodeWithTag("f"), panel)
    }

    @Test
    fun `metadata plate renders odd entries and truncates long values`() {
        setScaledContent(fontScale = 1.3f) {
            JarnMetadataPlate(
                entries = listOf("Genre" to LONG_TITLE, "Players" to "1", "Released" to "1994"),
                modifier = Modifier.width(240.dp).testTag("m"),
            )
        }
        val value = rule.onNodeWithText(LONG_TITLE, useUnmergedTree = true)
        assertEquals(1, value.textLayout().lineCount)
        assertInside(value, rule.onNodeWithTag("m"))
        assertInside(rule.onNodeWithText("1994", useUnmergedTree = true), rule.onNodeWithTag("m"))
    }

    @Test
    fun `metadata plate emits nothing without entries`() {
        setScaledContent {
            Box(Modifier.testTag("root")) { JarnMetadataPlate(emptyList(), Modifier.testTag("m")) }
        }
        rule.onNodeWithTag("m").assertDoesNotExist()
    }
}
