package jr.brian.jarnfe.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JarnShellLogicTest {
    @Test
    fun `deck display is the first non primary display`() {
        assertEquals(4, pickDeckDisplayId(listOf(0, 4)))
        assertEquals(7, pickDeckDisplayId(listOf(7, 0, 9)))
    }

    @Test
    fun `single display has no deck display`() {
        assertNull(pickDeckDisplayId(listOf(0)))
        assertNull(pickDeckDisplayId(emptyList()))
    }
}
