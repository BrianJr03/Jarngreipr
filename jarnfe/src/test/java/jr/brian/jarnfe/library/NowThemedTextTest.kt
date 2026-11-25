package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import jr.brian.core.ui.library.JarnHeroPanel
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.core.ui.theme.jarnTypeWithFonts
import jr.brian.jarnfe.shell.JarnWordmark
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val LAST_PLAYED = "Last played today"
private const val HEADER = "In this system"
private const val EYEBROW = "Continue playing"
private const val MIXED = "Pok\u00e9mon \u30bc\u30eb\u30c0"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NowThemedTextTest {
    @get:Rule
    val rule = createComposeRule()

    private val type = jarnTypeWithFonts("chrome_wide", "comic")

    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
        return results.single()
    }

    @Test
    fun `wordmark and Continue playing follow the override, shelf labels stay on Jarn`() {
        rule.setContent {
            JarnTheme(type = type) {
                Column {
                    NowPill(HEADER, strong = true)
                    NowPill(EYEBROW, strong = true, themed = true)
                    NowPill(MIXED, strong = true, themed = true)
                    JarnWordmark()
                }
            }
        }
        val header = rule.onNodeWithText(HEADER).textLayout().layoutInput.style
        val eyebrow = rule.onNodeWithText(EYEBROW).textLayout().layoutInput.style
        val mixed = rule.onNodeWithText(MIXED).textLayout().layoutInput.style
        val mark = rule.onNodeWithText("JARN").textLayout().layoutInput.style
        assertEquals(JarnTheme.baseType.captionStrong.fontFamily, header.fontFamily)
        assertEquals(type.labelStrong.fontFamily, eyebrow.fontFamily)
        assertEquals(JarnTheme.baseType.captionStrong.fontFamily, mixed.fontFamily)
        assertEquals(JarnTheme.baseType.captionStrong.fontSize, mixed.fontSize)
        assertEquals(type.heading.fontFamily, mark.fontFamily)
    }

    @Test
    fun `top screen Continue playing body follows the resume style`() {
        var expected: androidx.compose.ui.text.font.FontFamily? = null
        rule.setContent {
            JarnTheme(type = type) {
                expected = resumeStatsStyle().fontFamily
                JarnHeroPanel(eyebrow = null, title = "Okami", body = LAST_PLAYED, bodyStyle = resumeStatsStyle(), art = {})
            }
        }
        val body = rule.onNodeWithText(LAST_PLAYED).textLayout().layoutInput.style
        assertEquals(type.label.fontFamily, expected)
        assertEquals(expected, body.fontFamily)
    }
}
