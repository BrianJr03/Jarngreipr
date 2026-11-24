package jr.brian.home.data.config

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import jr.brian.frontend.apps.JarnAppPinStore
import jr.brian.frontend.library.art.ArtSourceStore
import jr.brian.frontend.library.art.GameArtChoiceStore
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.LibraryDatabase
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.db.SystemEntity
import jr.brian.frontend.library.play.PlayStatsBackup
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
class JarnPlayStatsConfigImportTest {
    private val managers = mockk<ManagerContainer>(relaxed = true)
    private lateinit var db: LibraryDatabase
    private lateinit var stats: PlayStatsBackup
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
        stats = PlayStatsBackup(context, db.playHistoryDao())
        importExport = ImportExportManager(
            managers = managers,
            screenAssignmentStore = mockk<ScreenAssignmentStore>(relaxed = true),
            experienceStore = ExperienceStore(context),
            artSourceStore = ArtSourceStore(context),
            libraryFolderBackup = LibraryFolderBackup(context, db.sourceDao()),
            jarnLookStore = mockk(relaxed = true),
            artChoiceStore = GameArtChoiceStore(context),
            appPinStore = JarnAppPinStore(context),
            playStatsBackup = stats,
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
    fun `schema version includes play stats`() {
        assertEquals(59, JarngreiprConfig.CONFIG_VERSION)
    }

    @Test
    fun `totals round trip through a real export and import`() = runBlocking {
        val id = seedGame(playCount = 7, minutes = 90)
        val exported = importExport.encodeToJson(
            JarngreiprConfig(
                feature = FeatureConfig(jarnPlayStats = stats.export()!!.map { it.toConfig() }),
            ),
        )
        db.gameDao().update(db.gameDao().findById(id)!!.copy(playCount = 0, playTimeMinutes = 0))

        assertImportOk(exported)

        assertEquals(7, db.gameDao().findById(id)!!.playCount)
        assertEquals(90, db.gameDao().findById(id)!!.playTimeMinutes)
    }

    @Test
    fun `an older backup leaves current totals alone`() = runBlocking {
        val id = seedGame(playCount = 7, minutes = 90)

        assertImportOk(V47_BACKUP)

        assertEquals(7, db.gameDao().findById(id)!!.playCount)
        assertEquals(90, db.gameDao().findById(id)!!.playTimeMinutes)
        assertNull(importExport.decodeFromJson(V47_BACKUP).feature.jarnPlayStats)
    }

    @Test
    fun `a backup with lower totals never lowers them`() = runBlocking {
        val id = seedGame(playCount = 7, minutes = 90)

        assertImportOk(backupWith(playCount = 2, minutes = 10))

        assertEquals(7, db.gameDao().findById(id)!!.playCount)
        assertEquals(90, db.gameDao().findById(id)!!.playTimeMinutes)
    }

    private suspend fun seedGame(playCount: Int, minutes: Int): Long {
        val sourceId = db.sourceDao().insert(SourceEntity(treeUri = "tree", displayPath = "Roms"))
        db.systemDao().upsertAll(listOf(SystemEntity(id = "gba", displayName = "GBA")))
        return db.gameDao().insert(
            GameEntity(
                sourceId = sourceId,
                systemId = "gba",
                relativePath = "a.gba",
                displayName = "A",
                playCount = playCount,
                playTimeMinutes = minutes,
            ),
        )
    }

    private suspend fun assertImportOk(backup: String) {
        val result = importExport.importFromStream(ByteArrayInputStream(backup.toByteArray()))
        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
    }

    private fun backupWith(playCount: Int, minutes: Int) =
        """{ "version": 48, "feature": { "jarnPlayStats": [ { "systemId": "gba",
            "relativePath": "a.gba", "playCount": $playCount, "playTimeMinutes": $minutes } ] } }"""

    private companion object {
        const val V47_BACKUP = """{ "version": 47, "feature": { "artSource": "EsDe" } }"""
    }
}
