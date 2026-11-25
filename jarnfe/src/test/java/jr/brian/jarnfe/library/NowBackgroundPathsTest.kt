package jr.brian.jarnfe.library

import jr.brian.jarnfe.systems.JarnManagedSystem
import jr.brian.jarnfe.systems.JarnSystemAppearance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NowBackgroundPathsTest {
    private val system = JarnLibrarySystem(id = "snes", title = "SNES", gameCount = 3)

    @Test
    fun `picked background comes first, then system art, then scene art`() {
        val picked = system.copy(nowBackgroundPath = "content://picked", artPath = "bg")
        assertEquals(
            listOf("content://picked", "bg", "scene"),
            nowSystemPaths(picked, JarnSystemNow("snes", sceneArtPath = "scene")),
        )
    }

    @Test
    fun `picked background shows even before the system art is known`() {
        val picked = system.copy(nowBackgroundPath = "content://picked")
        assertEquals(listOf("content://picked"), nowSystemPaths(picked, JarnSystemNow("snes")))
    }

    @Test
    fun `blank or repeated picks are skipped`() {
        assertNull(nowSystemPaths(system.copy(nowBackgroundPath = " "), JarnSystemNow("snes")))
        assertEquals(
            listOf("bg"),
            nowSystemPaths(system.copy(nowBackgroundPath = "bg", artPath = "bg"), JarnSystemNow("snes")),
        )
    }

    @Test
    fun `without a pick the order is unchanged`() {
        assertEquals(
            listOf("bg", "scene"),
            nowSystemPaths(system.copy(artPath = "bg"), JarnSystemNow("snes", sceneArtPath = "scene")),
        )
    }

    @Test
    fun `only systems with a picked background are mapped`() {
        val systems = listOf(
            managed("snes", "content://snes"),
            managed("gba", null),
            managed("n64", ""),
        )
        assertEquals(mapOf("snes" to "content://snes"), nowBackgroundsById(systems))
    }

    @Test
    fun `no manager means no picks`() = runTest {
        assertEquals(emptyMap<String, String>(), nowBackgrounds(null).first())
    }

    private fun managed(id: String, focus: String?) = JarnManagedSystem(
        id = id,
        title = id,
        gameCount = 1,
        isHidden = false,
        appearance = JarnSystemAppearance(focusBackgroundUri = focus),
    )
}
