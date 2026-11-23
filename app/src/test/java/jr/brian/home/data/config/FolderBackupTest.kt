package jr.brian.home.data.config

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import jr.brian.frontend.library.art.ArtSourceStore
import jr.brian.home.data.ExperienceStore
import jr.brian.home.data.FolderManager
import jr.brian.home.data.ManagerContainer
import jr.brian.home.data.ScreenAssignmentStore
import jr.brian.home.data.folderDataStore
import jr.brian.home.esde.model.ESDEPrefsState
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import jr.brian.jarnfe.backup.JarnBackupArea
import jr.brian.jarnfe.backup.JarnBackupChange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Drives real folder storage through the export rules and the real import path. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class FolderBackupTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val folderManager = FolderManager(context)
    private val managers = mockk<ManagerContainer>(relaxed = true)
    private lateinit var importExport: ImportExportManager

    @Before
    fun setup() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context.folderDataStore.edit { it.clear() }
        every { managers.app.folderManager } returns folderManager
        every { managers.ui.customAppNameManager.customNames } returns MutableStateFlow(emptyMap())
        every { managers.feature.esdePreferencesManager._state } returns MutableStateFlow(ESDEPrefsState())
        importExport = ImportExportManager(
            managers = managers,
            screenAssignmentStore = mockk<ScreenAssignmentStore>(relaxed = true),
            experienceStore = ExperienceStore(context),
            artSourceStore = ArtSourceStore(context),
            libraryFolderBackup = mockk(relaxed = true),
            jarnLookStore = mockk(relaxed = true),
            artChoiceStore = jr.brian.frontend.library.art.GameArtChoiceStore(context),
            appPinStore = jr.brian.frontend.apps.JarnAppPinStore(context),
            playStatsBackup = io.mockk.mockk(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `readable pages round trip`() = runBlocking {
        folderManager.setAllFolders(1, FolderManager.TAB_TYPE_WIDGETS, listOf(folder("kept")))
        val backup = exportFolders()
        folderManager.setAllFolders(1, FolderManager.TAB_TYPE_WIDGETS, listOf(folder("changed")))

        restore(backup)

        assertEquals("kept", stored(1, FolderManager.TAB_TYPE_WIDGETS).single().id)
    }

    @Test
    fun `a page that was empty at export restores as empty`() = runBlocking {
        val backup = exportFolders()
        folderManager.setAllFolders(2, FolderManager.TAB_TYPE_APPS, listOf(folder("added later")))

        restore(backup)

        assertTrue(stored(2, FolderManager.TAB_TYPE_APPS).isEmpty())
    }

    @Test
    fun `unreadable bytes survive export then import`() = runBlocking {
        writeRaw(APPS_PAGE_0, DAMAGED)
        val backup = exportFolders()

        assertFalse("folders_page_0" in backup)
        assertFalse("apps_0" in backup)
        assertEquals(1, omittedFolderPageCount(backup))

        restore(backup)

        assertEquals(DAMAGED, readRaw(APPS_PAGE_0))
    }

    @Test
    fun `a page unreadable only at export keeps its current folders`() = runBlocking {
        writeRaw(APPS_PAGE_0, DAMAGED)
        val backup = exportFolders()
        folderManager.setAllFolders(0, FolderManager.TAB_TYPE_APPS, listOf(folder("repaired")))

        restore(backup)

        assertEquals("repaired", stored(0, FolderManager.TAB_TYPE_APPS).single().id)
    }

    @Test
    fun `an empty page in a backup does not erase bytes unreadable here`() = runBlocking {
        val backup = exportFolders()
        writeRaw(APPS_PAGE_0, DAMAGED)

        restore(backup)

        assertEquals(DAMAGED, readRaw(APPS_PAGE_0))
    }

    @Test
    fun `real folders in a backup repair an unreadable page`() = runBlocking {
        folderManager.setAllFolders(0, FolderManager.TAB_TYPE_APPS, listOf(folder("good")))
        val backup = exportFolders()
        writeRaw(APPS_PAGE_0, DAMAGED)

        restore(backup)

        assertEquals("good", stored(0, FolderManager.TAB_TYPE_APPS).single().id)
    }

    @Test
    fun `an old backup still replaces current folders`() = runBlocking {
        folderManager.setAllFolders(0, FolderManager.TAB_TYPE_APPS, listOf(folder("current")))
        folderManager.setAllFolders(3, FolderManager.TAB_TYPE_WIDGETS, listOf(folder("stale")))

        restore(mapOf("apps_0" to listOf(item("from backup"))))

        assertEquals("from backup", stored(0, FolderManager.TAB_TYPE_APPS).single().id)
        assertTrue(stored(3, FolderManager.TAB_TYPE_WIDGETS).isEmpty())
    }

    @Test
    fun `an old backup still leaves unreadable bytes alone`() = runBlocking {
        writeRaw(APPS_PAGE_0, DAMAGED)

        restore(mapOf("widgets_1" to listOf(item("from backup"))))

        assertEquals(DAMAGED, readRaw(APPS_PAGE_0))
    }

    @Test
    fun `older builds can skip the marker`() {
        assertFalse(FOLDER_BACKUP_EXPLICIT_PAGES_KEY.contains('_'))
    }

    @Test
    fun `an unreadable page reads as null and a missing page as empty`() = runBlocking {
        writeRaw(APPS_PAGE_0, DAMAGED)

        assertNull(folderManager.readFolders(0, FolderManager.TAB_TYPE_APPS))
        assertEquals(emptyList<Folder>(), folderManager.readFolders(1, FolderManager.TAB_TYPE_APPS))
    }

    @Test
    fun `preview shows the folders a restore would really leave`() {
        val current = buildFolderBackup { page, tabType ->
            if (page == 0 && tabType == FolderManager.TAB_TYPE_APPS) listOf(item("kept")) else emptyList()
        }
        val partial = mapOf(FOLDER_BACKUP_EXPLICIT_PAGES_KEY to emptyList<FolderItemConfig>())

        val keeps = folderPreview(partial, current)
        assertEquals(JarnBackupChange.Same, keeps.change)
        assertEquals(1, keeps.backupCount)

        val clears = folderPreview(mapOf("widgets_1" to listOf(item("new"))), current)
        assertEquals(JarnBackupChange.Replaces, clears.change)
        assertEquals(1, clears.backupCount)
        assertEquals(1, clears.currentCount)
    }

    @Test
    fun `identical exports preview as unchanged against an old style copy`() {
        val current = buildFolderBackup { page, tabType ->
            if (page == 0 && tabType == FolderManager.TAB_TYPE_APPS) listOf(item("same")) else emptyList()
        }
        val oldStyle = mapOf("apps_0" to listOf(item("same")))

        assertEquals(JarnBackupChange.Same, folderPreview(oldStyle, current).change)
    }

    private fun folderPreview(
        backup: Map<String, List<FolderItemConfig>>,
        current: Map<String, List<FolderItemConfig>>,
    ) = buildBackupPreview(configWith(backup), configWith(current))
        .items.first { it.area == JarnBackupArea.Folders }

    private fun configWith(folders: Map<String, List<FolderItemConfig>>) =
        JarngreiprConfig(app = AppConfig(folders = folders))

    private suspend fun exportFolders(): Map<String, List<FolderItemConfig>> =
        buildFolderBackup { page, tabType ->
            folderManager.readFolders(page, tabType)?.map { it.toFolderItemConfig() }
        }

    private suspend fun restore(folders: Map<String, List<FolderItemConfig>>) {
        importExport.applyConfig(configWith(folders))
    }

    private suspend fun stored(page: Int, tabType: String) =
        folderManager.getFolders(page, tabType).first()

    private suspend fun writeRaw(key: String, value: String) {
        context.folderDataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    private suspend fun readRaw(key: String): String? =
        context.folderDataStore.data.first()[stringPreferencesKey(key)]

    private fun folder(id: String) = Folder(
        id = id,
        name = id,
        appPackageNames = listOf("app"),
        position = AppPosition(id, 1f, 2f, 64f),
    )

    private fun item(id: String) = FolderItemConfig(id, id, listOf("app"), 1f, 2f)

    private companion object {
        const val APPS_PAGE_0 = "folders_page_0"
        const val DAMAGED = "{not valid folders"
    }
}
