package jr.brian.home.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import jr.brian.home.canvas.data.CanvasTabType
import jr.brian.home.data.FolderManager.Companion.TAB_TYPE_APPS
import jr.brian.home.data.FolderManager.Companion.TAB_TYPE_WIDGETS
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class FolderManagerReorderTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var context: Context
    private lateinit var folderManager: FolderManager

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        folderManager = FolderManager(context)
        clearAllPages()
    }

    @After
    fun tearDown() = runBlocking {
        clearAllPages()
        testScope.cancel()
    }

    private suspend fun clearAllPages() {
        for (tabType in listOf(TAB_TYPE_APPS, TAB_TYPE_WIDGETS, CanvasTabType.VALUE)) {
            for (pageIndex in 0..9) {
                folderManager.setAllFolders(pageIndex, tabType, emptyList())
            }
        }
    }

    private fun folder(id: String) = Folder(
        id = id,
        name = id,
        appPackageNames = listOf("com.$id"),
        position = AppPosition(packageName = id, x = 0f, y = 0f, iconSize = 64f)
    )

    @Test
    fun reorderPages_remapsAppsFoldersToNewIndices() = testScope.runTest {
        folderManager.createFolder(0, folder("f0"), TAB_TYPE_APPS)
        folderManager.createFolder(1, folder("f1"), TAB_TYPE_APPS)
        folderManager.createFolder(2, folder("f2"), TAB_TYPE_APPS)

        folderManager.reorderPages(mapOf(0 to 2, 1 to 0, 2 to 1), TAB_TYPE_APPS)

        assertEquals("f2", folderManager.getFolders(0, TAB_TYPE_APPS).first().single().id)
        assertEquals("f0", folderManager.getFolders(1, TAB_TYPE_APPS).first().single().id)
        assertEquals("f1", folderManager.getFolders(2, TAB_TYPE_APPS).first().single().id)
    }

    @Test
    fun reorderPages_preservesOtherTabTypes() = testScope.runTest {
        folderManager.createFolder(0, folder("apps0"), TAB_TYPE_APPS)
        folderManager.createFolder(1, folder("apps1"), TAB_TYPE_APPS)
        folderManager.createFolder(0, folder("canvas0"), CanvasTabType.VALUE)
        folderManager.createFolder(1, folder("canvas1"), CanvasTabType.VALUE)

        folderManager.reorderPages(mapOf(0 to 1, 1 to 0), TAB_TYPE_APPS)

        assertEquals("apps1", folderManager.getFolders(0, TAB_TYPE_APPS).first().single().id)
        assertEquals("apps0", folderManager.getFolders(1, TAB_TYPE_APPS).first().single().id)
        assertEquals("canvas0", folderManager.getFolders(0, CanvasTabType.VALUE).first().single().id)
        assertEquals("canvas1", folderManager.getFolders(1, CanvasTabType.VALUE).first().single().id)
    }

    @Test
    fun removePage_shiftsLaterIndicesDown() = testScope.runTest {
        folderManager.createFolder(0, folder("f0"), TAB_TYPE_APPS)
        folderManager.createFolder(1, folder("f1"), TAB_TYPE_APPS)
        folderManager.createFolder(2, folder("f2"), TAB_TYPE_APPS)

        folderManager.removePage(1, TAB_TYPE_APPS)

        assertEquals("f0", folderManager.getFolders(0, TAB_TYPE_APPS).first().single().id)
        assertEquals("f2", folderManager.getFolders(1, TAB_TYPE_APPS).first().single().id)
        assertTrue(folderManager.getFolders(2, TAB_TYPE_APPS).first().isEmpty())
    }

    @Test
    fun removePage_canvasTabTypeShiftsIndependentlyOfApps() = testScope.runTest {
        folderManager.createFolder(0, folder("apps0"), TAB_TYPE_APPS)
        folderManager.createFolder(2, folder("apps2"), TAB_TYPE_APPS)
        folderManager.createFolder(0, folder("canvas0"), CanvasTabType.VALUE)
        folderManager.createFolder(2, folder("canvas2"), CanvasTabType.VALUE)

        folderManager.removePage(0, CanvasTabType.VALUE)

        assertTrue(folderManager.getFolders(0, CanvasTabType.VALUE).first().isEmpty())
        assertEquals("canvas2", folderManager.getFolders(1, CanvasTabType.VALUE).first().single().id)
        assertEquals("apps0", folderManager.getFolders(0, TAB_TYPE_APPS).first().single().id)
        assertEquals("apps2", folderManager.getFolders(2, TAB_TYPE_APPS).first().single().id)
    }
}
