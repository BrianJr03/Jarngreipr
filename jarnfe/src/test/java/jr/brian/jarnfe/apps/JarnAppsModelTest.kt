package jr.brian.jarnfe.apps

import jr.brian.jarnfe.dock.JarnDockApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnAppsModelTest {
    private fun app(name: String) = JarnDockApp("pkg.$name", name)

    private val apps = listOf(app("zeta"), app("Alpha"), app("beta"), app("Gamma"))

    private fun List<JarnDockApp>.names() = map { it.label }

    @Test
    fun `every app shows in All from A to Z by default`() {
        val sections = jarnAppSections(JarnAppsSnapshot(apps, emptyList(), emptySet()))
        assertEquals(listOf("Alpha", "beta", "Gamma", "zeta"), sections.all.names())
        assertTrue(sections.pinned.isEmpty())
        assertTrue(sections.hidden.isEmpty())
    }

    @Test
    fun `a pinned app leaves All and keeps pin order`() {
        val sections = jarnAppSections(JarnAppsSnapshot(apps, listOf("pkg.zeta", "pkg.Alpha"), emptySet()))
        assertEquals(listOf("zeta", "Alpha"), sections.pinned.names())
        assertEquals(listOf("beta", "Gamma"), sections.all.names())
    }

    @Test
    fun `a hidden pin shows only in Hidden`() {
        val sections = jarnAppSections(JarnAppsSnapshot(apps, listOf("pkg.beta"), setOf("pkg.beta")))
        assertTrue(sections.pinned.isEmpty())
        assertEquals(listOf("beta"), sections.hidden.names())
        assertTrue("beta" !in sections.all.names())
    }

    @Test
    fun `a pin for an uninstalled app is ignored`() {
        val sections = jarnAppSections(JarnAppsSnapshot(apps, listOf("pkg.gone"), emptySet()))
        assertTrue(sections.pinned.isEmpty())
    }

    @Test
    fun `hidden rows appear only while expanded`() {
        val sections = jarnAppSections(JarnAppsSnapshot(apps, emptyList(), setOf("pkg.beta")))
        assertEquals(listOf("pkg.Alpha", "pkg.Gamma", "pkg.zeta", JARN_APPS_HIDDEN_KEY),
            jarnAppRows(sections, expanded = false, columns = 2).focusOrder())
        assertEquals("pkg.beta", jarnAppRows(sections, expanded = true, columns = 2).focusOrder().last())
    }

    @Test
    fun `focus moves to the next survivor, then the previous`() {
        val before = listOf("a", "b", "c")
        assertEquals("b", jarnAppsFocusAfter(before, before, "b"))
        assertEquals("c", jarnAppsFocusAfter(before, listOf("a", "c"), "b"))
        assertEquals("b", jarnAppsFocusAfter(before, listOf("a", "b"), "c"))
        assertNull(jarnAppsFocusAfter(before, emptyList(), "a"))
    }
}
