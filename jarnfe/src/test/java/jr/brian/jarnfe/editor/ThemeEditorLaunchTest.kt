package jr.brian.jarnfe.editor

import jr.brian.core.model.theme.ThemeSource
import jr.brian.jarnfe.library.LibraryBuiltInTheme
import jr.brian.jarnfe.themes.JarnThemeEntry
import jr.brian.jarnfe.themes.JarnThemeKind
import jr.brian.jarnfe.themes.JarnThemeRegistry
import jr.brian.jarnfe.themes.JarnUserThemeStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ThemeEditorLaunchTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val store by lazy { JarnUserThemeStore(temp.root) }

    @Test
    fun `code layouts are copied, never edited in place`() {
        assertFalse(JarnThemeRegistry.Grid.editableInPlace)
        assertFalse(JarnThemeRegistry.templates.first().editableInPlace)
    }

    @Test
    fun `a copy is saved at once, follows the look and gets its own id`() {
        val (id, spec) = store.duplicate(JarnThemeRegistry.Carousel, "Carousel copy")
        assertEquals("carousel-copy", id)
        assertEquals("Carousel copy", spec.meta.name)
        assertTrue(spec.meta.followsLook)
        assertEquals(spec, store.theme(id))
        assertEquals("carousel-copy-2", store.duplicate(JarnThemeRegistry.Carousel, "Carousel copy").first)
    }

    @Test
    fun `a copy of an import keeps its colors and its source`() {
        val esde = JarnThemeRegistry.Grid.theme.spec.let { it.copy(meta = it.meta.copy(source = ThemeSource.EsDe, license = "CC BY-NC")) }
        val entry = JarnThemeEntry("esde", JarnThemeKind.Imported, LibraryBuiltInTheme(esde), ownColors = true)
        val (_, copy) = store.duplicate(entry, "Mine")
        assertFalse(copy.meta.followsLook)
        assertEquals(ThemeSource.EsDe, copy.meta.source)
        assertEquals("CC BY-NC", copy.meta.license)
    }

    @Test
    fun `editing opens the draft before the saved theme`() {
        val (id, saved) = store.duplicate(JarnThemeRegistry.Grid, "Mine")
        val entry = JarnThemeEntry(id, JarnThemeKind.User, LibraryBuiltInTheme(saved))
        assertEquals(saved, store.editableSpec(entry))
        val draft = saved.copy(meta = saved.meta.copy(name = "Mine draft"))
        store.saveDraft(id, draft)
        assertEquals(draft, store.editableSpec(entry))
    }

    @Test
    fun `a copy of a copy numbers on instead of stacking copy`() {
        val plain = { name: String -> "$name copy" }
        val numbered = { name: String, n: Int -> "$name copy $n" }
        assertEquals("Grid", copyBaseName("Grid copy", plain, numbered))
        assertEquals("Grid", copyBaseName("Grid copy 3", plain, numbered))
        assertEquals("Grid", copyBaseName("Grid", plain, numbered))
        assertEquals("Copycat", copyBaseName("Copycat", plain, numbered))
        val next = nextCopyName(plain(copyBaseName("Grid copy", plain, numbered)), { numbered("Grid", it) }, setOf("Grid copy"))
        assertEquals("Grid copy 2", next)
    }

    @Test
    fun `a copy of a built-in leaves the header band free`() {
        val (_, copy) = store.duplicate(JarnThemeRegistry.Grid, "Grid copy")
        val box = copy.views.systems!!.primary.box
        assertTrue(box.y - box.originY * box.height >= jr.brian.jarnfe.library.LIBRARY_HEADER_BAND)
        assertEquals(JarnThemeRegistry.Grid.theme.spec.views.games, copy.views.games)
    }

    @Test
    fun `copies are numbered once the plain name is taken`() {
        val numbered = { n: Int -> "Grid copy $n" }
        assertEquals("Grid copy", nextCopyName("Grid copy", numbered, setOf("Grid")))
        assertEquals("Grid copy 2", nextCopyName("Grid copy", numbered, setOf("Grid copy")))
        assertEquals("Grid copy 4", nextCopyName("Grid copy", numbered, setOf("Grid copy", "Grid copy 2", "Grid copy 3")))
    }
}
