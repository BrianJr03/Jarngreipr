package jr.brian.home.esde.data

import jr.brian.home.esde.model.GameInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Cache invariants:
 *  - what save wrote, load returns.
 *  - a system removed from the save-set is deleted from disk (an empty ROMs
 *    folder should not leave a stale cache).
 *  - invalidateAll wipes everything.
 *  - a matching stamp signals a valid cache reuse; any inequality invalidates.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RomIndexCacheTest {

    private fun newCache(): RomIndexCache {
        val app = RuntimeEnvironment.getApplication()
        val dir = java.io.File(app.filesDir, "rom_index")
        dir.deleteRecursively()
        return RomIndexCache(app)
    }

    @Test
    fun `save then load round-trips a system`() = runBlocking {
        val cache = newCache()
        val entry = SystemCacheEntry(
            stamp = SystemStamp(
                systemDirLastModified = 1L,
                entryCount = 2,
                gamelistLastModified = 5L,
                decorationEnabled = true,
                romsRootUsed = "/storage/emulated/0/Roms",
            ),
            games = listOf(
                GameInfo(path = "./Foo.iso", name = "Foo", systemName = "psx"),
                GameInfo(path = "./Bar.chd", name = "Bar", systemName = "psx"),
            ),
        )
        cache.saveAll(mapOf("psx" to entry))

        val loaded = cache.loadAll()
        assertEquals(setOf("psx"), loaded.keys)
        assertEquals(entry, loaded["psx"])
    }

    @Test
    fun `save deletes systems missing from the new save-set`() = runBlocking {
        val cache = newCache()
        val ps2 = fakeEntry("ps2")
        val psx = fakeEntry("psx")
        cache.saveAll(mapOf("ps2" to ps2, "psx" to psx))
        assertEquals(setOf("ps2", "psx"), cache.loadAll().keys)

        cache.saveAll(mapOf("psx" to psx))

        val after = cache.loadAll()
        assertNull("stale ps2 entry must be evicted", after["ps2"])
        assertEquals(psx, after["psx"])
    }

    @Test
    fun `invalidateAll removes every cached system`() = runBlocking {
        val cache = newCache()
        cache.saveAll(mapOf("ps2" to fakeEntry("ps2"), "psx" to fakeEntry("psx")))
        assertTrue(cache.loadAll().isNotEmpty())

        cache.invalidateAll()
        assertEquals(emptyMap<String, SystemCacheEntry>(), cache.loadAll())
    }

    @Test
    fun `stamp matches only when every field equals`() {
        val a = SystemStamp(1L, 2, 3L, true, "/x")
        val b = SystemStamp(1L, 2, 3L, true, "/x")
        val differingCount = SystemStamp(1L, 99, 3L, true, "/x")
        val differingRoot = SystemStamp(1L, 2, 3L, true, "/y")

        assertTrue(a.matches(b))
        assertTrue(!a.matches(differingCount))
        assertTrue(!a.matches(differingRoot))
    }

    private fun fakeEntry(systemName: String): SystemCacheEntry = SystemCacheEntry(
        stamp = SystemStamp(1L, 1, -1L, true, "/tmp"),
        games = listOf(GameInfo(path = "./one.iso", name = "One", systemName = systemName)),
    )
}
