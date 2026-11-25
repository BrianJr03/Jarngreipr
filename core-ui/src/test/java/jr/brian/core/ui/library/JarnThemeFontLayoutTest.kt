package jr.brian.core.ui.library

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnSheet
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnBaseTypeScope
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.core.ui.theme.JarnType
import jr.brian.core.ui.theme.jarnTypeWithFonts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val HERO_TITLE = "The Legend of Zelda Breath of the Wild"
private const val HEADING = "Continue Playing"
private const val LABEL = "Recently Added"

/** Michroma, Bangers, Baloo 2 and the other wide faces at their measured scales. */
private val WideKeys = listOf("chrome_wide", "comic", "rounded", "stencil", "mono")

/** Scaled faces may run a little wider than Barlow, never far past it. */
private const val MAX_WIDTH_RATIO = 1.35f

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class JarnThemeFontLayoutTest {

    @get:Rule
    val rule = createComposeRule()

    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
        return results.single()
    }

    @Test
    fun `hero titles fit their box in every wide face`() {
        rule.setContent {
            Column {
                WideKeys.forEach { key ->
                    val type = jarnTypeWithFonts(key, key)
                    JarnTheme(type = type) {
                        Box(Modifier.requiredSize(320.dp, 140.dp)) {
                            JarnBalancedTitle(HERO_TITLE, type.display, JarnTheme.colors.textHigh)
                        }
                    }
                }
            }
        }
        val titles = rule.onAllNodesWithContentDescription(HERO_TITLE, useUnmergedTree = true)
        WideKeys.forEachIndexed { index, key ->
            val layout = titles[index].textLayout()
            val boxWidth = layout.layoutInput.constraints.maxWidth
            assertTrue(key, layout.lineCount <= 2)
            assertFalse(key, layout.didOverflowHeight)
            (0 until layout.lineCount).forEach { line ->
                assertFalse("$key line $line", layout.isLineEllipsized(line))
                assertTrue("$key line $line", layout.getLineRight(line) <= boxWidth)
            }
        }
    }

    @Test
    fun `scaled faces keep headings and labels near Barlow's width`() {
        val widths = mutableMapOf<String, Float>()
        rule.setContent {
            val measurer = rememberTextMeasurer()
            fun width(text: String, style: TextStyle) = measurer.measure(text, style, maxLines = 1).size.width.toFloat()
            val base = JarnType()
            widths["base heading"] = width(HEADING, base.heading)
            widths["base label"] = width(LABEL, base.label)
            WideKeys.forEach { key ->
                val type = jarnTypeWithFonts(key, key)
                widths["$key heading"] = width(HEADING, type.heading)
                widths["$key label"] = width(LABEL, type.label)
            }
        }
        rule.waitForIdle()
        WideKeys.forEach { key ->
            listOf("heading", "label").forEach { role ->
                val ratio = widths.getValue("$key $role") / widths.getValue("base $role")
                assertTrue("$key $role is ${ratio}x Barlow", ratio <= MAX_WIDTH_RATIO)
            }
        }
        assertNotEquals(widths["base heading"], widths["chrome_wide heading"])
    }

    @Test
    fun `base type scope ignores the font override`() {
        val override = jarnTypeWithFonts("chrome_wide", "comic")
        var outer: JarnType? = null
        var inner: JarnType? = null
        rule.setContent {
            JarnTheme(type = override) {
                outer = JarnTheme.type
                JarnBaseTypeScope { inner = JarnTheme.type }
            }
        }
        rule.runOnIdle {
            assertEquals(override, outer)
            assertEquals(JarnTheme.baseType, inner)
            assertEquals(JarnType(), JarnTheme.baseType)
        }
    }

    @Test
    fun `sheets render on Jarn's own type`() {
        var inSheet: JarnType? = null
        rule.setContent {
            JarnTheme(type = jarnTypeWithFonts("stencil", "stencil")) {
                JarnSheet(visible = true, onDismiss = {}) {
                    inSheet = JarnTheme.type
                    JarnText(LABEL, JarnTheme.type.label, JarnTheme.colors.textHigh)
                }
            }
        }
        rule.runOnIdle { assertEquals(JarnTheme.baseType, inSheet) }
    }
}
