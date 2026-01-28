package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jr.brian.home.model.app.AppPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppPositionManagerTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var appPositionManager: AppPositionManager

    private val testData = mutableMapOf<String, Any>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup mocks
        mockContext = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)

        // Mock SharedPreferences behavior
        every { mockContext.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit
        every { mockEditor.commit() } returns true

        // Mock data storage
        every { mockPrefs.getBoolean(any(), any()) } answers {
            testData[firstArg()] as? Boolean ?: secondArg()
        }
        every { mockPrefs.getString(any(), any()) } answers {
            testData[firstArg()] as? String ?: secondArg()
        }
        every { mockEditor.putBoolean(any(), any()) } answers {
            testData[firstArg()] = secondArg<Boolean>()
            mockEditor
        }
        every { mockEditor.putString(any(), any()) } answers {
            testData[firstArg()] = secondArg<String>()
            mockEditor
        }

        appPositionManager = AppPositionManager(mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testData.clear()
    }

    @Test
    fun `initial free mode state is false for all pages`() = runTest {
        // Then
        appPositionManager.isFreeModeByPage.test {
            val state = awaitItem()
            for (page in 0 until 10) {
                assertFalse("Page $page should have free mode disabled by default", 
                    state[page] ?: false)
            }
        }
    }

    @Test
    fun `setFreeMode enables free mode for specific page`() = runTest {
        // Given
        val pageIndex = 1

        // When
        appPositionManager.setFreeMode(pageIndex, true)

        // Then
        appPositionManager.isFreeModeByPage.test {
            val state = awaitItem()
            assertTrue("Free mode should be enabled for page $pageIndex", 
                state[pageIndex] ?: false)
        }
        verify { mockEditor.putBoolean("free_mode_$pageIndex", true) }
    }

    @Test
    fun `setFreeMode disables free mode for specific page`() = runTest {
        // Given
        val pageIndex = 2
        appPositionManager.setFreeMode(pageIndex, true)

        // When
        appPositionManager.setFreeMode(pageIndex, false)

        // Then
        appPositionManager.isFreeModeByPage.test {
            val state = awaitItem()
            assertFalse("Free mode should be disabled for page $pageIndex", 
                state[pageIndex] ?: true)
        }
        verify { mockEditor.putBoolean("free_mode_$pageIndex", false) }
    }

    @Test
    fun `setFreeMode persists state to SharedPreferences`() = runTest {
        // Given
        val pageIndex = 0

        // When
        appPositionManager.setFreeMode(pageIndex, true)

        // Then
        verify { mockPrefs.edit() }
        verify { mockEditor.putBoolean("free_mode_$pageIndex", true) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `free mode state is independent across pages`() = runTest {
        // When
        appPositionManager.setFreeMode(0, true)
        appPositionManager.setFreeMode(1, false)
        appPositionManager.setFreeMode(2, true)

        // Then
        appPositionManager.isFreeModeByPage.test {
            val state = awaitItem()
            assertTrue("Page 0 should have free mode enabled", state[0] ?: false)
            assertFalse("Page 1 should have free mode disabled", state[1] ?: true)
            assertTrue("Page 2 should have free mode enabled", state[2] ?: false)
        }
    }

    @Test
    fun `initial drag lock state is true for first 3 pages`() = runTest {
        // Then
        appPositionManager.isDragLockedByPage.test {
            val state = awaitItem()
            assertTrue("Page 0 should be drag locked by default", state[0] ?: false)
            assertTrue("Page 1 should be drag locked by default", state[1] ?: false)
            assertTrue("Page 2 should be drag locked by default", state[2] ?: false)
        }
    }

    @Test
    fun `setDragLock locks dragging for specific page`() = runTest {
        // Given
        val pageIndex = 1

        // When
        appPositionManager.setDragLock(pageIndex, true)

        // Then
        appPositionManager.isDragLockedByPage.test {
            val state = awaitItem()
            assertTrue("Drag should be locked for page $pageIndex", 
                state[pageIndex] ?: false)
        }
        verify { mockEditor.putBoolean("drag_locked_$pageIndex", true) }
    }

    @Test
    fun `setDragLock unlocks dragging for specific page`() = runTest {
        // Given
        val pageIndex = 0

        // When
        appPositionManager.setDragLock(pageIndex, false)

        // Then
        appPositionManager.isDragLockedByPage.test {
            val state = awaitItem()
            assertFalse("Drag should be unlocked for page $pageIndex", 
                state[pageIndex] ?: true)
        }
        verify { mockEditor.putBoolean("drag_locked_$pageIndex", false) }
    }

    @Test
    fun `setDragLock persists state to SharedPreferences`() = runTest {
        // Given
        val pageIndex = 2

        // When
        appPositionManager.setDragLock(pageIndex, false)

        // Then
        verify { mockPrefs.edit() }
        verify { mockEditor.putBoolean("drag_locked_$pageIndex", false) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `drag lock state is independent across pages`() = runTest {
        // When
        appPositionManager.setDragLock(0, true)
        appPositionManager.setDragLock(1, false)
        appPositionManager.setDragLock(2, true)

        // Then
        appPositionManager.isDragLockedByPage.test {
            val state = awaitItem()
            assertTrue("Page 0 should have drag locked", state[0] ?: false)
            assertFalse("Page 1 should not have drag locked", state[1] ?: true)
            assertTrue("Page 2 should have drag locked", state[2] ?: false)
        }
    }

    @Test
    fun `savePosition saves app position for specific page`() {
        // Given
        val pageIndex = 0
        val position = AppPosition("com.example.app", 100f, 200f, 64f)

        // When
        appPositionManager.savePosition(pageIndex, position)

        // Then
        val savedPosition = appPositionManager.getPosition(pageIndex, "com.example.app")
        assertEquals(position, savedPosition)
    }

    @Test
    fun `savePosition persists to SharedPreferences`() {
        // Given
        val pageIndex = 1
        val position = AppPosition("com.test.app", 150f, 250f, 72f)

        // When
        appPositionManager.savePosition(pageIndex, position)

        // Then
        verify { mockPrefs.edit() }
        verify { mockEditor.putString("positions_$pageIndex", any()) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `savePosition updates existing position`() {
        // Given
        val pageIndex = 0
        val packageName = "com.example.app"
        val initialPosition = AppPosition(packageName, 100f, 200f, 64f)
        val updatedPosition = AppPosition(packageName, 300f, 400f, 80f)

        // When
        appPositionManager.savePosition(pageIndex, initialPosition)
        appPositionManager.savePosition(pageIndex, updatedPosition)

        // Then
        val savedPosition = appPositionManager.getPosition(pageIndex, packageName)
        assertEquals(updatedPosition, savedPosition)
        assertEquals(300f, savedPosition?.x)
        assertEquals(400f, savedPosition?.y)
        assertEquals(80f, savedPosition?.iconSize)
    }

    @Test
    fun `getPosition returns null for non-existent app`() {
        // Given
        val pageIndex = 0

        // When
        val position = appPositionManager.getPosition(pageIndex, "com.nonexistent.app")

        // Then
        assertNull(position)
    }

    @Test
    fun `getPositions returns empty map for page with no positions`() {
        // Given
        val pageIndex = 5

        // When
        val positions = appPositionManager.getPositions(pageIndex)

        // Then
        assertTrue("Positions should be empty for new page", positions.isEmpty())
    }

    @Test
    fun `getPositions returns all positions for specific page`() {
        // Given
        val pageIndex = 2
        val position1 = AppPosition("com.app1", 100f, 200f, 64f)
        val position2 = AppPosition("com.app2", 300f, 400f, 72f)
        val position3 = AppPosition("com.app3", 500f, 600f, 80f)

        // When
        appPositionManager.savePosition(pageIndex, position1)
        appPositionManager.savePosition(pageIndex, position2)
        appPositionManager.savePosition(pageIndex, position3)

        // Then
        val positions = appPositionManager.getPositions(pageIndex)
        assertEquals(3, positions.size)
        assertTrue(positions.containsKey("com.app1"))
        assertTrue(positions.containsKey("com.app2"))
        assertTrue(positions.containsKey("com.app3"))
    }

    @Test
    fun `positions are isolated between pages`() {
        // Given
        val page0Position = AppPosition("com.app.page0", 100f, 200f, 64f)
        val page1Position = AppPosition("com.app.page1", 300f, 400f, 72f)

        // When
        appPositionManager.savePosition(0, page0Position)
        appPositionManager.savePosition(1, page1Position)

        // Then
        val page0Positions = appPositionManager.getPositions(0)
        val page1Positions = appPositionManager.getPositions(1)

        assertEquals(1, page0Positions.size)
        assertEquals(1, page1Positions.size)
        assertTrue(page0Positions.containsKey("com.app.page0"))
        assertFalse(page0Positions.containsKey("com.app.page1"))
        assertTrue(page1Positions.containsKey("com.app.page1"))
        assertFalse(page1Positions.containsKey("com.app.page0"))
    }

    @Test
    fun `removePosition removes app from specific page`() {
        // Given
        val pageIndex = 0
        val packageName = "com.example.app"
        val position = AppPosition(packageName, 100f, 200f, 64f)
        appPositionManager.savePosition(pageIndex, position)

        // When
        appPositionManager.removePosition(pageIndex, packageName)

        // Then
        val removedPosition = appPositionManager.getPosition(pageIndex, packageName)
        assertNull(removedPosition)
    }

    @Test
    fun `removePosition persists changes to SharedPreferences`() {
        // Given
        val pageIndex = 1
        val packageName = "com.test.app"
        val position = AppPosition(packageName, 150f, 250f, 72f)
        appPositionManager.savePosition(pageIndex, position)

        // When
        appPositionManager.removePosition(pageIndex, packageName)

        // Then
        verify(atLeast = 2) { mockEditor.putString("positions_$pageIndex", any()) }
        verify(atLeast = 2) { mockEditor.apply() }
    }

    @Test
    fun `clearAllPositions removes all positions from specific page`() {
        // Given
        val pageIndex = 0
        appPositionManager.savePosition(pageIndex, AppPosition("com.app1", 100f, 200f, 64f))
        appPositionManager.savePosition(pageIndex, AppPosition("com.app2", 300f, 400f, 72f))
        appPositionManager.savePosition(pageIndex, AppPosition("com.app3", 500f, 600f, 80f))

        // When
        appPositionManager.clearAllPositions(pageIndex)

        // Then
        val positions = appPositionManager.getPositions(pageIndex)
        assertTrue("All positions should be cleared", positions.isEmpty())
    }

    @Test
    fun `clearAllPositions only affects specified page`() {
        // Given
        val page0Position = AppPosition("com.app.page0", 100f, 200f, 64f)
        val page1Position = AppPosition("com.app.page1", 300f, 400f, 72f)
        appPositionManager.savePosition(0, page0Position)
        appPositionManager.savePosition(1, page1Position)

        // When
        appPositionManager.clearAllPositions(0)

        // Then
        val page0Positions = appPositionManager.getPositions(0)
        val page1Positions = appPositionManager.getPositions(1)
        assertTrue("Page 0 positions should be cleared", page0Positions.isEmpty())
        assertEquals(1, page1Positions.size)
    }

    @Test
    fun `clearAllPositions persists changes to SharedPreferences`() {
        // Given
        val pageIndex = 2
        appPositionManager.savePosition(pageIndex, AppPosition("com.app1", 100f, 200f, 64f))

        // When
        appPositionManager.clearAllPositions(pageIndex)

        // Then
        verify(atLeast = 2) { mockEditor.putString("positions_$pageIndex", any()) }
        verify(atLeast = 2) { mockEditor.apply() }
    }

    @Test
    fun `position data is saved in correct format`() {
        // Given
        val pageIndex = 0
        val position = AppPosition("com.example.app", 123.45f, 678.90f, 96f)

        // When
        appPositionManager.savePosition(pageIndex, position)

        // Then
        verify { 
            mockEditor.putString(
                "positions_$pageIndex", 
                match { it.contains("com.example.app,123.45,678.9,96.0") }
            ) 
        }
    }

    @Test
    fun `multiple positions are saved with correct separator`() {
        // Given
        val pageIndex = 1
        val position1 = AppPosition("com.app1", 100f, 200f, 64f)
        val position2 = AppPosition("com.app2", 300f, 400f, 72f)

        // When
        appPositionManager.savePosition(pageIndex, position1)
        appPositionManager.savePosition(pageIndex, position2)

        // Then
        verify(atLeast = 1) { 
            mockEditor.putString(
                "positions_$pageIndex", 
                match { it.contains("||") }
            ) 
        }
    }

    @Test
    fun `position with default icon size is saved correctly`() {
        // Given
        val pageIndex = 0
        val position = AppPosition("com.test.app", 50f, 75f)

        // When
        appPositionManager.savePosition(pageIndex, position)

        // Then
        val savedPosition = appPositionManager.getPosition(pageIndex, "com.test.app")
        assertEquals(64f, savedPosition?.iconSize)
    }

    @Test
    fun `saving position to negative page index is handled gracefully`() {
        // Given
        val position = AppPosition("com.example.app", 100f, 200f, 64f)

        // When
        appPositionManager.savePosition(-1, position)

        // Then - should not crash
        val savedPosition = appPositionManager.getPosition(-1, "com.example.app")
        assertEquals(position, savedPosition)
    }

    @Test
    fun `saving position with extreme coordinates is handled correctly`() {
        // Given
        val pageIndex = 0
        val position = AppPosition("com.extreme.app", Float.MAX_VALUE, Float.MIN_VALUE, 1000f)

        // When
        appPositionManager.savePosition(pageIndex, position)

        // Then
        val savedPosition = appPositionManager.getPosition(pageIndex, "com.extreme.app")
        assertEquals(Float.MAX_VALUE, savedPosition?.x)
        assertEquals(Float.MIN_VALUE, savedPosition?.y)
    }

    @Test
    fun `concurrent position updates on same page are handled correctly`() {
        // Given
        val pageIndex = 0
        val position1 = AppPosition("com.app1", 100f, 200f, 64f)
        val position2 = AppPosition("com.app2", 300f, 400f, 72f)

        // When
        appPositionManager.savePosition(pageIndex, position1)
        appPositionManager.savePosition(pageIndex, position2)

        // Then
        val positions = appPositionManager.getPositions(pageIndex)
        assertEquals(2, positions.size)
        assertEquals(position1, positions["com.app1"])
        assertEquals(position2, positions["com.app2"])
    }

    @Test
    fun `getPosition from empty page returns null`() {
        // When
        val position = appPositionManager.getPosition(0, "com.nonexistent.app")

        // Then
        assertNull(position)
    }

    @Test
    fun `removing non-existent position does not cause error`() {
        // When/Then - should not crash
        appPositionManager.removePosition(0, "com.nonexistent.app")
        
        // Verify it still attempts to save
        verify { mockEditor.apply() }
    }

    @Test
    fun `clearing positions on empty page does not cause error`() {
        // When/Then - should not crash
        appPositionManager.clearAllPositions(5)
        
        // Should not save anything for empty page
        val positions = appPositionManager.getPositions(5)
        assertTrue(positions.isEmpty())
    }

    @Test
    fun `full workflow - add, update, and remove positions`() {
        // Given
        val pageIndex = 0
        val packageName = "com.workflow.app"

        // When - Add position
        val initialPosition = AppPosition(packageName, 100f, 200f, 64f)
        appPositionManager.savePosition(pageIndex, initialPosition)

        // Then - Verify added
        var position = appPositionManager.getPosition(pageIndex, packageName)
        assertEquals(initialPosition, position)

        // When - Update position
        val updatedPosition = AppPosition(packageName, 300f, 400f, 80f)
        appPositionManager.savePosition(pageIndex, updatedPosition)

        // Then - Verify updated
        position = appPositionManager.getPosition(pageIndex, packageName)
        assertEquals(updatedPosition, position)

        // When - Remove position
        appPositionManager.removePosition(pageIndex, packageName)

        // Then - Verify removed
        position = appPositionManager.getPosition(pageIndex, packageName)
        assertNull(position)
    }

    @Test
    fun `combined free mode and drag lock states work independently`() = runTest {
        // Given
        val pageIndex = 1

        // When
        appPositionManager.setFreeMode(pageIndex, true)
        appPositionManager.setDragLock(pageIndex, false)

        // Then
        appPositionManager.isFreeModeByPage.test {
            val freeModeState = awaitItem()
            assertTrue("Free mode should be enabled", freeModeState[pageIndex] ?: false)
        }

        appPositionManager.isDragLockedByPage.test {
            val dragLockState = awaitItem()
            assertFalse("Drag lock should be disabled", dragLockState[pageIndex] ?: true)
        }
    }

    @Test
    fun `managing multiple pages simultaneously works correctly`() {
        // Given/When
        for (pageIndex in 0 until 5) {
            appPositionManager.setFreeMode(pageIndex, pageIndex % 2 == 0)
            appPositionManager.setDragLock(pageIndex, pageIndex % 2 == 1)
            
            val position = AppPosition(
                "com.app$pageIndex", 
                pageIndex * 100f, 
                pageIndex * 200f, 
                64f + pageIndex * 8f
            )
            appPositionManager.savePosition(pageIndex, position)
        }

        // Then - Verify all pages have correct data
        for (pageIndex in 0 until 5) {
            val position = appPositionManager.getPosition(pageIndex, "com.app$pageIndex")
            assertEquals(pageIndex * 100f, position?.x)
            assertEquals(pageIndex * 200f, position?.y)
            assertEquals(64f + pageIndex * 8f, position?.iconSize)
        }
    }
}
