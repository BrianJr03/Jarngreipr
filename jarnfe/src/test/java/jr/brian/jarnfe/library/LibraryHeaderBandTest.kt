package jr.brian.jarnfe.library

import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ViewSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

private const val EPS = 1e-4f

private fun ThemeRect.top() = y - originY * height
private fun ThemeRect.bottom() = top() + height

class LibraryHeaderBandTest {
    @Test
    fun `a full screen list moves below the band and keeps its bottom`() {
        val view = ViewSpec(ThemePrimary.Grid(LibraryFullBox))
        val box = view.clearingHeaderBand().primary.box
        assertEquals(LIBRARY_BELOW_HEADER, box.top(), EPS)
        assertEquals(1f, box.bottom(), EPS)
        assertEquals(LibraryFullBox.width, box.width, EPS)
    }

    @Test
    fun `a list already below the band is left alone`() {
        val view = ViewSpec(ThemePrimary.Shelf(ThemeRect(0.04f, 0.44f, 0.92f, 0.46f)))
        assertSame(view, view.clearingHeaderBand())
    }

    @Test
    fun `a centered box keeps its origin`() {
        val view = ViewSpec(ThemePrimary.List(ThemeRect(0.5f, 0.5f, 0.8f, 1f, originX = 0.5f, originY = 0.5f)))
        val box = view.clearingHeaderBand().primary.box
        assertEquals(LIBRARY_BELOW_HEADER, box.top(), EPS)
        assertEquals(1f, box.bottom(), EPS)
        assertEquals(0.5f, box.originY, EPS)
    }

    @Test
    fun `a grid copy lays out like the templates, clear of the strip`() {
        val copy = LibraryBuiltInThemes.Grid.spec.withSystemsBelowHeader()
        val box = copy.views.systems!!.primary.box
        assertEquals(LIBRARY_BELOW_HEADER, box.top(), EPS)
        LibraryTemplateThemes.All.forEach { template ->
            val templateBox = template.spec.views.systems!!.primary.box
            assertEquals(true, templateBox.top() >= LIBRARY_HEADER_BAND)
        }
    }
}
