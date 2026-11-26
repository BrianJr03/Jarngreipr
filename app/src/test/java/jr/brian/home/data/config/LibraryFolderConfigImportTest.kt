package jr.brian.home.data.config

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import jr.brian.frontend.library.art.ArtSourceStore
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.LibraryDatabase
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.db.SystemEntity
import jr.brian.frontend.library.setup.LibraryFolderBackup
import jr.brian.home.data.ExperienceStore
import jr.brian.home.data.ManagerContainer
import jr.brian.home.data.ScreenAssignmentStore
import jr.brian.home.esde.model.ESDEPrefsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import java.io.ByteArrayInputStream

/** Drives the real import and export path against a real Room library. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class LibraryFolderConfigImportTest {
    private val managers = mockk<ManagerContainer>(relaxed = true)
    private lateinit var db: LibraryDatabase
    private lateinit var folderBackup: LibraryFolderBackup
    private lateinit var importExport: ImportExportManager

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { managers.ui.customAppNameManager.customNames } returns MutableStateFlow(emptyMap())
        every { managers.feature.esdePreferencesManager._state } returns MutableStateFlow(ESDEPrefsState())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        folderBackup = LibraryFolderBackup(context, db.sourceDao())
        importExport = ImportExportManager(
            managers = managers,
            screenAssignmentStore = mockk<ScreenAssignmentStore>(relaxed = true),
            experienceStore = ExperienceStore(context),
            artSourceStore = ArtSourceStore(context),
            libraryFolderBackup = folderBackup,
            jarnLookStore = mockk(relaxed = true),
            artChoiceStore = jr.brian.frontend.library.art.GameArtChoiceStore(ApplicationProvider.getApplicationContext()),
            appPinStore = jr.brian.frontend.apps.JarnAppPinStore(ApplicationProvider.getApplicationContext()),
            playStatsBackup = io.mockk.mockk(relaxed = true),
            backupExtras = testBackupExtras(),
            jarnPreviewStore = io.mockk.mockk(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `schema version includes library folders`() {
        assertEquals(72, JarngreiprConfig.CONFIG_VERSION)
    }

    @Test
    fun `folders round trip through a real export and import`() = runBlocking {
        db.sourceDao().insert(SourceEntity(treeUri = ROMS, displayPath = "Roms", volumeId = "primary"))
        val folders = folderBackup.export()!!.map { it.toConfig() }
        val exported = importExport.encodeToJson(
            JarngreiprConfig(feature = FeatureConfig(libraryFolders = folders)),
        )
        db.clearAllTables()

        assertImportOk(exported)

        val restored = db.sourceDao().findByTreeUri(ROMS)!!
        assertEquals("Roms", restored.displayPath)
        assertEquals("primary", restored.volumeId)
        assertFalse("restored without a grant must need reconnecting", restored.hasPersistedGrant)
    }

    @Test
    fun `an older backup leaves current folders, games, and favorites alone`() = runBlocking {
        val gameId = seedFavorite()
        val before = db.sourceDao().getAll()

        assertImportOk(V39_BACKUP)

        assertEquals(before, db.sourceDao().getAll())
        assertTrue(db.gameDao().findById(gameId)!!.isFavorite)
        assertNull(importExport.decodeFromJson(V39_BACKUP).feature.libraryFolders)
    }

    @Test
    fun `importing keeps games and favorites and adds no duplicate`() = runBlocking {
        val gameId = seedFavorite()
        val before = db.sourceDao().findByTreeUri(ROMS)

        assertImportOk(backupWith(ROMS, SD))
        assertImportOk(backupWith(ROMS, SD))

        assertEquals(2, db.sourceDao().getAll().size)
        assertEquals(before, db.sourceDao().findByTreeUri(ROMS))
        assertTrue(db.gameDao().findById(gameId)!!.isFavorite)
        assertFalse(db.sourceDao().findByTreeUri(SD)!!.hasPersistedGrant)
    }

    @Test
    fun `an entry with no uri does not fail the import`() = runBlocking {
        assertImportOk("""{ "version": 40, "feature": { "libraryFolders": [ { "displayPath": "x" } ] } }""")

        assertTrue(db.sourceDao().getAll().isEmpty())
    }

    private suspend fun seedFavorite(): Long {
        val sourceId = db.sourceDao().insert(SourceEntity(treeUri = ROMS, displayPath = "Roms"))
        db.systemDao().upsertAll(listOf(SystemEntity(id = "gba", displayName = "GBA")))
        return db.gameDao().insert(
            GameEntity(sourceId = sourceId, systemId = "gba", relativePath = "a.gba", displayName = "A", isFavorite = true),
        )
    }

    private suspend fun assertImportOk(backup: String) {
        val result = importExport.importFromStream(ByteArrayInputStream(backup.toByteArray()))
        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
    }

    private fun backupWith(vararg uris: String): String {
        val folders = uris.joinToString { """{ "treeUri": "$it", "displayPath": "Folder" }""" }
        return """{ "version": 40, "feature": { "libraryFolders": [ $folders ] } }"""
    }

    private companion object {
        const val ROMS = "content://com.android.externalstorage.documents/tree/primary%3AROMs"
        const val SD = "content://com.android.externalstorage.documents/tree/3A0D-5000%3AGames"
        const val V39_BACKUP = """{ "version": 39, "feature": { "artSource": "EsDe" } }"""
    }
}
