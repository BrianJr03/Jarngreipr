package jr.brian.home.esde.launch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppJarnGameOptionsTest {
    @Test
    fun `joins a relative path under a folder`() {
        assertEquals("primary:Roms/snes/a/b.sfc", childDocumentId("primary:Roms/snes", "./a/b.sfc"))
    }

    @Test
    fun `joins a relative path under a volume root`() {
        assertEquals("1A2B-3C4D:b.sfc", childDocumentId("1A2B-3C4D:", "b.sfc"))
    }

    @Test
    fun `library key keeps colons in the file name`() {
        assertEquals(LibraryKeyParts(4, "snes", "a:b.sfc"), parseLibraryKey("4:snes:a:b.sfc"))
    }

    @Test
    fun `malformed library key is rejected`() {
        assertNull(parseLibraryKey("x:snes:a.sfc"))
        assertNull(parseLibraryKey("4:snes"))
    }
}
