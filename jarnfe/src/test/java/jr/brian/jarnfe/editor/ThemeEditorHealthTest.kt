package jr.brian.jarnfe.editor

import jr.brian.core.model.theme.ThemeColor
import jr.brian.jarnfe.R
import jr.brian.jarnfe.themekit.ThemeLayoutIssue
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeEditorHealthTest {
    @Test
    fun `videos weigh more than pictures`() {
        val light = editorWith(textLayer("a")).viewSpec!!.load
        assertEquals(ThemeLoad.Light, light)
        var heavy = editorWith()
        repeat(3) { heavy = heavy.withNewLayer(ThemeEditorNewLayer.Video) }
        assertEquals(ThemeLoad.Heavy, heavy.viewSpec!!.load)
    }

    @Test
    fun `issues point at their piece, and unreadable colors at the colors`() {
        val own = editorWith().withFollowsLook(false, editorWith().spec.tokens.colors)
        val grey = ThemeColor(0xFF888888.toInt())
        val unreadable = own.withColors(own.spec.tokens.colors.copy(bg = grey, surface = grey, raised = grey, textHigh = grey))
        val issues = unreadable.issues(listOf(ThemeLayoutIssue.TextInSafeZone("title"), ThemeLayoutIssue.PrimaryClamped))
        assertEquals(
            listOf(
                ThemeEditorIssue(null, R.string.jarnfe_editor_hard_to_read),
                ThemeEditorIssue(ThemeEditorTarget.Layer("title"), R.string.jarnfe_editor_issue_text_covered),
                ThemeEditorIssue(ThemeEditorTarget.Navigator, R.string.jarnfe_editor_issue_list_clamped),
            ),
            issues,
        )
    }
}
