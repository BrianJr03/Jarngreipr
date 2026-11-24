package jr.brian.home.esde.launch

import jr.brian.jarnfe.launch.JarnGameFileStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class RomFileStatusTest {
    @Test
    fun `a file is only present when it was really seen`() {
        assertEquals(JarnGameFileStatus.Missing, romFileStatus(missing = true, canSeeAllFiles = true))
        assertEquals(JarnGameFileStatus.Missing, romFileStatus(missing = true, canSeeAllFiles = false))
        assertEquals(JarnGameFileStatus.Present, romFileStatus(missing = false, canSeeAllFiles = true))
        assertEquals(JarnGameFileStatus.Unknown, romFileStatus(missing = false, canSeeAllFiles = false))
    }
}
