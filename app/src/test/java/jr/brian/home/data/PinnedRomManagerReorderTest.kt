package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import jr.brian.home.canvas.data.CanvasTabType
import jr.brian.home.model.rom.PinnedRomInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PinnedRomManagerReorderTest {
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var manager: PinnedRomManager
    private val store = mutableMapOf<String, Any?>()

    private fun rom(key: String) = PinnedRomInfo(
        key = key,
        name = key,
        systemName = "snes",
        path = "/roms/$key.smc"
    )

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        prefs = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences("pinned_roms_prefs", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.apply() } answers { }
        every { editor.putString(any(), any()) } answers {
            store[firstArg()] = secondArg(); editor
        }
        every { editor.remove(any()) } answers {
            store.remove(firstArg<String>()); editor
        }
        every { prefs.getString(any(), any()) } answers {
            store[firstArg()] as? String ?: secondArg()
        }
        every { prefs.all } answers { store.toMap() }

        manager = PinnedRomManager(context)
    }

    @Test
    fun `reorderPages preserves tab type isolation`() = runTest {
        manager.addPinnedRom(0, rom("apps_p0"), PinnedRomManager.TAB_TYPE_APPS)
        manager.addPinnedRom(1, rom("apps_p1"), PinnedRomManager.TAB_TYPE_APPS)
        manager.addPinnedRom(0, rom("canvas_p0"), CanvasTabType.VALUE)
        manager.addPinnedRom(1, rom("canvas_p1"), CanvasTabType.VALUE)

        // Reorder ONLY the apps tab type
        manager.reorderPages(mapOf(0 to 1, 1 to 0), PinnedRomManager.TAB_TYPE_APPS)

        assertEquals(
            "apps_p1",
            manager.getPinnedRoms(0, PinnedRomManager.TAB_TYPE_APPS).first().single().key
        )
        assertEquals(
            "apps_p0",
            manager.getPinnedRoms(1, PinnedRomManager.TAB_TYPE_APPS).first().single().key
        )
        // Canvas namespace untouched
        assertEquals(
            "canvas_p0",
            manager.getPinnedRoms(0, CanvasTabType.VALUE).first().single().key
        )
        assertEquals(
            "canvas_p1",
            manager.getPinnedRoms(1, CanvasTabType.VALUE).first().single().key
        )
    }

    @Test
    fun `removePage shifts canvas tab type independently of apps`() = runTest {
        manager.addPinnedRom(0, rom("apps_p0"), PinnedRomManager.TAB_TYPE_APPS)
        manager.addPinnedRom(2, rom("apps_p2"), PinnedRomManager.TAB_TYPE_APPS)
        manager.addPinnedRom(0, rom("canvas_p0"), CanvasTabType.VALUE)
        manager.addPinnedRom(2, rom("canvas_p2"), CanvasTabType.VALUE)

        manager.removePage(0, CanvasTabType.VALUE)

        // Canvas: p0 dropped, p2 -> p1
        assertTrue(manager.getPinnedRoms(0, CanvasTabType.VALUE).first().isEmpty())
        assertEquals(
            "canvas_p2",
            manager.getPinnedRoms(1, CanvasTabType.VALUE).first().single().key
        )
        // Apps namespace untouched
        assertEquals(
            "apps_p0",
            manager.getPinnedRoms(0, PinnedRomManager.TAB_TYPE_APPS).first().single().key
        )
        assertEquals(
            "apps_p2",
            manager.getPinnedRoms(2, PinnedRomManager.TAB_TYPE_APPS).first().single().key
        )
    }
}
