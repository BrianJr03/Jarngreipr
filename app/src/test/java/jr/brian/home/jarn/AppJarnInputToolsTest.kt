package jr.brian.home.jarn

import jr.brian.home.model.BackButtonShortcut
import jr.brian.jarnfe.tools.JarnBackShortcut
import org.junit.Assert.assertEquals
import org.junit.Test

class AppJarnInputToolsTest {

    @Test
    fun `every stored shortcut maps both ways`() {
        assertEquals(BackButtonShortcut.entries.size, JarnBackShortcut.entries.size)
        BackButtonShortcut.entries.forEach { assertEquals(it, it.toJarn().toStored()) }
    }
}
