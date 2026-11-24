package jr.brian.home.canvas.jarn

import android.appwidget.AppWidgetHost
import android.content.Context
import android.content.SharedPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import jr.brian.home.canvas.data.CanvasLayoutManager
import jr.brian.home.canvas.data.CanvasTabType
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.data.FolderManager
import jr.brian.home.data.PageTypeManager
import jr.brian.home.data.WidgetPreferences
import jr.brian.home.model.PageType
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import jr.brian.home.model.widget.WidgetConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Every store here outlives an [AppJarnCanvasTiles], the way disk outlives a process. */
class JarnTileRemovalTest {
    private val journalDisk = mutableMapOf<String, String>()
    private val saved = MutableStateFlow<Map<Int, CanvasLayout>>(emptyMap())
    private val folderStore = mutableMapOf<String, Folder>()
    private val widgetConfigs = MutableStateFlow<List<WidgetConfig>>(emptyList())
    private var dieDuringFolderDelete = false
    private var dieDuringWidgetRelease = false

    private val context = mockk<Context>(relaxed = true)
    private val layouts = mockk<CanvasLayoutManager>(relaxed = true)
    private val folders = mockk<FolderManager>(relaxed = true)
    private val widgetPreferences = mockk<WidgetPreferences>(relaxed = true)

    @Before
    fun setup() {
        val prefs = mockk<SharedPreferences>(relaxed = true)
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns editor
        every { prefs.getString(any(), any()) } answers { journalDisk[firstArg()] ?: secondArg() }
        every { editor.putString(any(), any()) } answers { journalDisk[firstArg()] = secondArg(); editor }
        every { editor.remove(any()) } answers { journalDisk.remove(firstArg<String>()); editor }
        every { editor.commit() } returns true

        every { layouts.layoutsByPage } returns saved
        every { layouts.getLayout(any()) } answers { saved.value[firstArg()] ?: CanvasLayout() }
        every { layouts.removeItem(any(), any()) } answers {
            val page = firstArg<Int>()
            val id = secondArg<String>()
            val layout = saved.value.getValue(page)
            saved.value = saved.value + (page to layout.copy(items = layout.items.filter { it.id != id }))
        }

        coEvery { folders.readFolders(any(), CanvasTabType.VALUE) } answers { folderStore.values.toList() }
        coEvery { folders.deleteFolder(any(), any(), CanvasTabType.VALUE) } answers {
            if (dieDuringFolderDelete) throw ProcessDeath()
            folderStore.remove(secondArg<String>())
            true
        }
        every { widgetPreferences.widgetConfigs } answers {
            if (dieDuringWidgetRelease) throw ProcessDeath()
            widgetConfigs
        }

        mockkConstructor(AppWidgetHost::class)
        every { anyConstructed<AppWidgetHost>().deleteAppWidgetId(any()) } returns Unit
    }

    @After
    fun tearDown() = unmockkAll()

    private class ProcessDeath : RuntimeException()

    private fun launch() = AppJarnCanvasTiles(
        context = context,
        layouts = layouts,
        pageTypes = mockk<PageTypeManager>(relaxed = true).also {
            every { it.pageTypes } returns MutableStateFlow(listOf(PageType.UNIFIED_CANVAS, PageType.UNIFIED_CANVAS))
        },
        folders = folders,
        pinnedRoms = mockk(relaxed = true),
        customNames = mockk(relaxed = true),
        widgetPreferences = widgetPreferences,
        nowPlaying = mockk(relaxed = true),
        rss = mockk(relaxed = true),
        removals = JarnTileRemovalJournal(context),
    )

    private fun page(index: Int, vararg items: CanvasItem) {
        saved.value = saved.value + (index to CanvasLayout(items = items.toList()))
    }

    private fun givenFolder(id: String) {
        folderStore[id] = Folder(id, "Games", listOf("a"), AppPosition("a", 0f, 0f))
    }

    private fun AppJarnCanvasTiles.dies(block: suspend AppJarnCanvasTiles.() -> Unit) = runBlocking {
        assertTrue(runCatching { block() }.exceptionOrNull() is ProcessDeath)
    }

    private fun tileIds(index: Int) = saved.value.getValue(index).items.map { it.id }

