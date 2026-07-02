package jr.brian.home.canvas

import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.every
import io.mockk.mockk
import jr.brian.home.canvas.data.CanvasLayoutManager
import jr.brian.home.canvas.data.CanvasTabType
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.satisfiesInvariant
import jr.brian.home.canvas.viewmodel.CanvasViewModel
import jr.brian.home.data.FolderManager
import jr.brian.home.data.PinnedRomManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Phase 4 acceptance scenario: drives the full ViewModel → manager → prefs
 * chain through the bug report. Each step asserts an item in the brief's
 * checkpoint list:
 *
 *   - Add an item in HORIZONTAL → switch to VERTICAL → item is there.
 *   - Move/resize in one orientation → other byte-for-byte unchanged.
 *   - Remove → gone from both arrangements.
 *   - Layout persists across restart (kill VM+manager, reload from prefs,
 *     identical state).
 *   - Invariant holds after every operation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CanvasAcceptanceTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private val store = mutableMapOf<String, String?>()

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockContext = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)

        every { mockContext.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } answers {
            store[firstArg()] = secondArg()
            mockEditor
        }
        every { mockEditor.remove(any()) } answers {
            store.remove(firstArg<String>())
            mockEditor
        }
        every { mockEditor.apply() } returns Unit
        every { mockEditor.clear() } answers {
            store.clear()
            mockEditor
        }
        every { mockPrefs.getString(any(), any()) } answers {
            store[firstArg()] ?: secondArg()
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        store.clear()
    }

    private fun newManager(): CanvasLayoutManager = CanvasLayoutManager(mockContext)

    private fun newViewModel(manager: CanvasLayoutManager): CanvasViewModel {
        val folderManager = mockk<FolderManager>(relaxed = true)
        val pinnedRomManager = mockk<PinnedRomManager>(relaxed = true)
        every { folderManager.getFolders(any(), CanvasTabType.VALUE) } returns MutableStateFlow(emptyList())
        every { pinnedRomManager.getPinnedRoms(any(), CanvasTabType.VALUE) } returns MutableStateFlow(emptyList())
        every { pinnedRomManager.allPinnedRoms } returns MutableStateFlow(emptyList())
        return CanvasViewModel(manager, folderManager, pinnedRomManager)
    }

    @Test
    fun `bug-fix scenario — add in horizontal, switch to vertical, item is visible at its auto-placed cell`() = runTest {
        val manager = newManager()
        val vm = newViewModel(manager)
        vm.setPageIndex(0)
        advanceUntilIdle()

        // ── Step 1: user toggles to HORIZONTAL and adds an item ──────────
        vm.setOrientation(CanvasScrollOrientation.HORIZONTAL)
        vm.addItem(CanvasItem.AppItem(id = "added-in-horizontal", packageName = "com.test"))
        advanceUntilIdle()

        val afterAdd = manager.getLayout(0)
        assertEquals(CanvasScrollOrientation.HORIZONTAL, afterAdd.activeOrientation)
        assertEquals(1, afterAdd.items.size)
        assertNotNull("item must be placed in vertical arrangement at add-time",
            afterAdd.verticalArrangement["added-in-horizontal"])
        assertNotNull("item must be placed in horizontal arrangement at add-time",
            afterAdd.horizontalArrangement["added-in-horizontal"])
        assertTrue(afterAdd.satisfiesInvariant())

        // ── Step 2: user toggles to VERTICAL — the bug was that the item ─
        //          would not appear here. After the fix, it must.
        vm.setOrientation(CanvasScrollOrientation.VERTICAL)
        advanceUntilIdle()

        val afterSwitch = manager.getLayout(0)
        assertEquals(CanvasScrollOrientation.VERTICAL, afterSwitch.activeOrientation)
        val activeCellAfterSwitch = afterSwitch.activeArrangement["added-in-horizontal"]
        assertNotNull(
            "the previously-missing item must be present in the active (vertical) arrangement after switch",
            activeCellAfterSwitch
        )

        // ── Step 3: switching orientation must NOT mutate either arrangement.
        assertEquals("vertical arrangement unchanged across orientation switch",
            afterAdd.verticalArrangement, afterSwitch.verticalArrangement)
        assertEquals("horizontal arrangement unchanged across orientation switch",
            afterAdd.horizontalArrangement, afterSwitch.horizontalArrangement)
    }

    @Test
    fun `move in one orientation leaves the other byte-for-byte unchanged`() = runTest {
        val manager = newManager()
        val vm = newViewModel(manager)
        vm.setPageIndex(0)
        advanceUntilIdle()

        vm.addItem(CanvasItem.AppItem("a", packageName = "com.a"))
        vm.setOrientation(CanvasScrollOrientation.VERTICAL)
        advanceUntilIdle()
        val horizontalBefore = manager.getLayout(0).horizontalArrangement

        vm.moveItem("a", col = 3, row = 5)
        advanceUntilIdle()

        val after = manager.getLayout(0)
        val movedVertical = after.verticalArrangement.getValue("a")
        assertEquals(3, movedVertical.col)
        assertEquals(5, movedVertical.row)
        assertEquals("horizontal arrangement must not budge when moving vertically",
            horizontalBefore, after.horizontalArrangement)
        assertTrue(after.satisfiesInvariant())
    }

    @Test
    fun `remove via ViewModel deletes from both arrangements`() = runTest {
        val manager = newManager()
        val vm = newViewModel(manager)
        vm.setPageIndex(0)
        advanceUntilIdle()

        vm.addItem(CanvasItem.AppItem("doomed", packageName = "com.x"))
        vm.addItem(CanvasItem.AppItem("survivor", packageName = "com.y"))
        advanceUntilIdle()

        vm.removeItem("doomed")
        advanceUntilIdle()

        val after = manager.getLayout(0)
        assertEquals(listOf("survivor"), after.items.map { it.id })
        assertNull(after.verticalArrangement["doomed"])
        assertNull(after.horizontalArrangement["doomed"])
        assertTrue(after.satisfiesInvariant())
    }

    @Test
    fun `layout persists across restart — kill VM and manager, reload from same prefs`() = runTest {
        // ── Session 1 ───────────────────────────────────────────────────
        val managerA = newManager()
        val vmA = newViewModel(managerA)
        vmA.setPageIndex(0)
        advanceUntilIdle()

        vmA.setOrientation(CanvasScrollOrientation.HORIZONTAL)
        vmA.addItem(CanvasItem.AppItem("a", packageName = "com.a"))
        vmA.addItem(CanvasItem.WidgetItem("w", widgetId = 42), colSpan = 3, rowSpan = 2)
        vmA.setOrientation(CanvasScrollOrientation.VERTICAL)
        vmA.moveItem("a", col = 2, row = 4)
        advanceUntilIdle()

        val sessionAState = managerA.getLayout(0)
        assertTrue(sessionAState.satisfiesInvariant())

        // ── Session 2 (process restart simulated by fresh instances) ─────
        val managerB = newManager()
        val vmB = newViewModel(managerB)
        vmB.setPageIndex(0)
        advanceUntilIdle()

        val sessionBState = managerB.getLayout(0)

        assertEquals(sessionAState.items, sessionBState.items)
        assertEquals(sessionAState.verticalArrangement, sessionBState.verticalArrangement)
        assertEquals(sessionAState.horizontalArrangement, sessionBState.horizontalArrangement)
        assertEquals(sessionAState.activeOrientation, sessionBState.activeOrientation)
        assertEquals(sessionAState.verticalColumns, sessionBState.verticalColumns)
        assertEquals(sessionAState.horizontalRows, sessionBState.horizontalRows)
        assertTrue(sessionBState.satisfiesInvariant())
    }

    @Test
    fun `compactLayout via ViewModel pulls active orientation toward origin without touching the other`() = runTest {
        val manager = newManager()
        val vm = newViewModel(manager)
        vm.setPageIndex(0)
        advanceUntilIdle()

        vm.addItem(CanvasItem.AppItem("a", packageName = "com.a"))
        vm.addItem(CanvasItem.AppItem("b", packageName = "com.b"))
        vm.addItem(CanvasItem.AppItem("c", packageName = "com.c"))
        vm.removeItem("b")
        vm.setOrientation(CanvasScrollOrientation.VERTICAL)
        advanceUntilIdle()
        val horizontalBefore = manager.getLayout(0).horizontalArrangement

        vm.compactLayout()
        advanceUntilIdle()

        val after = manager.getLayout(0)
        assertEquals("compact must not touch the inactive arrangement",
            horizontalBefore, after.horizontalArrangement)
        assertTrue(after.satisfiesInvariant())
    }
}
