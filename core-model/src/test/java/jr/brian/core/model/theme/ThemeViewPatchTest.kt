package jr.brian.core.model.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeViewPatchTest {
    private val box = ThemeRect(0f, 0f, 1f, 1f)
    private val narrowBox = ThemeRect(0f, 0.2f, 1f, 0.8f)

    private fun panel(id: String, y: Float = 0f) = ThemeLayer.Panel(id = id, rect = ThemeRect(0f, y, 0.5f, 0.1f))

    private val view = ViewSpec(
        primary = ThemePrimary.Grid(box = box, columns = 5, itemAspect = 0.75f),
        layers = listOf(panel("a"), panel("b"), panel("c")),
        narrow = ThemeViewPatch(
            primary = ThemePrimaryPatch(box = narrowBox, columns = 3, rows = 9, itemsBefore = 1),
            layers = listOf(panel("b", y = 0.5f), panel("d")),
            hide = listOf("c"),
        ),
    )

    @Test
    fun `a wide area keeps the view as written`() {
        val wide = view.forArea(1920, 805)
        assertEquals(view.primary, wide.primary)
        assertEquals(view.layers, wide.layers)
        assertNull(wide.narrow)
    }

    @Test
    fun `a narrow area applies the patch`() {
        val narrow = view.forArea(1240, 860)
        assertEquals(ThemePrimary.Grid(box = narrowBox, columns = 3, itemAspect = 0.75f), narrow.primary)
        assertEquals(listOf(panel("a"), panel("b", y = 0.5f), panel("d")), narrow.layers)
        assertNull(narrow.narrow)
    }

    @Test
    fun `the split sits between a 4 by 3 and a 16 by 9 browse area`() {
        assertEquals(5, (view.forArea(1800, 1000).primary as ThemePrimary.Grid).columns)
        assertEquals(3, (view.forArea(1790, 1000).primary as ThemePrimary.Grid).columns)
    }

    @Test
    fun `each list type takes only the fields it has`() {
        val patch = ThemeViewPatch(ThemePrimaryPatch(itemsBefore = 1, itemsAfter = 0, columns = 2, rows = 4))
        val shelf = ViewSpec(ThemePrimary.Shelf(box), narrow = patch).forArea(4, 3).primary as ThemePrimary.Shelf
        assertEquals(1, shelf.itemsBefore)
        assertEquals(0, shelf.itemsAfter)
        val list = ViewSpec(ThemePrimary.List(box), narrow = patch).forArea(4, 3).primary as ThemePrimary.List
        assertEquals(4, list.rows)
    }

    @Test
    fun `a view without a patch is returned as is`() {
        val plain = ViewSpec(ThemePrimary.Shelf(box))
        assertSame(plain, plain.forArea(4, 3))
    }

    @Test
    fun `a patch with unknown keys and layer types still decodes`() {
        val text = """
            {"primary":{"type":"shelf","box":{"x":0.0,"y":0.0,"width":1.0,"height":1.0}},
            "narrow":{"primary":{"itemsBefore":1,"tilt":3},"layers":[{"type":"sparkle","id":"s"}],"future":true}}
        """.trimIndent().replace("\n", "")
        val decoded = JarnThemeFormat.json.decodeFromString(ViewSpec.serializer(), text)
        assertEquals(ThemeViewPatch(ThemePrimaryPatch(itemsBefore = 1)), decoded.narrow)
    }

    @Test
    fun `a narrow edit becomes the smallest patch`() {
        val wide = ViewSpec(ThemePrimary.Grid(box, columns = 5), layers = listOf(panel("a"), panel("b")))
        val edited = wide.narrowView().copy(
            primary = ThemePrimary.Grid(narrowBox, columns = 3),
            layers = listOf(panel("a", y = 0.4f), panel("c")),
        )
        val patched = wide.withNarrowEdit(edited)
        assertEquals(wide.primary, patched.primary)
        assertEquals(wide.layers, patched.layers)
        assertEquals(ThemeViewPatch(ThemePrimaryPatch(box = narrowBox, columns = 3), listOf(panel("a", y = 0.4f), panel("c")), listOf("b")), patched.narrow)
        assertEquals(edited.layers.toSet(), patched.narrowView().layers.toSet())
        assertNull(wide.withNarrowEdit(wide.narrowView()).narrow)
    }

    @Test
    fun `settings one screen can't hold apply to both`() {
        val wide = ViewSpec(ThemePrimary.Grid(box, columns = 5))
        val asShelf = wide.withNarrowEdit(ViewSpec(ThemePrimary.Shelf(narrowBox)))
        assertEquals(ThemePrimary.Shelf(box), asShelf.primary)
        assertEquals(ThemePrimaryPatch(box = narrowBox), asShelf.narrow?.primary)
    }

    @Test
    fun `resetting drops one piece's entries, or the list's`() {
        val patched = view.withoutNarrowLayer("b").withoutNarrowLayer("c")
        assertEquals(listOf(panel("d")), patched.narrow?.layers)
        assertEquals(emptyList<String>(), patched.narrow?.hide)
        assertTrue(view.narrowChanges("c") && view.narrowChanges("b") && !view.narrowChanges("a"))
        assertNull(view.withoutNarrowPrimary().narrow?.primary)
        assertNull(ViewSpec(ThemePrimary.Shelf(box), narrow = ThemeViewPatch(hide = listOf("x"))).withoutNarrowLayer("x").narrow)
    }
}
