package jr.brian.jarnfe.canvas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JarnCanvasFoldersTest {
    @Test
    fun `editing keeps stored order and apps that are not installed right now`() {
        val next = jarnFolderPackages(listOf("gone", "a", "b"), setOf("c"), setOf("a"))
        assertEquals(listOf("gone", "b", "c"), next)
    }

    @Test
    fun `adding an app already stored does not duplicate it`() {
        assertEquals(listOf("a"), jarnFolderPackages(listOf("a"), setOf("a"), emptySet()))
    }

    @Test
    fun `removing every stored app leaves an empty list`() {
        assertEquals(emptyList<String>(), jarnFolderPackages(listOf("a", "b"), emptySet(), setOf("a", "b")))
    }

    @Test
    fun `the picker includes folder apps with no launcher entry, sorted by name`() {
        val installed = listOf(JarnCanvasApp("z", "Zed"), JarnCanvasApp("a", "Alpha"))
        val inFolder = listOf(JarnCanvasApp("m", "mid"), JarnCanvasApp("a", "Alpha"))
        assertEquals(listOf("a", "m", "z"), jarnFolderPickerApps(installed, inFolder).map { it.packageName })
    }

    @Test
    fun `a blank name is rejected and a real name is trimmed`() {
        assertNull(jarnFolderNameOrNull("   "))
        assertEquals("Games", jarnFolderNameOrNull("  Games "))
    }

    @Test
    fun `opposite display flips both ways`() {
        assertEquals(JarnAppDisplay.Primary, JarnAppDisplay.Current.opposite())
        assertEquals(JarnAppDisplay.Current, JarnAppDisplay.Primary.opposite())
    }
}
