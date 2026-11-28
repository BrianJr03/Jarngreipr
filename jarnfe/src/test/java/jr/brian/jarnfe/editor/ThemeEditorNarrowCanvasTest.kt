package jr.brian.jarnfe.editor

import jr.brian.core.model.theme.THEME_NARROW_ASPECT
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemePrimaryPatch
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeViewPatch
import jr.brian.core.model.theme.ViewSpec
import jr.brian.core.model.theme.forArea
import jr.brian.jarnfe.library.LibraryTemplateThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** #576: the narrow canvas is shaped so a view's narrow layout is the one drawn. */
class ThemeEditorNarrowCanvasTest {
    @Test
    fun `the narrow canvas is under the narrow cutoff`() {
        assertTrue(NARROW_CANVAS_ASPECT < THEME_NARROW_ASPECT)
    }

    @Test
    fun `a canvas at that shape draws the narrow patch`() {
        val view = ViewSpec(
            primary = ThemePrimary.Grid(ThemeRect(0f, 0f, 1f, 1f), columns = 5),
            narrow = ThemeViewPatch(ThemePrimaryPatch(columns = 3)),
        )
        val height = 700
        val width = (height * NARROW_CANVAS_ASPECT).toInt()
        assertEquals(3, (view.forArea(width, height).primary as ThemePrimary.Grid).columns)
    }

    @Test
    fun `the editor opens wide and the toggle is part of its state`() {
        val state = ThemeEditorState(themeId = "t", spec = LibraryTemplateThemes.CoverGrid.spec)
        assertFalse(state.narrow)
        assertTrue(state.copy(narrow = true).narrow)
    }
}
