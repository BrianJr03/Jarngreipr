package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import jr.brian.home.canvas.data.CanvasLayoutManager
import jr.brian.home.canvas.data.CanvasTabType
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.model.PageType
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.rom.PinnedRomInfo
import jr.brian.home.model.widget.WidgetConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PageOrderCoordinatorTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var pageTypeContext: Context
    private lateinit var pageTypePrefs: SharedPreferences
    private lateinit var pageTypeEditor: SharedPreferences.Editor
    private val pageTypeStore = mutableMapOf<String, Any?>()

    private lateinit var sharedContext: Context
    private val storesByPrefName = mutableMapOf<String, MutableMap<String, Any?>>()
    private val prefsByName = mutableMapOf<String, SharedPreferences>()

    private lateinit var pageTypeManager: PageTypeManager
    private lateinit var pageCountManager: PageCountManager
    private lateinit var homeTabManager: HomeTabManager
    private lateinit var appVisibilityManager: AppVisibilityManager
    private lateinit var appPositionManager: AppPositionManager
    private lateinit var canvasLayoutManager: CanvasLayoutManager
    private lateinit var pinnedRomManager: PinnedRomManager
    private lateinit var dockManager: DockManager
    private lateinit var floatyModeManager: FloatyModeManager
    private lateinit var appDrawerFabManager: AppDrawerFabManager
    private lateinit var notificationManager: NotificationManager

    private lateinit var folderManager: FolderManager
    private lateinit var widgetPageAppManager: WidgetPageAppManager
    private lateinit var widgetPreferences: WidgetPreferences

    private lateinit var coordinator: PageOrderCoordinator

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        sharedContext = mockk(relaxed = true)
        every { sharedContext.getSharedPreferences(any(), any()) } answers {
            val name = firstArg<String>()
            prefsByName.getOrPut(name) { newMockPrefs(name) }
        }

        pageTypeManager = PageTypeManager(sharedContext)
        pageCountManager = PageCountManager(sharedContext)
        homeTabManager = HomeTabManager(sharedContext)
        appVisibilityManager = AppVisibilityManager(sharedContext)
        appPositionManager = AppPositionManager(sharedContext)
        canvasLayoutManager = CanvasLayoutManager(sharedContext)
        pinnedRomManager = PinnedRomManager(sharedContext)
        dockManager = DockManager(sharedContext)
        floatyModeManager = FloatyModeManager(sharedContext)
        appDrawerFabManager = AppDrawerFabManager(sharedContext)
        notificationManager = NotificationManager(sharedContext)

        folderManager = mockk(relaxed = true)
        widgetPageAppManager = mockk(relaxed = true)
        widgetPreferences = mockk(relaxed = true)
        coEvery { widgetPreferences.widgetConfigs } returns flowOf(emptyList())

        coordinator = PageOrderCoordinator(
            pageTypeManager = pageTypeManager,
            pageCountManager = pageCountManager,
            homeTabManager = homeTabManager,
            appVisibilityManager = appVisibilityManager,
            appPositionManager = appPositionManager,
            folderManager = folderManager,
            pinnedRomManager = pinnedRomManager,
            canvasLayoutManager = canvasLayoutManager,
            widgetPageAppManager = widgetPageAppManager,
            widgetPreferences = widgetPreferences,
            dockManager = dockManager,
            appDrawerFabManager = appDrawerFabManager,
            floatyModeManager = floatyModeManager,
            notificationManager = notificationManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        storesByPrefName.clear()
        prefsByName.clear()
    }

    private fun newMockPrefs(name: String): SharedPreferences {
        val store = storesByPrefName.getOrPut(name) { mutableMapOf() }
        val prefs = mockk<SharedPreferences>(relaxed = true)
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { prefs.edit() } returns editor
        every { prefs.all } answers { store.toMap() }
        every { editor.apply() } answers { }
        every { editor.commit() } returns true
        every { editor.putString(any(), any()) } answers {
            store[firstArg()] = secondArg(); editor
        }
        every { editor.putBoolean(any(), any()) } answers {
            store[firstArg()] = secondArg(); editor
        }
        every { editor.putInt(any(), any()) } answers {
            store[firstArg()] = secondArg(); editor
        }
        every { editor.remove(any()) } answers {
            store.remove(firstArg<String>()); editor
        }
        every { editor.clear() } answers {
            store.clear(); editor
        }
        every { prefs.getString(any(), any()) } answers {
            store[firstArg()] as? String ?: secondArg()
        }
        every { prefs.getBoolean(any(), any()) } answers {
            store[firstArg()] as? Boolean ?: secondArg()
        }
        every { prefs.getInt(any(), any()) } answers {
            store[firstArg()] as? Int ?: secondArg()
        }
        return prefs
    }

    private fun rom(key: String) = PinnedRomInfo(
        key = key,
        name = key,
        systemName = "snes",
        path = "/roms/$key.smc"
    )

    @Test
    fun `reorder fans out permutation to every per-page store`() = runTest {
        pageTypeManager.addPage(PageType.APPS_TAB)
        pageTypeManager.addPage(PageType.APPS_AND_WIDGETS_TAB)
        pageTypeManager.addPage(PageType.UNIFIED_CANVAS)

        appVisibilityManager.hideApp(0, "com.app0")
        appVisibilityManager.hideApp(2, "com.app2")
        appPositionManager.savePosition(0, AppPosition("com.a", 1f, 1f, 64f))
        appPositionManager.savePosition(2, AppPosition("com.c", 3f, 3f, 64f))
        canvasLayoutManager.addItem(2, CanvasItem.AppItem(id = "x", packageName = "com.x"))
        pinnedRomManager.addPinnedRom(0, rom("apps0"), PinnedRomManager.TAB_TYPE_APPS)
        pinnedRomManager.addPinnedRom(2, rom("canvas2"), CanvasTabType.VALUE)
        dockManager.setDockVisiblePages(setOf(0, 2))
        notificationManager.saveShadeTabPage(2)
        homeTabManager.setHomeTabIndex(2)

        // new order: [APPS_TAB(was 0), UNIFIED_CANVAS(was 2), APPS_AND_WIDGETS_TAB(was 1)]
        val newOrder = listOf(
            PageType.APPS_TAB,
            PageType.UNIFIED_CANVAS,
            PageType.APPS_AND_WIDGETS_TAB
        )
        val oldIndicesInNewOrder = listOf(0, 2, 1)

        coordinator.reorder(
            newOrder = newOrder,
            oldIndicesInNewOrder = oldIndicesInNewOrder,
            newCurrentTabIndex = 1
        )

        // Page types and home updated.
        assertEquals(newOrder, pageTypeManager.pageTypes.value)
        assertEquals(1, homeTabManager.homeTabIndex.value)

        // Hidden apps and positions follow.
        assertEquals(setOf("com.app0"), appVisibilityManager.getHiddenApps(0))
        assertEquals(setOf("com.app2"), appVisibilityManager.getHiddenApps(1))
        assertEquals("com.a", appPositionManager.getPosition(0, "com.a")?.packageName)
        assertEquals("com.c", appPositionManager.getPosition(1, "com.c")?.packageName)

        // Canvas layout moved from old 2 to new 1.
        assertEquals("x", canvasLayoutManager.getLayout(1).items.single().id)

        // Pinned ROMs followed in their tab namespaces.
        assertEquals(
            "apps0",
            pinnedRomManager.getPinnedRoms(0, PinnedRomManager.TAB_TYPE_APPS).first().single().key
        )
        assertEquals(
            "canvas2",
            pinnedRomManager.getPinnedRoms(1, CanvasTabType.VALUE).first().single().key
        )

        // Dock visible pages remapped: old 0 -> new 0, old 2 -> new 1.
        assertEquals(setOf(0, 1), dockManager.dockVisiblePages.value)

        // Shade tab page followed (was 2 -> new 1).
        assertEquals(1, notificationManager.shadeTabPage)

        // Coordinator should have fanned out to folder & widget-page managers too.
        coVerify { folderManager.reorderPages(any(), PinnedRomManager.TAB_TYPE_APPS) }
        coVerify { folderManager.reorderPages(any(), CanvasTabType.VALUE) }
        coVerify { widgetPageAppManager.reorderPages(any()) }
    }

    @Test
    fun `removePage shifts content and adjusts home tab index`() = runTest {
        pageTypeManager.addPage(PageType.APPS_TAB)
        pageTypeManager.addPage(PageType.APPS_TAB)
        pageTypeManager.addPage(PageType.APPS_TAB)
        pageCountManager.setPageCount(3)
        appVisibilityManager.hideApp(0, "com.app0")
        appVisibilityManager.hideApp(2, "com.app2")
        homeTabManager.setHomeTabIndex(2)

        coordinator.removePage(pagerPageIndex = 1)

        assertEquals(2, pageTypeManager.pageTypes.value.size)
        assertEquals(setOf("com.app0"), appVisibilityManager.getHiddenApps(0))
        assertEquals(setOf("com.app2"), appVisibilityManager.getHiddenApps(1))
        assertEquals(1, homeTabManager.homeTabIndex.value) // 2 was above 1, decrement
    }

    @Test
    fun `removePage of widget-hosting page invokes onDeleteWidgetPage with its widget-page index`() = runTest {
        pageTypeManager.addPage(PageType.APPS_TAB)
        pageTypeManager.addPage(PageType.APPS_AND_WIDGETS_TAB)
        pageTypeManager.addPage(PageType.APPS_AND_WIDGETS_TAB)
        pageCountManager.setPageCount(3)

        var deletedWidgetPage: Int? = null
        coordinator.removePage(pagerPageIndex = 2) { idx -> deletedWidgetPage = idx }

        // Widget page index of pager page 2 is 1 (only one non-APPS_TAB before it).
        assertEquals(1, deletedWidgetPage)
    }

    @Test
    fun `reorder rewrites WidgetConfig pageIndex via WidgetPreferences`() = runTest {
        pageTypeManager.addPage(PageType.APPS_TAB)
        pageTypeManager.addPage(PageType.APPS_AND_WIDGETS_TAB)
        pageTypeManager.addPage(PageType.APPS_AND_WIDGETS_TAB)

        val w0 = widgetConfig(widgetId = 100, pageIndex = 0)
        val w1 = widgetConfig(widgetId = 101, pageIndex = 1)
        coEvery { widgetPreferences.widgetConfigs } returns flowOf(listOf(w0, w1))

        var captured: List<WidgetConfig>? = null
        coEvery { widgetPreferences.saveWidgetConfigs(any()) } answers {
            captured = firstArg()
        }

        // Reverse the two widget pages.
        // pager: [APPS_TAB, APPS_AND_WIDGETS_TAB(was 2), APPS_AND_WIDGETS_TAB(was 1)]
        coordinator.reorder(
            newOrder = listOf(
                PageType.APPS_TAB,
                PageType.APPS_AND_WIDGETS_TAB,
                PageType.APPS_AND_WIDGETS_TAB
            ),
            oldIndicesInNewOrder = listOf(0, 2, 1),
            newCurrentTabIndex = 0
        )

        // Widget page 0 (was 0) becomes 1; widget page 1 (was 1) becomes 0.
        val ids = captured?.associate { it.widgetId to it.pageIndex }
        assertEquals(mapOf(100 to 1, 101 to 0), ids)
    }

    private fun widgetConfig(widgetId: Int, pageIndex: Int) = WidgetConfig(
        widgetId = widgetId,
        providerClassName = "com.example.WidgetProvider",
        providerPackageName = "com.example",
        x = 0,
        y = 0,
        width = 100,
        height = 100,
        pageIndex = pageIndex,
        order = 0
    )
}
