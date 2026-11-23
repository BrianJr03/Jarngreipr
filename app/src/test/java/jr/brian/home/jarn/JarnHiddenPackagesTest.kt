package jr.brian.home.jarn

import jr.brian.home.model.PageType
import org.junit.Assert.assertEquals
import org.junit.Test

class JarnHiddenPackagesTest {
    @Test
    fun `an app counts as hidden only when every app page hides it`() {
        val types = listOf(PageType.APPS_TAB, PageType.APP_DRAWER_TAB)
        val hidden = mapOf(0 to setOf("both", "first"), 1 to setOf("both"))
        assertEquals(setOf("both"), jarnHiddenPackages(types, hidden))
    }

    @Test
    fun `pages that never list apps do not block hiding`() {
        val types = listOf(PageType.APPS_TAB, PageType.UNIFIED_CANVAS, PageType.RSS_TAB)
        assertEquals(setOf("a"), jarnHiddenPackages(types, mapOf(0 to setOf("a"))))
    }

    @Test
    fun `with only canvas pages the canvas pages decide`() {
        val types = listOf(PageType.UNIFIED_CANVAS)
        assertEquals(setOf("a"), jarnHiddenPackages(types, mapOf(0 to setOf("a"))))
    }

    @Test
    fun `no pages means nothing is hidden`() {
        assertEquals(emptySet<String>(), jarnHiddenPackages(emptyList(), mapOf(0 to setOf("a"))))
    }

    @Test
    fun `unhide also clears a stale page`() {
        assertEquals(setOf(0, 1, 7), jarnUnhidePages(2, mapOf(7 to setOf("a"), 8 to setOf("b")), "a"))
    }
}
