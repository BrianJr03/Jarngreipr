package jr.brian.jarnfe.editor

import jr.brian.core.model.theme.ThemeRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeVersionDiffTest {
    private val then = editorWith(textLayer("title"), textLayer("dev"))

    @Test
    fun `the same version reads as no changes`() {
        assertTrue(themeVersionDiff(then.spec, then.spec).same)
    }

    @Test
    fun `pieces are counted once each`() {
        val now = then
            .withoutLayer("dev")
            .withNewLayer(ThemeEditorNewLayer.Picture)
            .copy(selected = ThemeEditorTarget.Layer("title"))
            .let { it.withSelectedRect(ThemeRect(0.5f, 0.5f, 0.2f, 0.1f)).withLayer("title") { l -> l.withOpacity(0.5f) } }
        assertEquals(ThemeVersionDiff(added = 1, removed = 1, changed = 1, listChanged = false, colorsChanged = false), themeVersionDiff(then.spec, now.spec))
    }

    @Test
    fun `list and colors changes are flagged`() {
        val following = then.withFollowsLook(true, then.spec.tokens.colors)
        val now = following.withPrimary { it.nextType() }.withFollowsLook(false, then.spec.tokens.colors)
        val diff = themeVersionDiff(following.spec, now.spec)
        assertTrue(diff.listChanged)
        assertTrue(diff.colorsChanged)
    }
}
