package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.model.rom.toPinnedRomInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifies the dedupe/cap/persistence contract for [RecentlyPlayedManager] —
 * the surface the Last Played widget binds to. Uses the mockk/SharedPreferences
 * pattern from the other data-layer tests (see [HomeButtonManagerTest]).
 */
class RecentlyPlayedManagerTest {
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val storage = mutableMapOf<String, Any?>()
    private var now = 1_000L

    @Before
    fun setup() {
        storage.clear()
        now = 1_000L

        context = mockk(relaxed = true)
        prefs = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } answers {
            storage[firstArg()] = secondArg<String>()
            editor
        }
        every { editor.apply() } returns Unit
        every { prefs.getString(any(), any()) } answers {
            storage[firstArg()] as? String ?: secondArg()
        }
    }

    private fun newManager() = RecentlyPlayedManager(context) { now }

    private fun game(name: String, system: String = "snes", path: String = "$name.smc"): GameInfo =
        GameInfo(path = path, name = name, systemName = system)

    @Test
    fun `starts empty when nothing has been persisted`() {
        val manager = newManager()
        assertTrue(manager.entries.value.isEmpty())
    }

    @Test
    fun `recording a game places it at the front with launchCount 1`() {
        val manager = newManager()
        manager.record(game("Mario"))

        val entries = manager.entries.value
        assertEquals(1, entries.size)
        assertEquals("Mario", entries[0].rom.name)
        assertEquals(1, entries[0].launchCount)
        assertEquals(now, entries[0].launchedAt)
    }

    @Test
    fun `recording multiple games keeps the newest first`() {
        val manager = newManager()
        manager.record(game("Mario"))
        now = 2_000L
        manager.record(game("Zelda"))
        now = 3_000L
        manager.record(game("Metroid"))

        val entries = manager.entries.value
        assertEquals(listOf("Metroid", "Zelda", "Mario"), entries.map { it.rom.name })
    }

    @Test
    fun `recording the same key twice dedupes and bumps launchCount`() {
        val manager = newManager()
        manager.record(game("Mario"))
        now = 2_000L
        manager.record(game("Zelda"))
        now = 3_000L
        manager.record(game("Mario"))

        val entries = manager.entries.value
        assertEquals(2, entries.size)
        assertEquals("Mario", entries[0].rom.name)
        assertEquals(2, entries[0].launchCount)
        assertEquals(3_000L, entries[0].launchedAt)
        assertEquals("Zelda", entries[1].rom.name)
    }

    @Test
    fun `recording beyond MAX_ENTRIES drops the oldest`() {
        val manager = newManager()
        repeat(RecentlyPlayedManager.MAX_ENTRIES + 5) { i ->
            now = 1_000L + i
            manager.record(game("game-$i", path = "p$i.smc"))
        }
        val entries = manager.entries.value
        assertEquals(RecentlyPlayedManager.MAX_ENTRIES, entries.size)
        assertEquals("game-${RecentlyPlayedManager.MAX_ENTRIES + 4}", entries.first().rom.name)
        assertFalse(entries.any { it.rom.name == "game-0" })
    }

    @Test
    fun `remove drops the entry with the matching key`() {
        val manager = newManager()
        manager.record(game("Mario"))
        manager.record(game("Zelda"))

        manager.remove("snes/Mario.smc")
        val entries = manager.entries.value
        assertEquals(1, entries.size)
        assertEquals("Zelda", entries[0].rom.name)
    }

    @Test
    fun `remove with no matching key is a no-op`() {
        val manager = newManager()
        manager.record(game("Mario"))
        val sizeBefore = manager.entries.value.size

        manager.remove("nes/Nothing.nes")
        assertEquals(sizeBefore, manager.entries.value.size)
    }

    @Test
    fun `clear empties the list and persists`() {
        val manager = newManager()
        manager.record(game("Mario"))
        manager.record(game("Zelda"))

        manager.clear()
        assertTrue(manager.entries.value.isEmpty())

        // A fresh manager observing the same storage sees the empty state.
        assertTrue(newManager().entries.value.isEmpty())
    }

    @Test
    fun `restore replaces the list wholesale and trims to the cap`() {
        val manager = newManager()
        val oversized = (0..30).map { i ->
            RecentlyPlayedEntry(
                rom = game("game-$i", path = "p$i.smc").toPinnedRomInfo(),
                launchedAt = 1_000L + i
            )
        }
        manager.restore(oversized)
        assertEquals(RecentlyPlayedManager.MAX_ENTRIES, manager.entries.value.size)
    }

    @Test
    fun `writes survive a round trip through storage`() {
        val first = newManager()
        first.record(game("Mario"))
        now = 2_000L
        first.record(game("Zelda"))

        val second = newManager()
        val entries = second.entries.value
        assertEquals(2, entries.size)
        assertEquals("Zelda", entries[0].rom.name)
        assertEquals("Mario", entries[1].rom.name)
    }

    @Test
    fun `relaunching the top entry keeps it at the top but bumps the count`() {
        val manager = newManager()
        manager.record(game("Mario"))
        now = 2_000L
        manager.record(game("Mario"))

        val entries = manager.entries.value
        assertEquals(1, entries.size)
        assertEquals(2, entries[0].launchCount)
        assertEquals(2_000L, entries[0].launchedAt)
    }
}
