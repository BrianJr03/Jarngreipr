package jr.brian.jarnfe.themes

import jr.brian.core.model.theme.ThemeSource
import jr.brian.jarnfe.library.LibraryBuiltInThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class JarnUserThemeStoreTest {
    @get:Rule
    val temp = TemporaryFolder()

    private var clock = 1_000L
    private val store by lazy { JarnUserThemeStore(temp.root) { clock++ } }
    private val spec = LibraryBuiltInThemes.Carousel.spec.let { it.copy(meta = it.meta.copy(name = "Mine", followsLook = true)) }

    @Test
    fun `saving keeps the theme, a version and drops the draft`() {
        store.saveDraft("mine", spec)
        assertEquals(spec, store.draft("mine"))
        store.save("mine", spec)
        assertEquals(spec, store.theme("mine"))
        assertNull(store.draft("mine"))
        assertEquals(1, store.history("mine").size)
    }

    @Test
    fun `history keeps the last twenty, newest first`() {
        repeat(25) { n -> store.save("mine", spec.copy(meta = spec.meta.copy(version = "$n"))) }
        val history = store.history("mine")
        assertEquals(20, history.size)
        assertEquals("24", history.first().spec.meta.version)
        assertTrue(history.zipWithNext().all { (a, b) -> a.savedAt > b.savedAt })
    }

    @Test
    fun `listing skips hidden folders and themes without a file`() {
        store.save("mine", spec)
        temp.newFolder(".incoming-x")
        temp.newFolder("empty")
        assertEquals(listOf("mine"), store.list().map { it.id })
    }

    @Test
    fun `new ids are folder safe and unique`() {
        assertEquals("my-big-shelf", store.newId("My Big Shelf!"))
        store.save("my-big-shelf", spec)
        assertEquals("my-big-shelf-2", store.newId("My Big Shelf!"))
    }

    @Test
    fun `saved themes list beside imports and keep their own colors setting`() {
        store.save("mine", spec)
        store.save("esde", spec.copy(meta = spec.meta.copy(source = ThemeSource.EsDe, followsLook = false)))
        val entries = readImportedThemes(temp.root).associateBy { it.id }
        assertFalse(entries.getValue("mine").ownColors)
        assertTrue(entries.getValue("esde").ownColors)
    }
}
