package jr.brian.home.canvas.viewmodel

import android.graphics.drawable.Drawable
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jr.brian.home.canvas.data.CanvasLayoutManager
import jr.brian.home.canvas.data.CanvasTabType
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect
import jr.brian.home.canvas.model.ResolvedCanvasItem
import jr.brian.home.data.FolderManager
import jr.brian.home.data.PinnedRomManager
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import jr.brian.home.model.rom.PinnedRomInfo
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

@OptIn(ExperimentalCoroutinesApi::class)
class CanvasViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var canvasLayoutManager: CanvasLayoutManager
    private lateinit var folderManager: FolderManager
    private lateinit var pinnedRomManager: PinnedRomManager

    private lateinit var layoutsByPage: MutableStateFlow<Map<Int, CanvasLayout>>
    private lateinit var foldersForPage: MutableStateFlow<List<Folder>>
    private lateinit var romsForPage: MutableStateFlow<List<PinnedRomInfo>>

    private val mockDrawable: Drawable = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        layoutsByPage = MutableStateFlow(emptyMap())
        foldersForPage = MutableStateFlow(emptyList())
        romsForPage = MutableStateFlow(emptyList())

        canvasLayoutManager = mockk(relaxed = true)
        folderManager = mockk(relaxed = true)
        pinnedRomManager = mockk(relaxed = true)

        every { canvasLayoutManager.layoutsByPage } returns layoutsByPage
        every { folderManager.getFolders(any(), CanvasTabType.VALUE) } returns foldersForPage
        every { pinnedRomManager.getPinnedRoms(any(), CanvasTabType.VALUE) } returns romsForPage
        every { pinnedRomManager.allPinnedRoms } returns MutableStateFlow(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newVm(): CanvasViewModel =
        CanvasViewModel(canvasLayoutManager, folderManager, pinnedRomManager)

    private fun appInfo(pkg: String, label: String = pkg) =
        AppInfo(label = label, packageName = pkg, icon = mockDrawable)

    private fun layoutOf(vararg items: CanvasItem): CanvasLayout {
        val placement = mapOf(*items.mapIndexed { i, item -> item.id to GridRect(0, i, 1, 1) }.toTypedArray())
        return CanvasLayout(
            items = items.toList(),
            verticalArrangement = placement,
            horizontalArrangement = placement
        )
    }

    @Test
    fun `initial state is unbound with empty defaults`() = runTest {
        val vm = newVm()
        advanceUntilIdle()

        vm.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(-1, state.pageIndex)
            assertTrue(state.resolvedItems.isEmpty())
            assertEquals(CanvasScrollOrientation.HORIZONTAL, state.layout.activeOrientation)
        }
    }

    @Test
    fun `setPageIndex switches to that page's layout`() = runTest {
        val layout = layoutOf(CanvasItem.AppItem("a", packageName = "com.x"))
            .copy(activeOrientation = CanvasScrollOrientation.HORIZONTAL)
        layoutsByPage.value = mapOf(2 to layout)

        val vm = newVm()
        vm.setPageIndex(2)
        advanceUntilIdle()

        vm.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(2, state.pageIndex)
            assertEquals(CanvasScrollOrientation.HORIZONTAL, state.layout.activeOrientation)
            assertEquals(1, state.resolvedItems.size)
        }
    }

    @Test
    fun `app items resolve to AppInfo when package is present`() = runTest {
        layoutsByPage.value = mapOf(
            0 to layoutOf(CanvasItem.AppItem("a", packageName = "com.x"))
        )

        val vm = newVm()
        vm.setPageIndex(0)
        vm.setApps(listOf(appInfo("com.x", label = "X App")))
        advanceUntilIdle()

        vm.uiState.test {
            val resolved = expectMostRecentItem().resolvedItems.single()
            assertTrue(resolved is ResolvedCanvasItem.App)
            assertEquals("X App", (resolved as ResolvedCanvasItem.App).info?.label)
        }
    }

    @Test
    fun `app items resolve to null info when package is missing`() = runTest {
        layoutsByPage.value = mapOf(
            0 to layoutOf(CanvasItem.AppItem("a", packageName = "com.gone"))
        )

        val vm = newVm()
        vm.setPageIndex(0)
        vm.setApps(emptyList())
        advanceUntilIdle()

        vm.uiState.test {
            val resolved = expectMostRecentItem().resolvedItems.single()
            assertTrue(resolved is ResolvedCanvasItem.App)
            assertNull((resolved as ResolvedCanvasItem.App).info)
        }
    }

    @Test
    fun `folder items resolve via FolderManager canvas tab type`() = runTest {
        val folder = Folder(
            id = "fld-1",
            name = "Games",
            appPackageNames = listOf("com.x"),
            position = AppPosition(packageName = "fld-1", x = 0f, y = 0f)
        )
        foldersForPage.value = listOf(folder)
        layoutsByPage.value = mapOf(
            0 to layoutOf(CanvasItem.FolderItem("fi", folderId = "fld-1"))
        )

        val vm = newVm()
        vm.setPageIndex(0)
        advanceUntilIdle()

        vm.uiState.test {
            val resolved = expectMostRecentItem().resolvedItems.single()
            assertTrue(resolved is ResolvedCanvasItem.Folder)
            assertEquals("Games", (resolved as ResolvedCanvasItem.Folder).folder?.name)
        }
        verify { folderManager.getFolders(0, CanvasTabType.VALUE) }
    }

    @Test
    fun `rom items resolve via PinnedRomManager canvas tab type`() = runTest {
        val rom = PinnedRomInfo(
            key = "snes/mario",
            name = "Mario",
            systemName = "snes",
            path = "/roms/mario.smc"
        )
        romsForPage.value = listOf(rom)
        layoutsByPage.value = mapOf(
            0 to layoutOf(CanvasItem.RomItem("r1", romKey = "snes/mario"))
        )

        val vm = newVm()
        vm.setPageIndex(0)
        advanceUntilIdle()

        vm.uiState.test {
            val resolved = expectMostRecentItem().resolvedItems.single()
            assertTrue(resolved is ResolvedCanvasItem.Rom)
            assertEquals("Mario", (resolved as ResolvedCanvasItem.Rom).info?.name)
        }
        verify { pinnedRomManager.getPinnedRoms(0, CanvasTabType.VALUE) }
    }

    @Test
    fun `widget and rss launcher items resolve to passthrough variants`() = runTest {
        layoutsByPage.value = mapOf(
            0 to layoutOf(
                CanvasItem.WidgetItem("w", widgetId = 42),
                CanvasItem.RssLauncherItem("rss")
            )
        )

        val vm = newVm()
        vm.setPageIndex(0)
        advanceUntilIdle()

        vm.uiState.test {
            val items = expectMostRecentItem().resolvedItems
            assertEquals(2, items.size)
            assertNotNull(items.firstOrNull { it is ResolvedCanvasItem.Widget })
            assertNotNull(items.firstOrNull { it is ResolvedCanvasItem.RssLauncher })
        }
    }

    @Test
    fun `intents delegate to CanvasLayoutManager with bound page index`() = runTest {
        val vm = newVm()
        vm.setPageIndex(5)
        advanceUntilIdle()

        val item = CanvasItem.AppItem("a", packageName = "com.x")
        vm.addItem(item)
        vm.moveItem("a", col = 1, row = 2)
        vm.resizeItem("a", colSpan = 3, rowSpan = 4)
        vm.removeItem("a")
        vm.setOrientation(CanvasScrollOrientation.HORIZONTAL)
        vm.setGrid(columns = 6, rows = 8)

        verify { canvasLayoutManager.addItem(5, item) }
        verify { canvasLayoutManager.moveItem(5, "a", 1, 2) }
        verify { canvasLayoutManager.resizeItem(5, "a", 3, 4) }
        verify { canvasLayoutManager.removeItem(5, "a") }
        verify { canvasLayoutManager.setOrientation(5, CanvasScrollOrientation.HORIZONTAL) }
        verify { canvasLayoutManager.setGrid(5, 6, 8) }
    }

    @Test
    fun `intents are no-ops when no page is bound`() = runTest {
        val vm = newVm()
        advanceUntilIdle()

        vm.addItem(CanvasItem.AppItem("a", packageName = "com.x"))
        vm.moveItem("a", 1, 2)
        vm.removeItem("a")

        verify(exactly = 0) { canvasLayoutManager.addItem(any(), any()) }
        verify(exactly = 0) { canvasLayoutManager.moveItem(any(), any(), any(), any()) }
        verify(exactly = 0) { canvasLayoutManager.removeItem(any(), any()) }
    }

    @Test
    fun `reactive folder sync auto-adds FolderItems for new canvas-owned folders`() = runTest {
        every { canvasLayoutManager.getLayout(0) } returns CanvasLayout()

        val vm = newVm()
        vm.setPageIndex(0)
        advanceUntilIdle()

        foldersForPage.value = listOf(
            Folder(
                id = "fld-new",
                name = "New",
                appPackageNames = listOf("com.x"),
                position = AppPosition(packageName = "fld-new", x = 0f, y = 0f)
            )
        )
        advanceUntilIdle()

        verify {
            canvasLayoutManager.addItem(
                0,
                match { it is CanvasItem.FolderItem && it.folderId == "fld-new" }
            )
        }
    }

    @Test
    fun `reactive folder sync skips folders already referenced by layout`() = runTest {
        every { canvasLayoutManager.getLayout(0) } returns layoutOf(
            CanvasItem.FolderItem("fi", folderId = "fld-existing")
        )

        val vm = newVm()
        vm.setPageIndex(0)
        advanceUntilIdle()

        foldersForPage.value = listOf(
            Folder(
                id = "fld-existing",
                name = "Old",
                appPackageNames = emptyList(),
                position = AppPosition(packageName = "fld-existing", x = 0f, y = 0f)
            )
        )
        advanceUntilIdle()

        verify(exactly = 0) {
            canvasLayoutManager.addItem(any(), any())
        }
    }

    @Test
    fun `removeItemAndCleanup also deletes folder entity for FolderItems`() = runTest {
        val vm = newVm()
        vm.setPageIndex(2)
        advanceUntilIdle()

        val folderItem = CanvasItem.FolderItem(id = "folder-x", folderId = "fld-x")
        vm.removeItemAndCleanup(folderItem)
        advanceUntilIdle()

        coVerify { folderManager.deleteFolder(2, "fld-x", CanvasTabType.VALUE) }
        verify { canvasLayoutManager.removeItem(2, "folder-x") }
    }

    @Test
    fun `pinRomToCanvas pins ROM under canvas tab type and adds RomItem`() = runTest {
        every { canvasLayoutManager.getLayout(3) } returns CanvasLayout()
        val rom = PinnedRomInfo(
            key = "snes/mario",
            name = "Mario",
            systemName = "snes",
            path = "/roms/mario.smc"
        )

        val vm = newVm()
        vm.setPageIndex(3)
        advanceUntilIdle()

        vm.pinRomToCanvas(rom)

        verify { pinnedRomManager.addPinnedRom(3, rom, CanvasTabType.VALUE) }
        verify {
            canvasLayoutManager.addItem(
                eq(3),
                match { it is CanvasItem.RomItem && it.romKey == "snes/mario" }
            )
        }
    }

    @Test
    fun `pinRomToCanvas skips adding RomItem when already on canvas`() = runTest {
        every { canvasLayoutManager.getLayout(0) } returns layoutOf(
            CanvasItem.RomItem("ri", romKey = "snes/mario")
        )
        val rom = PinnedRomInfo(
            key = "snes/mario",
            name = "Mario",
            systemName = "snes",
            path = "/roms/mario.smc"
        )

        val vm = newVm()
        vm.setPageIndex(0)
        advanceUntilIdle()

        vm.pinRomToCanvas(rom)

        verify { pinnedRomManager.addPinnedRom(0, rom, CanvasTabType.VALUE) }
        verify(exactly = 0) {
            canvasLayoutManager.addItem(
                eq(0),
                match { it is CanvasItem.RomItem }
            )
        }
    }

    @Test
    fun `layout updates after bind are reflected in uiState`() = runTest {
        val vm = newVm()
        vm.setPageIndex(0)
        advanceUntilIdle()

        layoutsByPage.value = mapOf(
            0 to layoutOf(CanvasItem.RssLauncherItem("rss")).copy(
                verticalColumns = 3,
                horizontalRows = 5
            )
        )
        advanceUntilIdle()

        vm.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(3, state.layout.verticalColumns)
            assertEquals(5, state.layout.horizontalRows)
            assertEquals(1, state.resolvedItems.size)
        }
    }
}
