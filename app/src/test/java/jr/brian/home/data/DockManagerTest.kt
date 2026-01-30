package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DockManagerTest {
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var dockManager: DockManager

    private val prefsStorage = mutableMapOf<String, Any>()

    @Before
    fun setup() {
        // Clear storage before each test
        prefsStorage.clear()

        // Mock Context
        context = mockk(relaxed = true)

        // Mock SharedPreferences with storage
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        // Setup SharedPreferences behavior
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putInt(any(), any()) } answers {
            prefsStorage[firstArg()] = secondArg<Int>()
            editor
        }
        every { editor.putBoolean(any(), any()) } answers {
            prefsStorage[firstArg()] = secondArg<Boolean>()
            editor
        }
        every { editor.putString(any(), any()) } answers {
            val value = secondArg<String?>()
            if (value != null) {
                prefsStorage[firstArg()] = value
            }
            editor
        }
        every { editor.remove(any()) } answers {
            prefsStorage.remove(firstArg<String>())
            editor
        }
        every { editor.apply() } returns Unit
        every { sharedPreferences.getInt(any(), any()) } answers {
            prefsStorage[firstArg()] as? Int ?: secondArg()
        }
        every { sharedPreferences.getBoolean(any(), any()) } answers {
            prefsStorage[firstArg()] as? Boolean ?: secondArg()
        }
        every { sharedPreferences.getString(any(), any()) } answers {
            prefsStorage[firstArg()] as? String ?: secondArg()
        }
    }

    @Test
    fun `initial state loads empty dock when no prefs exist`() = runTest {
        // When
        dockManager = DockManager(context)

        // Then
        assertEquals(emptyList<String>(), dockManager.dockApps.first())
    }

    @Test
    fun `initial state loads saved dock apps from preferences`() = runTest {
        // Given
        prefsStorage["dock_slot_count"] = 3
        prefsStorage["dock_app_0"] = "com.example.app1"
        prefsStorage["dock_app_1"] = "com.example.app2"
        prefsStorage["dock_app_2"] = "com.example.app3"

        // When
        dockManager = DockManager(context)

        // Then
        val dockApps = dockManager.dockApps.first()
        assertEquals(3, dockApps.size)
        assertEquals("com.example.app1", dockApps[0])
        assertEquals("com.example.app2", dockApps[1])
        assertEquals("com.example.app3", dockApps[2])
    }

    @Test
    fun `initial state loads dock color from preferences`() = runTest {
        // Given
        val testColor = Color.Blue
        prefsStorage["dock_color"] = testColor.toArgb()

        // When
        dockManager = DockManager(context)

        // Then
        assertEquals(testColor, dockManager.dockColor.first())
    }

    @Test
    fun `initial state loads dock size from preferences`() = runTest {
        // Given
        prefsStorage["dock_size"] = DockSize.STANDARD.ordinal

        // When
        dockManager = DockManager(context)

        // Then
        assertEquals(DockSize.STANDARD, dockManager.dockSize.first())
    }

    @Test
    fun `initial state loads dock visibility from preferences`() = runTest {
        // Given
        prefsStorage["dock_visible"] = false

        // When
        dockManager = DockManager(context)

        // Then
        assertFalse(dockManager.isDockVisible.first())
    }

    @Test
    fun `initial state defaults dock visibility to true when not set`() = runTest {
        // When
        dockManager = DockManager(context)

        // Then
        assertTrue(dockManager.isDockVisible.first())
    }

    @Test
    fun `initial state loads dock visible pages from preferences`() = runTest {
        // Given
        prefsStorage["dock_visible_pages"] = "0,1,2"

        // When
        dockManager = DockManager(context)

        // Then
        val pages = dockManager.dockVisiblePages.first()
        assertEquals(setOf(0, 1, 2), pages)
    }

    @Test
    fun `initial state loads max dock apps from preferences`() = runTest {
        // Given
        prefsStorage["max_dock_apps"] = 4

        // When
        dockManager = DockManager(context)

        // Then
        assertEquals(4, dockManager.maxDockApps.first())
    }

    @Test
    fun `addAppToDock adds app to empty dock at position`() = runTest {
        // Given
        dockManager = DockManager(context)

        // When
        dockManager.addAppToDock("com.example.app", 0)

        // Then
        val dockApps = dockManager.dockApps.first()
        assertEquals(1, dockApps.size)
        assertEquals("com.example.app", dockApps[0])
    }

    @Test
    fun `addAppToDock adds app at specific position expanding list`() = runTest {
        // Given
        dockManager = DockManager(context)

        // When
        dockManager.addAppToDock("com.example.app", 2)

        // Then
        val dockApps = dockManager.dockApps.first()
        assertEquals(3, dockApps.size)
        assertEquals("", dockApps[0])
        assertEquals("", dockApps[1])
        assertEquals("com.example.app", dockApps[2])
    }

    @Test
    fun `addAppToDock does not add duplicate app`() = runTest {
        // Given
        dockManager = DockManager(context)
        dockManager.addAppToDock("com.example.app", 0)

        // When
        dockManager.addAppToDock("com.example.app", 1)

        // Then
        val dockApps = dockManager.dockApps.first()
        assertEquals(1, dockApps.size)
        assertEquals("com.example.app", dockApps[0])
    }

    @Test
    fun `addAppToDock rejects position beyond max dock apps`() = runTest {
        // Given
        dockManager = DockManager(context)

        // When
        dockManager.addAppToDock("com.example.app", DockManager.MAX_DOCK_APPS)

        // Then
        assertEquals(emptyList<String>(), dockManager.dockApps.first())
    }

    @Test
    fun `addAppToDock rejects when dock is full`() = runTest {
        // Given
        dockManager = DockManager(context)
        repeat(DockManager.MAX_DOCK_APPS) { i ->
            dockManager.addAppToDock("com.example.app$i", i)
        }

        // When
        dockManager.addAppToDock("com.example.extra", 0)

        // Then
        val dockApps = dockManager.dockApps.first()
        assertEquals(DockManager.MAX_DOCK_APPS, dockApps.count { it.isNotEmpty() })
        assertFalse(dockApps.contains("com.example.extra"))
    }

    @Test
    fun `addAppToDock persists to SharedPreferences`() = runTest {
        // Given
        dockManager = DockManager(context)

        // When
        dockManager.addAppToDock("com.example.app", 0)

        // Then
        verify { editor.putInt("dock_slot_count", 1) }
        verify { editor.putString("dock_app_0", "com.example.app") }
        verify { editor.apply() }
    }

    @Test
    fun `removeAppFromDock removes app from dock`() = runTest {
        // Given
        dockManager = DockManager(context)
        dockManager.addAppToDock("com.example.app", 0)

        // When
        dockManager.removeAppFromDock("com.example.app")

        // Then
        val dockApps = dockManager.dockApps.first()
        assertEquals(1, dockApps.size)
        assertEquals("", dockApps[0])
    }

    @Test
    fun `removeAppFromDock does nothing if app not in dock`() = runTest {
        // Given
        dockManager = DockManager(context)
        dockManager.addAppToDock("com.example.app1", 0)

        // When
        dockManager.removeAppFromDock("com.example.app2")

        // Then
        val dockApps = dockManager.dockApps.first()
        assertEquals(1, dockApps.size)
        assertEquals("com.example.app1", dockApps[0])
    }

    @Test
    fun `addEmptySlot adds empty slot at position`() = runTest {
        // Given
        dockManager = DockManager(context)

        // When
        dockManager.addEmptySlot(1)

        // Then
        val dockApps = dockManager.dockApps.first()
        assertEquals(2, dockApps.size)
        assertEquals("", dockApps[0])
        assertEquals("", dockApps[1])
    }

    @Test
    fun `removeEmptySlot removes empty slot at position`() = runTest {
        // Given
        dockManager = DockManager(context)
        dockManager.addEmptySlot(0)
        dockManager.addEmptySlot(1)

        // When
        dockManager.removeEmptySlot(1)

        // Then
        val dockApps = dockManager.dockApps.first()
        assertEquals(1, dockApps.size)
    }

    @Test
    fun `removeEmptySlot does nothing if slot has app`() = runTest {
        // Given
        dockManager = DockManager(context)
        dockManager.addAppToDock("com.example.app", 0)

        // When
        dockManager.removeEmptySlot(0)

        // Then
        val dockApps = dockManager.dockApps.first()
        assertEquals(1, dockApps.size)
        assertEquals("com.example.app", dockApps[0])
    }

    @Test
    fun `removeEmptySlot clears preferences when dock becomes empty`() = runTest {
        // Given
        dockManager = DockManager(context)
        dockManager.addEmptySlot(0)

        // When
        dockManager.removeEmptySlot(0)

        // Then
        verify { editor.remove("dock_slot_count") }
        verify { editor.apply() }
    }

    @Test
    fun `isAppInDock returns true when app is in dock`() = runTest {
        // Given
        dockManager = DockManager(context)
        dockManager.addAppToDock("com.example.app", 0)

        // When/Then
        assertTrue(dockManager.isAppInDock("com.example.app"))
    }

    @Test
    fun `isAppInDock returns false when app is not in dock`() = runTest {
        // Given
        dockManager = DockManager(context)

        // When/Then
        assertFalse(dockManager.isAppInDock("com.example.app"))
    }

    @Test
    fun `setDockColor updates dock color`() = runTest {
        // Given
        dockManager = DockManager(context)
        val newColor = Color.Red

        // When
        dockManager.setDockColor(newColor)

        // Then
        assertEquals(newColor, dockManager.dockColor.first())
        verify { editor.putInt("dock_color", newColor.toArgb()) }
        verify { editor.apply() }
    }

    @Test
    fun `setDockSize updates dock size`() = runTest {
        // Given
        dockManager = DockManager(context)

        // When
        dockManager.setDockSize(DockSize.MINI)

        // Then
        assertEquals(DockSize.MINI, dockManager.dockSize.first())
        verify { editor.putInt("dock_size", DockSize.MINI.ordinal) }
        verify { editor.apply() }
    }

    @Test
    fun `setDockVisibility updates visibility`() = runTest {
        // Given
        dockManager = DockManager(context)

        // When
        dockManager.setDockVisibility(false)

        // Then
        assertFalse(dockManager.isDockVisible.first())
        verify { editor.putBoolean("dock_visible", false) }
        verify { editor.apply() }
    }

    @Test
    fun `setDockVisiblePages updates visible pages`() = runTest {
        // Given
        dockManager = DockManager(context)
        val pages = setOf(0, 2, 4)

        // When
        dockManager.setDockVisiblePages(pages)

        // Then
        assertEquals(pages, dockManager.dockVisiblePages.first())
        verify { editor.putString("dock_visible_pages", "0,2,4") }
        verify { editor.apply() }
    }

    @Test
    fun `setMaxDockApps updates max dock apps`() = runTest {
        // Given
        dockManager = DockManager(context)

        // When
        dockManager.setMaxDockApps(4)

        // Then
        assertEquals(4, dockManager.maxDockApps.first())
        verify { editor.putInt("max_dock_apps", 4) }
        verify { editor.apply() }
    }

    @Test
    fun `setMaxDockApps rejects value below minimum`() = runTest {
        // Given
        dockManager = DockManager(context)
        val originalMax = dockManager.maxDockApps.first()

        // When
        dockManager.setMaxDockApps(1)

        // Then
        assertEquals(originalMax, dockManager.maxDockApps.first())
    }

    @Test
    fun `setMaxDockApps rejects value above maximum`() = runTest {
        // Given
        dockManager = DockManager(context)
        val originalMax = dockManager.maxDockApps.first()

        // When
        dockManager.setMaxDockApps(DockManager.MAX_DOCK_APPS + 1)

        // Then
        assertEquals(originalMax, dockManager.maxDockApps.first())
    }

    @Test
    fun `togglePageVisibility adds page when empty set`() = runTest {
        // Given
        dockManager = DockManager(context)

        // When
        dockManager.togglePageVisibility(1, 3)

        // Then
        val pages = dockManager.dockVisiblePages.first()
        assertEquals(setOf(0, 2), pages) // All pages added except toggled one
    }

    @Test
    fun `togglePageVisibility removes page when in set`() = runTest {
        // Given
        dockManager = DockManager(context)
        dockManager.setDockVisiblePages(setOf(0, 1, 2))

        // When
        dockManager.togglePageVisibility(1, 3)

        // Then
        val pages = dockManager.dockVisiblePages.first()
        assertEquals(setOf(0, 2), pages)
    }

    @Test
    fun `togglePageVisibility adds page when not in set`() = runTest {
        // Given
        dockManager = DockManager(context)
        dockManager.setDockVisiblePages(setOf(0, 2))

        // When
        dockManager.togglePageVisibility(1, 3)

        // Then
        val pages = dockManager.dockVisiblePages.first()
        assertEquals(setOf(0, 1, 2), pages)
    }

    @Test
    fun `togglePageVisibility disables dock when all pages removed`() = runTest {
        // Given
        dockManager = DockManager(context)
        dockManager.setDockVisiblePages(setOf(0))

        // When
        dockManager.togglePageVisibility(0, 1)

        // Then
        assertFalse(dockManager.isDockVisible.first())
        assertEquals(emptySet<Int>(), dockManager.dockVisiblePages.first())
    }

    @Test
    fun `isDockVisibleOnPage returns true for empty set`() = runTest {
        // Given
        dockManager = DockManager(context)

        // When/Then
        assertTrue(dockManager.isDockVisibleOnPage(0))
        assertTrue(dockManager.isDockVisibleOnPage(1))
        assertTrue(dockManager.isDockVisibleOnPage(2))
    }

    @Test
    fun `isDockVisibleOnPage returns true when page in set`() = runTest {
        // Given
        dockManager = DockManager(context)
        dockManager.setDockVisiblePages(setOf(0, 2))

        // When/Then
        assertTrue(dockManager.isDockVisibleOnPage(0))
        assertTrue(dockManager.isDockVisibleOnPage(2))
    }

    @Test
    fun `isDockVisibleOnPage returns false when page not in set`() = runTest {
        // Given
        dockManager = DockManager(context)
        dockManager.setDockVisiblePages(setOf(0, 2))

        // When/Then
        assertFalse(dockManager.isDockVisibleOnPage(1))
        assertFalse(dockManager.isDockVisibleOnPage(3))
    }

    @Test
    fun `swapDockApps swaps apps at positions`() = runTest {
        // Given
        dockManager = DockManager(context)
        dockManager.addAppToDock("com.example.app1", 0)
        dockManager.addAppToDock("com.example.app2", 1)

        // When
        dockManager.swapDockApps(0, 1)

        // Then
        val dockApps = dockManager.dockApps.first()
        assertEquals("com.example.app2", dockApps[0])
        assertEquals("com.example.app1", dockApps[1])
    }

    @Test
    fun `swapDockApps does nothing when positions are same`() = runTest {
        // Given
        dockManager = DockManager(context)
        dockManager.addAppToDock("com.example.app", 0)
        val originalDockApps = dockManager.dockApps.first()

        // When
        dockManager.swapDockApps(0, 0)

        // Then
        assertEquals(originalDockApps, dockManager.dockApps.first())
    }

    @Test
    fun `swapDockApps does nothing when position out of bounds`() = runTest {
        // Given
        dockManager = DockManager(context)
        dockManager.addAppToDock("com.example.app", 0)
        val originalDockApps = dockManager.dockApps.first()

        // When
        dockManager.swapDockApps(0, 5)

        // Then
        assertEquals(originalDockApps, dockManager.dockApps.first())
    }

    @Test
    fun `swapDockApps can swap with empty slot`() = runTest {
        // Given
        dockManager = DockManager(context)
        dockManager.addAppToDock("com.example.app", 0)
        dockManager.addEmptySlot(1)

        // When
        dockManager.swapDockApps(0, 1)

        // Then
        val dockApps = dockManager.dockApps.first()
        assertEquals("", dockApps[0])
        assertEquals("com.example.app", dockApps[1])
    }

    @Test
    fun `dock handles mixed empty slots and apps`() = runTest {
        // Given
        dockManager = DockManager(context)
        dockManager.addAppToDock("com.example.app1", 0)
        dockManager.addEmptySlot(1)
        dockManager.addAppToDock("com.example.app2", 2)

        // When
        val dockApps = dockManager.dockApps.first()

        // Then
        assertEquals(3, dockApps.size)
        assertEquals("com.example.app1", dockApps[0])
        assertEquals("", dockApps[1])
        assertEquals("com.example.app2", dockApps[2])
    }

    @Test
    fun `SharedPreferences uses correct preference name`() {
        // When
        DockManager(context)

        // Then
        verify { context.getSharedPreferences("dock_prefs", Context.MODE_PRIVATE) }
    }

    @Test
    fun `constants are correctly defined`() {
        assertEquals(5, DockManager.MAX_DOCK_APPS)
    }

    @Test
    fun `dock settings persist across multiple manager instances`() = runTest {
        // Given
        dockManager = DockManager(context)
        dockManager.addAppToDock("com.example.app", 0)
        dockManager.setDockColor(Color.Blue)
        dockManager.setDockSize(DockSize.MINI)
        dockManager.setDockVisibility(false)

        // When - Create new manager instance
        val newManager = DockManager(context)

        // Then
        assertEquals(dockManager.dockApps.first(), newManager.dockApps.first())
        assertEquals(Color.Blue, newManager.dockColor.first())
        assertEquals(DockSize.MINI, newManager.dockSize.first())
        assertFalse(newManager.isDockVisible.first())
    }
}
