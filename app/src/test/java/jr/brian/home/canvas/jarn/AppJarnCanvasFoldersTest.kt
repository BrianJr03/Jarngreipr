package jr.brian.home.canvas.jarn

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jr.brian.home.canvas.data.CanvasLayoutManager
import jr.brian.home.canvas.data.CanvasTabType
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.data.FolderManager
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import kotlinx.coroutines.runBlocking
import org.junit.Test

class AppJarnCanvasFoldersTest {
    private val layouts = mockk<CanvasLayoutManager>(relaxed = true)
    private val folders = mockk<FolderManager>(relaxed = true)
    private val tiles = AppJarnCanvasTiles(
        context = mockk(relaxed = true),
        layouts = layouts,
        pageTypes = mockk(relaxed = true),
        folders = folders,
        pinnedRoms = mockk(relaxed = true),
        customNames = mockk(relaxed = true),
        widgetPreferences = mockk(relaxed = true),
        nowPlaying = mockk(relaxed = true),
        rss = mockk(relaxed = true),
    )
    private val subject = AppJarnCanvasFolders(
        layouts = layouts,
        folders = folders,
        tiles = tiles,
        appOptions = mockk(relaxed = true),
    )
    private val folder = Folder("id", "Games", listOf("gone", "a"), AppPosition("id", 0f, 0f))

    private fun givenFolderOnPage() {
        every { layouts.getLayout(PAGE) } returns CanvasLayout(
            items = listOf(
                CanvasItem.FolderItem("folder-id", "id"),
                CanvasItem.FolderItem("folder-other", "other"),
                CanvasItem.AppItem("app", "a"),
            ),
        )
        coEvery { folders.getFolder(PAGE, "id", CanvasTabType.VALUE) } returns folder
    }

    @Test
    fun `deleting removes the folder first, then only its own tile`() = runBlocking {
        givenFolderOnPage()
        subject.deleteFolder(PAGE, "id")
        coVerifyOrder {
            folders.deleteFolder(PAGE, "id", CanvasTabType.VALUE)
            layouts.removeItem(PAGE, "folder-id")
        }
        verify(exactly = 0) { layouts.removeItem(PAGE, "folder-other") }
        verify(exactly = 0) { layouts.removeItem(PAGE, "app") }
    }

    @Test
    fun `emptying a folder removes it and its tile, leaving no missing tile`() = runBlocking {
        givenFolderOnPage()
        subject.editFolderApps(PAGE, "id", emptySet(), setOf("gone", "a"))
        coVerifyOrder {
            folders.deleteFolder(PAGE, "id", CanvasTabType.VALUE)
            layouts.removeItem(PAGE, "folder-id")
        }
    }

    @Test
    fun `editing keeps apps that are not installed and never drops other folders`() = runBlocking {
        givenFolderOnPage()
        subject.editFolderApps(PAGE, "id", setOf("b"), setOf("a"))
        coVerify {
            folders.updateFolder(
                PAGE,
                folder.copy(appPackageNames = listOf("gone", "b")),
                CanvasTabType.VALUE,
            )
        }
        coVerify(exactly = 0) { folders.updateFolderApps(any(), any(), any(), any()) }
        verify(exactly = 0) { layouts.removeItem(any(), any()) }
    }

    private companion object {
        const val PAGE = 2
    }
}
