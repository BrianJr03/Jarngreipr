package jr.brian.home.canvas.jarn

import android.net.Uri
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import jr.brian.home.canvas.data.CanvasLayoutManager
import jr.brian.home.canvas.data.CanvasTabType
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.data.FolderManager
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import kotlinx.coroutines.flow.MutableStateFlow
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
        removals = mockk(relaxed = true),
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
        every { layouts.layoutsByPage } returns MutableStateFlow(emptyMap())
        coEvery { folders.readFolders(PAGE, CanvasTabType.VALUE) } returns listOf(folder)
        coEvery { folders.getFolder(PAGE, "id", CanvasTabType.VALUE) } returns folder
        coEvery { folders.deleteFolder(PAGE, "id", CanvasTabType.VALUE) } returns true
        coEvery { folders.updateFolder(PAGE, any(), CanvasTabType.VALUE) } returns true
    }

    @Test
    fun `an unreadable folder store keeps every tile and the folder`() = runBlocking {
        givenFolderOnPage()
        coEvery { folders.readFolders(PAGE, CanvasTabType.VALUE) } returns null
        subject.deleteFolder(PAGE, "id")
        verify(exactly = 0) { layouts.removeItem(any(), any()) }
        coVerify(exactly = 0) { folders.deleteFolder(any(), any(), any()) }
    }

    @Test
    fun `a refused image background discards the newly saved copy`() = runBlocking {
        mockkStatic(Uri::class)
        every { Uri.parse("content://image") } returns mockk()
        coEvery { folders.saveFolderBackgroundImage("id", any()) } returns Result.success("/new.png")
        coEvery {
            folders.updateFolderBackground(PAGE, "id", null, "/new.png", CanvasTabType.VALUE)
        } returns false

        assertFalse(subject.setFolderBackgroundImage(PAGE, "id", "content://image"))

        verify { folders.discardFolderBackgroundImage("/new.png") }
        unmockkStatic(Uri::class)
    }

    @Test
    fun `deleting removes only its own tile first, then the folder`() = runBlocking {
        givenFolderOnPage()
        subject.deleteFolder(PAGE, "id")
        coVerifyOrder {
            layouts.removeItem(PAGE, "folder-id")
            folders.deleteFolder(PAGE, "id", CanvasTabType.VALUE)
        }
        verify(exactly = 0) { layouts.removeItem(PAGE, "folder-other") }
        verify(exactly = 0) { layouts.removeItem(PAGE, "app") }
    }

    @Test
    fun `emptying a folder removes it and its tile, leaving no missing tile`() = runBlocking {
        givenFolderOnPage()
        subject.editFolderApps(PAGE, "id", emptySet(), setOf("gone", "a"))
        coVerifyOrder {
            layouts.removeItem(PAGE, "folder-id")
            folders.deleteFolder(PAGE, "id", CanvasTabType.VALUE)
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
