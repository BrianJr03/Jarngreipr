package jr.brian.home.data

import jr.brian.frontend.library.db.SourceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StaleDerivedRootsTest {

    private val root = SourceEntity(id = 1, treeUri = "/storage/3A0D-5000/Ayn Thor", displayPath = "", lastScannedAt = 1)

    @Test
    fun `parent saved by a system folder pick is stale`() {
        val stale = staleDerivedRoots(
            redundant = listOf(root),
            romsPaths = listOf(root.treeUri),
            systemTrees = mapOf("n3ds" to "/storage/3A0D-5000/Ayn Thor/n3ds"),
        )
        assertEquals(listOf(root), stale)
    }

    @Test
    fun `root the user added that no system pick explains is kept`() {
        assertTrue(
            staleDerivedRoots(
                redundant = listOf(root),
                romsPaths = listOf(root.treeUri),
                systemTrees = mapOf("n3ds" to "/storage/3A0D-5000/Ayn Thor/roms/other-n3ds"),
            ).isEmpty(),
        )
    }

    @Test
    fun `source that is not a saved roms path is kept`() {
        assertTrue(
            staleDerivedRoots(
                redundant = listOf(root),
                romsPaths = emptyList(),
                systemTrees = mapOf("n3ds" to "/storage/3A0D-5000/Ayn Thor/n3ds"),
            ).isEmpty(),
        )
    }
}
