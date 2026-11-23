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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `moving a tile saves the folder before clearing the tile`() = runBlocking {
        givenFolderOnPage()
        val saved = folder.copy(appPackageNames = listOf("gone", "a", "new"))
        every { layouts.getLayout(OTHER_PAGE) } returns CanvasLayout(items = listOf(CanvasItem.AppItem("tile", "new")))
        coEvery { folders.getFolder(PAGE, "id", CanvasTabType.VALUE) } returnsMany listOf(folder, saved)
        assertTrue(subject.moveAppTileToFolder(OTHER_PAGE, "tile", PAGE, "id"))
        coVerifyOrder {
            folders.updateFolder(PAGE, saved, CanvasTabType.VALUE)
            layouts.removeItem(OTHER_PAGE, "tile")
        }
    }

    @Test
    fun `an app already in the folder is not added twice, and its tile still goes`() = runBlocking {
        givenFolderOnPage()
        assertTrue(subject.moveAppTileToFolder(PAGE, "app", PAGE, "id"))
        coVerify(exactly = 0) { folders.updateFolder(any(), any(), any()) }
        verify { layouts.removeItem(PAGE, "app") }
    }

    @Test
    fun `a folder save that did not stick keeps the tile`() = runBlocking {
        givenFolderOnPage()
        every { layouts.getLayout(OTHER_PAGE) } returns CanvasLayout(items = listOf(CanvasItem.AppItem("tile", "new")))
        coEvery { folders.getFolder(PAGE, "id", CanvasTabType.VALUE) } returns folder
        assertFalse(subject.moveAppTileToFolder(OTHER_PAGE, "tile", PAGE, "id"))
        verify(exactly = 0) { layouts.removeItem(any(), any()) }
    }

    @Test
    fun `a folder save that throws keeps the tile`() = runBlocking {
        givenFolderOnPage()
        every { layouts.getLayout(OTHER_PAGE) } returns CanvasLayout(items = listOf(CanvasItem.AppItem("tile", "new")))
        coEvery { folders.updateFolder(any(), any(), any()) } throws IllegalStateException("disk")
        assertTrue(runCatching { subject.moveAppTileToFolder(OTHER_PAGE, "tile", PAGE, "id") }.isFailure)
        verify(exactly = 0) { layouts.removeItem(any(), any()) }
    }

    @Test
    fun `a missing folder or a non-app tile changes nothing`() = runBlocking {
        givenFolderOnPage()
        coEvery { folders.getFolder(PAGE, "missing", CanvasTabType.VALUE) } returns null
        assertFalse(subject.moveAppTileToFolder(PAGE, "app", PAGE, "missing"))
        assertFalse(subject.moveAppTileToFolder(PAGE, "folder-other", PAGE, "id"))
        coVerify(exactly = 0) { folders.updateFolder(any(), any(), any()) }
        verify(exactly = 0) { layouts.removeItem(any(), any()) }
    }

    private companion object {
        const val OTHER_PAGE = 1
        const val PAGE = 2
    }
}