    @Test
    fun `a death before the folder is deleted leaves no tile on a missing folder, and the next start finishes`() {
        givenFolder("f1")
        page(0, CanvasItem.FolderItem("folder-f1", "f1"), CanvasItem.AppItem("app", "a"))
        dieDuringFolderDelete = true

        launch().dies { removeTile(0, "folder-f1") }

        assertEquals(listOf("app"), tileIds(0))
        assertTrue("f1" in folderStore)
        assertTrue(JarnTileRemovalJournal(context).hasPending())

        dieDuringFolderDelete = false
        runBlocking { launch().finishPendingRemovals() }

        assertFalse("f1" in folderStore)
        assertFalse(JarnTileRemovalJournal(context).hasPending())
        assertEquals(listOf("app"), tileIds(0))
    }

    @Test
    fun `finishing again after a clean finish changes nothing`() {
        givenFolder("f1")
        page(0, CanvasItem.FolderItem("folder-f1", "f1"))
        runBlocking { launch().removeTile(0, "folder-f1") }
        givenFolder("f1")

        runBlocking { launch().finishPendingRemovals() }

        assertTrue("f1" in folderStore)
        coVerify(exactly = 1) { folders.deleteFolder(any(), any(), any()) }
    }

    @Test
    fun `a tile whose removal never reached disk keeps its folder on the next start`() {
        givenFolder("f1")
        page(0, CanvasItem.FolderItem("folder-f1", "f1"))
        dieDuringFolderDelete = true
        launch().dies { removeTile(0, "folder-f1") }
        page(0, CanvasItem.FolderItem("folder-f1", "f1"))
        dieDuringFolderDelete = false

        runBlocking { launch().finishPendingRemovals() }

        assertTrue("f1" in folderStore)
        assertFalse(JarnTileRemovalJournal(context).hasPending())
    }

    @Test
    fun `a death before the widget id is released frees it on the next start`() {
        page(0, CanvasItem.WidgetItem("widget-7", 7))
        dieDuringWidgetRelease = true

        launch().dies { removeTile(0, "widget-7") }
        assertEquals(emptyList<String>(), tileIds(0))
        verify(exactly = 0) { anyConstructed<AppWidgetHost>().deleteAppWidgetId(any()) }

        dieDuringWidgetRelease = false
        runBlocking { launch().finishPendingRemovals() }

        verify(exactly = 1) { anyConstructed<AppWidgetHost>().deleteAppWidgetId(7) }
        assertFalse(JarnTileRemovalJournal(context).hasPending())
    }

    @Test
    fun `a widget id used on another page or a widget page is never released`() {
        page(0, CanvasItem.WidgetItem("widget-7", 7), CanvasItem.WidgetItem("widget-8", 8))
        page(1, CanvasItem.WidgetItem("other-7", 7))
        widgetConfigs.value = listOf(WidgetConfig(widgetId = 8, providerClassName = "P", providerPackageName = "p"))

        runBlocking {
            val tiles = launch()
            tiles.removeTile(0, "widget-7")
            tiles.removeTile(0, "widget-8")
        }

        verify(exactly = 0) { anyConstructed<AppWidgetHost>().deleteAppWidgetId(any()) }
        assertFalse(JarnTileRemovalJournal(context).hasPending())
    }

    @Test
    fun `a folder still placed on another page is never deleted`() {
        givenFolder("f1")
        page(0, CanvasItem.FolderItem("folder-f1", "f1"))
        page(1, CanvasItem.FolderItem("copy-f1", "f1"))

        runBlocking { launch().removeTile(0, "folder-f1") }

        assertTrue("f1" in folderStore)
        coVerify(exactly = 0) { folders.deleteFolder(any(), any(), any()) }
    }

    @Test
    fun `an unreadable folder store keeps the tile and writes no journal`() {
        givenFolder("f1")
        page(0, CanvasItem.FolderItem("folder-f1", "f1"))
        coEvery { folders.readFolders(0, CanvasTabType.VALUE) } returns null

        runBlocking { launch().removeTile(0, "folder-f1") }

        assertEquals(listOf("folder-f1"), tileIds(0))
        assertFalse(JarnTileRemovalJournal(context).hasPending())
    }
}
