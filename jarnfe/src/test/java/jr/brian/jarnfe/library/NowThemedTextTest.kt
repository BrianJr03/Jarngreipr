package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.core.ui.theme.jarnTypeWithFonts
import jr.brian.jarnfe.shell.JarnWordmark
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val HEADER = "In this system"
private const val COUNT = "12 games"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
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
    fun `shelf headers, counts and the wordmark follow the font override`() {
        rule.setContent {
            JarnTheme(type = type) {
                Column {
                    NowPill(HEADER, strong = true)
                    NowPill(COUNT, strong = false)
                    JarnWordmark()
                }
            }
        }
        val header = rule.onNodeWithText(HEADER).textLayout().layoutInput.style
        val count = rule.onNodeWithText(COUNT).textLayout().layoutInput.style
        val mark = rule.onNodeWithText("JARN").textLayout().layoutInput.style
        assertEquals(type.labelStrong.fontFamily, header.fontFamily)
        assertEquals(type.label.fontFamily, count.fontFamily)
        assertEquals(type.heading.fontFamily, mark.fontFamily)
    }
}
