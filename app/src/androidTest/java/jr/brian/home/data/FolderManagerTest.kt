package jr.brian.home.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class FolderManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var context: Context
    private lateinit var folderManager: FolderManager

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        folderManager = FolderManager(context)
        clearAllFolderData()
    }

    @After
    fun tearDown() = runBlocking {
        clearAllFolderData()
        testScope.cancel()
    }
    
    private suspend fun clearAllFolderData() {
        for (pageIndex in -1..20) {
            val folders = folderManager.getFolders(pageIndex, TAB_TYPE_APPS).first()
            folders.forEach { folder ->
                folderManager.deleteFolder(pageIndex, folder.id, TAB_TYPE_APPS)
            }
            
            val widgetFolders = folderManager.getFolders(pageIndex, TAB_TYPE_WIDGETS).first()
            widgetFolders.forEach { folder ->
                folderManager.deleteFolder(pageIndex, folder.id, TAB_TYPE_WIDGETS)
            }
        }
    }

    @Test
    fun createFolder_addsNewFolderToEmptyList() = testScope.runTest {
        // Given
        val pageIndex = 0
        val folder = createTestFolder(
            id = "folder1",
            name = "Work Apps",
            appPackageNames = listOf("com.example.app1", "com.example.app2")
        )

        // When
        folderManager.createFolder(pageIndex, folder)

        // Then
        val folders = folderManager.getFolders(pageIndex).first()
        assertEquals(1, folders.size)
        assertEquals("folder1", folders[0].id)
        assertEquals("Work Apps", folders[0].name)
        assertEquals(2, folders[0].appPackageNames.size)
    }

    @Test
    fun createFolder_addsFolderToExistingFolders() = testScope.runTest {
        // Given
        val pageIndex = 0
        val folder1 = createTestFolder(id = "folder1", name = "Work")
        val folder2 = createTestFolder(id = "folder2", name = "Games")

        // When
        folderManager.createFolder(pageIndex, folder1)
        folderManager.createFolder(pageIndex, folder2)

        // Then
        val folders = folderManager.getFolders(pageIndex).first()
        assertEquals(2, folders.size)
        assertTrue(folders.any { it.id == "folder1" })
        assertTrue(folders.any { it.id == "folder2" })
    }

    @Test
    fun createFolder_withDifferentTabTypes_storesSeparately() = testScope.runTest {
        // Given
        val pageIndex = 0
        val appFolder = createTestFolder(id = "appFolder", name = "Apps")
        val widgetFolder = createTestFolder(id = "widgetFolder", name = "Widgets")

        // When
        folderManager.createFolder(pageIndex, appFolder, TAB_TYPE_APPS)
        folderManager.createFolder(pageIndex, widgetFolder, TAB_TYPE_WIDGETS)

        // Then
        val appFolders = folderManager.getFolders(pageIndex, TAB_TYPE_APPS).first()
        val widgetFolders = folderManager.getFolders(pageIndex, TAB_TYPE_WIDGETS).first()
        
        assertEquals(1, appFolders.size)
        assertEquals(1, widgetFolders.size)
        assertEquals("appFolder", appFolders[0].id)
        assertEquals("widgetFolder", widgetFolders[0].id)
    }

    @Test
    fun createFolder_onDifferentPages_storesIndependently() = testScope.runTest {
        // Given
        val page0Folder = createTestFolder(id = "page0", name = "Page 0")
        val page1Folder = createTestFolder(id = "page1", name = "Page 1")

        // When
        folderManager.createFolder(0, page0Folder)
        folderManager.createFolder(1, page1Folder)

        // Then
        val page0Folders = folderManager.getFolders(0).first()
        val page1Folders = folderManager.getFolders(1).first()
        
        assertEquals(1, page0Folders.size)
        assertEquals(1, page1Folders.size)
        assertEquals("page0", page0Folders[0].id)
        assertEquals("page1", page1Folders[0].id)
    }

    @Test
    fun createFolder_preservesPositionData() = testScope.runTest {
        // Given
        val folder = createTestFolder(
            id = "folder1",
            name = "Test",
            x = 100f,
            y = 200f,
            iconSize = 80f
        )

        // When
        folderManager.createFolder(0, folder)

        // Then
        val folders = folderManager.getFolders(0).first()
        assertEquals(100f, folders[0].position.x, 0.01f)
        assertEquals(200f, folders[0].position.y, 0.01f)
        assertEquals(80f, folders[0].position.iconSize, 0.01f)
    }

    @Test
    fun updateFolder_modifiesExistingFolder() = testScope.runTest {
        // Given
        val originalFolder = createTestFolder(
            id = "folder1",
            name = "Original",
            appPackageNames = listOf("com.app1")
        )
        folderManager.createFolder(0, originalFolder)

        val updatedFolder = originalFolder.copy(
            name = "Updated",
            appPackageNames = listOf("com.app1", "com.app2", "com.app3")
        )

        // When
        folderManager.updateFolder(0, updatedFolder)

        // Then
        val folders = folderManager.getFolders(0).first()
        assertEquals(1, folders.size)
        assertEquals("Updated", folders[0].name)
        assertEquals(3, folders[0].appPackageNames.size)
    }

    @Test
    fun updateFolder_onlyUpdatesMatchingFolderId() = testScope.runTest {
        // Given
        val folder1 = createTestFolder(id = "folder1", name = "Folder 1")
        val folder2 = createTestFolder(id = "folder2", name = "Folder 2")
        folderManager.createFolder(0, folder1)
        folderManager.createFolder(0, folder2)

        val updatedFolder1 = folder1.copy(name = "Updated Folder 1")

        // When
        folderManager.updateFolder(0, updatedFolder1)

        // Then
        val folders = folderManager.getFolders(0).first()
        assertEquals(2, folders.size)
        assertEquals("Updated Folder 1", folders.find { it.id == "folder1" }?.name)
        assertEquals("Folder 2", folders.find { it.id == "folder2" }?.name)
    }

    @Test
    fun updateFolder_onNonExistentFolder_doesNothing() = testScope.runTest {
        // Given
        val existingFolder = createTestFolder(id = "folder1", name = "Existing")
        folderManager.createFolder(0, existingFolder)

        val nonExistentFolder = createTestFolder(id = "nonexistent", name = "Does Not Exist")

        // When
        folderManager.updateFolder(0, nonExistentFolder)

        // Then
        val folders = folderManager.getFolders(0).first()
        assertEquals(1, folders.size)
        assertEquals("folder1", folders[0].id)
    }

    @Test
    fun deleteFolder_removesFolderFromList() = testScope.runTest {
        // Given
        val folder = createTestFolder(id = "folder1", name = "To Delete")
        folderManager.createFolder(0, folder)

        // When
        folderManager.deleteFolder(0, "folder1")

        // Then
        val folders = folderManager.getFolders(0).first()
        assertTrue(folders.isEmpty())
    }

    @Test
    fun deleteFolder_removesOnlySpecifiedFolder() = testScope.runTest {
        // Given
        val folder1 = createTestFolder(id = "folder1", name = "Keep")
        val folder2 = createTestFolder(id = "folder2", name = "Delete")
        val folder3 = createTestFolder(id = "folder3", name = "Keep")
        
        folderManager.createFolder(0, folder1)
        folderManager.createFolder(0, folder2)
        folderManager.createFolder(0, folder3)

        // When
        folderManager.deleteFolder(0, "folder2")

        // Then
        val folders = folderManager.getFolders(0).first()
        assertEquals(2, folders.size)
        assertTrue(folders.any { it.id == "folder1" })
        assertTrue(folders.any { it.id == "folder3" })
        assertFalse(folders.any { it.id == "folder2" })
    }

    @Test
    fun deleteFolder_onNonExistentId_doesNotThrow() = testScope.runTest {
        // Given
        val folder = createTestFolder(id = "folder1", name = "Existing")
        folderManager.createFolder(0, folder)

        // When
        folderManager.deleteFolder(0, "nonexistent")

        // Then
        val folders = folderManager.getFolders(0).first()
        assertEquals(1, folders.size)
    }

    @Test
    fun renameFolder_changesFolderName() = testScope.runTest {
        // Given
        val folder = createTestFolder(id = "folder1", name = "Old Name")
        folderManager.createFolder(0, folder)

        // When
        folderManager.renameFolder(0, "folder1", "New Name")

        // Then
        val folders = folderManager.getFolders(0).first()
        assertEquals("New Name", folders[0].name)
    }

    @Test
    fun renameFolder_onlyRenamesMatchingFolder() = testScope.runTest {
        // Given
        val folder1 = createTestFolder(id = "folder1", name = "Name 1")
        val folder2 = createTestFolder(id = "folder2", name = "Name 2")
        folderManager.createFolder(0, folder1)
        folderManager.createFolder(0, folder2)

        // When
        folderManager.renameFolder(0, "folder1", "Updated Name 1")

        // Then
        val folders = folderManager.getFolders(0).first()
        assertEquals("Updated Name 1", folders.find { it.id == "folder1" }?.name)
        assertEquals("Name 2", folders.find { it.id == "folder2" }?.name)
    }

    @Test
    fun renameFolder_preservesOtherFolderProperties() = testScope.runTest {
        // Given
        val folder = createTestFolder(
            id = "folder1",
            name = "Original",
            appPackageNames = listOf("com.app1", "com.app2"),
            x = 50f,
            y = 100f
        )
        folderManager.createFolder(0, folder)

        // When
        folderManager.renameFolder(0, "folder1", "Renamed")

        // Then
        val folders = folderManager.getFolders(0).first()
        assertEquals("Renamed", folders[0].name)
        assertEquals(2, folders[0].appPackageNames.size)
        assertEquals(50f, folders[0].position.x, 0.01f)
        assertEquals(100f, folders[0].position.y, 0.01f)
    }

    @Test
    fun updateFolderApps_updatesAppList() = testScope.runTest {
        // Given
        val folder = createTestFolder(
            id = "folder1",
            name = "Folder",
            appPackageNames = listOf("com.app1")
        )
        folderManager.createFolder(0, folder)

        // When
        folderManager.updateFolderApps(
            0,
            "folder1",
            listOf("com.app1", "com.app2", "com.app3")
        )

        // Then
        val folders = folderManager.getFolders(0).first()
        assertEquals(3, folders[0].appPackageNames.size)
        assertTrue(folders[0].appPackageNames.contains("com.app2"))
    }

    @Test
    fun updateFolderApps_deletesFolderWhenAppListIsEmpty() = testScope.runTest {
        // Given
        val folder = createTestFolder(
            id = "folder1",
            name = "Folder",
            appPackageNames = listOf("com.app1")
        )
        folderManager.createFolder(0, folder)

        // When
        folderManager.updateFolderApps(0, "folder1", emptyList())

        // Then
        val folders = folderManager.getFolders(0).first()
        assertTrue(folders.isEmpty())
    }

    @Test
    fun updateFolderApps_onlyAffectsSpecifiedFolder() = testScope.runTest {
        // Given
        val folder1 = createTestFolder(
            id = "folder1",
            name = "Folder 1",
            appPackageNames = listOf("com.app1")
        )
        val folder2 = createTestFolder(
            id = "folder2",
            name = "Folder 2",
            appPackageNames = listOf("com.app2")
        )
        folderManager.createFolder(0, folder1)
        folderManager.createFolder(0, folder2)

        // When
        folderManager.updateFolderApps(0, "folder1", listOf("com.app1", "com.app3"))

        // Then
        val folders = folderManager.getFolders(0).first()
        val updatedFolder1 = folders.find { it.id == "folder1" }
        val unchangedFolder2 = folders.find { it.id == "folder2" }
        
        assertEquals(2, updatedFolder1?.appPackageNames?.size)
        assertEquals(1, unchangedFolder2?.appPackageNames?.size)
    }

    @Test
    fun updateFolderPosition_changesPositionValues() = testScope.runTest {
        // Given
        val folder = createTestFolder(id = "folder1", name = "Folder", x = 0f, y = 0f)
        folderManager.createFolder(0, folder)

        val newPosition = AppPosition(
            packageName = "folder1",
            x = 150f,
            y = 250f,
            iconSize = 96f
        )

        // When
        folderManager.updateFolderPosition(0, "folder1", newPosition)

        // Then
        val folders = folderManager.getFolders(0).first()
        assertEquals(150f, folders[0].position.x, 0.01f)
        assertEquals(250f, folders[0].position.y, 0.01f)
        assertEquals(96f, folders[0].position.iconSize, 0.01f)
    }

    @Test
    fun updateFolderPosition_onlyAffectsSpecifiedFolder() = testScope.runTest {
        // Given
        val folder1 = createTestFolder(id = "folder1", name = "F1", x = 10f, y = 10f)
        val folder2 = createTestFolder(id = "folder2", name = "F2", x = 20f, y = 20f)
        folderManager.createFolder(0, folder1)
        folderManager.createFolder(0, folder2)

        val newPosition = AppPosition(packageName = "folder1", x = 100f, y = 100f)

        // When
        folderManager.updateFolderPosition(0, "folder1", newPosition)

        // Then
        val folders = folderManager.getFolders(0).first()
        val updatedFolder1 = folders.find { it.id == "folder1" }
        val unchangedFolder2 = folders.find { it.id == "folder2" }
        
        assertNotNull(updatedFolder1)
        assertNotNull(unchangedFolder2)
        assertEquals(100f, updatedFolder1!!.position.x, 0.01f)
        assertEquals(20f, unchangedFolder2!!.position.x, 0.01f)
    }

    @Test
    fun updateFolderPosition_preservesOtherProperties() = testScope.runTest {
        // Given
        val folder = createTestFolder(
            id = "folder1",
            name = "Important Folder",
            appPackageNames = listOf("com.app1", "com.app2"),
            x = 0f,
            y = 0f
        )
        folderManager.createFolder(0, folder)

        val newPosition = AppPosition(packageName = "folder1", x = 200f, y = 300f)

        // When
        folderManager.updateFolderPosition(0, "folder1", newPosition)

        // Then
        val folders = folderManager.getFolders(0).first()
        assertEquals("Important Folder", folders[0].name)
        assertEquals(2, folders[0].appPackageNames.size)
    }

    @Test
    fun getFolders_returnsEmptyListInitially() = testScope.runTest {
        // When
        val folders = folderManager.getFolders(0).first()

        // Then
        assertTrue(folders.isEmpty())
    }

    @Test
    fun getFolders_returnsAllFoldersForPage() = testScope.runTest {
        // Given
        val folder1 = createTestFolder(id = "folder1", name = "F1")
        val folder2 = createTestFolder(id = "folder2", name = "F2")
        val folder3 = createTestFolder(id = "folder3", name = "F3")
        
        folderManager.createFolder(0, folder1)
        folderManager.createFolder(0, folder2)
        folderManager.createFolder(0, folder3)

        // When
        val folders = folderManager.getFolders(0).first()

        // Then
        assertEquals(3, folders.size)
    }

    @Test
    fun getFolders_emitsUpdatesWhenFoldersChange() = testScope.runTest {
        // Given
        val folder = createTestFolder(id = "folder1", name = "Test")

        // When/Then
        folderManager.getFolders(0).test {
            // Initial empty state
            assertEquals(0, awaitItem().size)

            // Add folder
            folderManager.createFolder(0, folder)
            assertEquals(1, awaitItem().size)

            // Delete folder
            folderManager.deleteFolder(0, "folder1")
            assertEquals(0, awaitItem().size)

            cancel()
        }
    }

    @Test
    fun getFolder_returnsMatchingFolder() = testScope.runTest {
        // Given
        val folder1 = createTestFolder(id = "folder1", name = "First")
        val folder2 = createTestFolder(id = "folder2", name = "Second")
        folderManager.createFolder(0, folder1)
        folderManager.createFolder(0, folder2)

        // When
        val result = folderManager.getFolder(0, "folder2")

        // Then
        assertNotNull(result)
        assertEquals("folder2", result?.id)
        assertEquals("Second", result?.name)
    }

    @Test
    fun getFolder_returnsNullForNonExistentId() = testScope.runTest {
        // Given
        val folder = createTestFolder(id = "folder1", name = "Test")
        folderManager.createFolder(0, folder)

        // When
        val result = folderManager.getFolder(0, "nonexistent")

        // Then
        assertNull(result)
    }

    @Test
    fun getFolder_returnsNullForEmptyFolderList() = testScope.runTest {
        // When
        val result = folderManager.getFolder(0, "folder1")

        // Then
        assertNull(result)
    }

    @Test
    fun removeAppFromFolders_removesAppFromSingleFolder() = testScope.runTest {
        // Given
        val folder = createTestFolder(
            id = "folder1",
            name = "Folder",
            appPackageNames = listOf("com.app1", "com.app2", "com.app3")
        )
        folderManager.createFolder(0, folder)

        // When
        folderManager.removeAppFromFolders(0, "com.app2")

        // Then
        val folders = folderManager.getFolders(0).first()
        assertEquals(1, folders.size)
        assertEquals(2, folders[0].appPackageNames.size)
        assertFalse(folders[0].appPackageNames.contains("com.app2"))
        assertTrue(folders[0].appPackageNames.contains("com.app1"))
        assertTrue(folders[0].appPackageNames.contains("com.app3"))
    }

    @Test
    fun removeAppFromFolders_removesAppFromMultipleFolders() = testScope.runTest {
        // Given
        val folder1 = createTestFolder(
            id = "folder1",
            name = "F1",
            appPackageNames = listOf("com.app1", "com.shared")
        )
        val folder2 = createTestFolder(
            id = "folder2",
            name = "F2",
            appPackageNames = listOf("com.app2", "com.shared")
        )
        folderManager.createFolder(0, folder1)
        folderManager.createFolder(0, folder2)

        // When
        folderManager.removeAppFromFolders(0, "com.shared")

        // Then
        val folders = folderManager.getFolders(0).first()
        assertEquals(2, folders.size)
        assertFalse(folders.any { it.appPackageNames.contains("com.shared") })
    }

    @Test
    fun removeAppFromFolders_deletesFolderWhenItBecomesEmpty() = testScope.runTest {
        // Given
        val folder1 = createTestFolder(
            id = "folder1",
            name = "Keep",
            appPackageNames = listOf("com.app1", "com.app2")
        )
        val folder2 = createTestFolder(
            id = "folder2",
            name = "Delete",
            appPackageNames = listOf("com.app3")
        )
        folderManager.createFolder(0, folder1)
        folderManager.createFolder(0, folder2)

        // When
        folderManager.removeAppFromFolders(0, "com.app3")

        // Then
        val folders = folderManager.getFolders(0).first()
        assertEquals(1, folders.size)
        assertEquals("folder1", folders[0].id)
    }

    @Test
    fun removeAppFromFolders_handlesNonExistentPackageName() = testScope.runTest {
        // Given
        val folder = createTestFolder(
            id = "folder1",
            name = "Folder",
            appPackageNames = listOf("com.app1", "com.app2")
        )
        folderManager.createFolder(0, folder)

        // When
        folderManager.removeAppFromFolders(0, "com.nonexistent")

        // Then
        val folders = folderManager.getFolders(0).first()
        assertEquals(1, folders.size)
        assertEquals(2, folders[0].appPackageNames.size)
    }

    @Test
    fun differentTabTypes_maintainSeparateStorage() = testScope.runTest {
        // Given
        val appsFolder = createTestFolder(id = "apps", name = "Apps Folder")
        val widgetsFolder = createTestFolder(id = "widgets", name = "Widgets Folder")

        // When
        folderManager.createFolder(0, appsFolder, TAB_TYPE_APPS)
        folderManager.createFolder(0, widgetsFolder, TAB_TYPE_WIDGETS)

        // Then
        val appsFolders = folderManager.getFolders(0, TAB_TYPE_APPS).first()
        val widgetsFolders = folderManager.getFolders(0, TAB_TYPE_WIDGETS).first()

        assertEquals(1, appsFolders.size)
        assertEquals(1, widgetsFolders.size)
        assertEquals("apps", appsFolders[0].id)
        assertEquals("widgets", widgetsFolders[0].id)
    }

    @Test
    fun operationsOnOneTabType_doNotAffectAnother() = testScope.runTest {
        // Given
        val appsFolder = createTestFolder(id = "apps", name = "Apps")
        val widgetsFolder = createTestFolder(id = "widgets", name = "Widgets")
        
        folderManager.createFolder(0, appsFolder, TAB_TYPE_APPS)
        folderManager.createFolder(0, widgetsFolder, TAB_TYPE_WIDGETS)

        // When
        folderManager.deleteFolder(0, "apps", TAB_TYPE_APPS)

        // Then
        val appsFolders = folderManager.getFolders(0, TAB_TYPE_APPS).first()
        val widgetsFolders = folderManager.getFolders(0, TAB_TYPE_WIDGETS).first()

        assertTrue(appsFolders.isEmpty())
        assertEquals(1, widgetsFolders.size)
    }

    @Test
    fun differentPages_maintainSeparateStorage() = testScope.runTest {
        // Given
        val page0Folder = createTestFolder(id = "p0", name = "Page 0")
        val page1Folder = createTestFolder(id = "p1", name = "Page 1")
        val page2Folder = createTestFolder(id = "p2", name = "Page 2")

        // When
        folderManager.createFolder(0, page0Folder)
        folderManager.createFolder(1, page1Folder)
        folderManager.createFolder(2, page2Folder)

        // Then
        assertEquals(1, folderManager.getFolders(0).first().size)
        assertEquals(1, folderManager.getFolders(1).first().size)
        assertEquals(1, folderManager.getFolders(2).first().size)
    }

    @Test
    fun operationsOnOnePage_doNotAffectAnother() = testScope.runTest {
        // Given
        val folder0 = createTestFolder(id = "f0", name = "Page 0")
        val folder1 = createTestFolder(id = "f1", name = "Page 1")
        
        folderManager.createFolder(0, folder0)
        folderManager.createFolder(1, folder1)

        // When
        folderManager.deleteFolder(0, "f0")

        // Then
        assertTrue(folderManager.getFolders(0).first().isEmpty())
        assertEquals(1, folderManager.getFolders(1).first().size)
    }

    @Test
    fun folder_withEmptyName_isAllowed() = testScope.runTest {
        // Given
        val folder = createTestFolder(id = "folder1", name = "")

        // When
        folderManager.createFolder(0, folder)

        // Then
        val folders = folderManager.getFolders(0).first()
        assertEquals(1, folders.size)
        assertEquals("", folders[0].name)
    }

    @Test
    fun folder_withSpecialCharactersInName_worksCorrectly() = testScope.runTest {
        // Given
        val folder = createTestFolder(
            id = "folder1",
            name = "Test!@#$%^&*()_+-=[]{}|;':\",./<>?"
        )

        // When
        folderManager.createFolder(0, folder)

        // Then
        val folders = folderManager.getFolders(0).first()
        assertEquals("Test!@#$%^&*()_+-=[]{}|;':\",./<>?", folders[0].name)
    }

    @Test
    fun folder_withUnicodeCharactersInName_worksCorrectly() = testScope.runTest {
        // Given
        val folder = createTestFolder(
            id = "folder1",
            name = "测试文件夹 🎉 ñoño"
        )

        // When
        folderManager.createFolder(0, folder)

        // Then
        val folders = folderManager.getFolders(0).first()
        assertEquals("测试文件夹 🎉 ñoño", folders[0].name)
    }

    @Test
    fun folder_withManyApps_worksCorrectly() = testScope.runTest {
        // Given
        val manyApps = (1..50).map { "com.app$it" }
        val folder = createTestFolder(
            id = "folder1",
            name = "Many Apps",
            appPackageNames = manyApps
        )

        // When
        folderManager.createFolder(0, folder)

        // Then
        val folders = folderManager.getFolders(0).first()
        assertEquals(50, folders[0].appPackageNames.size)
    }

    private fun createTestFolder(
        id: String,
        name: String,
        appPackageNames: List<String> = listOf("com.default.app"),
        x: Float = 0f,
        y: Float = 0f,
        iconSize: Float = 64f
    ): Folder {
        return Folder(
            id = id,
            name = name,
            appPackageNames = appPackageNames,
            position = AppPosition(
                packageName = id,
                x = x,
                y = y,
                iconSize = iconSize
            )
        )
    }
}
