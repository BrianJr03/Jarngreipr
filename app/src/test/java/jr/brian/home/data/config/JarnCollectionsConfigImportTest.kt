package jr.brian.home.data.config

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import jr.brian.frontend.library.collections.CollectionNameResult
import jr.brian.frontend.library.collections.CollectionsBackup
import jr.brian.frontend.library.collections.CollectionsRepository
import jr.brian.frontend.library.db.LibraryDatabase
import jr.brian.frontend.library.db.SystemEntity
import jr.brian.frontend.look.JarnLookStore
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

/** Drives the real export and import path against a real library database. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class JarnCollectionsConfigImportTest {
    private val managers = mockk<ManagerContainer>(relaxed = true)
    private lateinit var db: LibraryDatabase
    private lateinit var repository: CollectionsRepository
    private lateinit var importExport: ImportExportManager

    /** The export path for these areas. The full build needs managers this test doesn't fake. */
    private lateinit var extras: SettingsBackupExtras

    @Before
    fun setup() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { managers.ui.customAppNameManager.customNames } returns MutableStateFlow(emptyMap())
        every { managers.feature.esdePreferencesManager._state } returns MutableStateFlow(ESDEPrefsState())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LibraryDatabase::class.java).allowMainThreadQueries().build()
        repository = CollectionsRepository(db.collectionDao())
        extras = testBackupExtras(collections = CollectionsBackup(repository))
        importExport = ImportExportManager(
            managers = managers,
            screenAssignmentStore = mockk<ScreenAssignmentStore>(relaxed = true),
            experienceStore = ExperienceStore(context),
            artSourceStore = mockk(relaxed = true),
            libraryFolderBackup = mockk(relaxed = true),
            jarnLookStore = JarnLookStore(context),
            artChoiceStore = jr.brian.frontend.library.art.GameArtChoiceStore(context),
            appPinStore = jr.brian.frontend.apps.JarnAppPinStore(context),
            playStatsBackup = mockk(relaxed = true),
            backupExtras = extras,
            jarnPreviewStore = mockk(relaxed = true),
        )
        db.systemDao().upsert(SystemEntity(id = "snes", displayName = "SNES"))
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `schema version covers collections`() {
        assertEquals(72, JarngreiprConfig.CONFIG_VERSION)
    }

    @Test
    fun `collections round trip through the manager json`() = runBlocking {
        val rpg = create("RPGs")
        repository.addGame(rpg, "snes", "Chrono.sfc")

        val decoded = importExport.decodeFromJson(importExport.encodeToJson(extras.addTo(JarngreiprConfig()))).feature

        assertEquals(
            listOf(JarnCollectionConfig("RPGs", 0, listOf(JarnGameKeyConfig("snes", "Chrono.sfc")))),
            decoded.jarnCollections,
        )
    }

    @Test
    fun `an older backup leaves the device's collections alone`() = runBlocking {
        val mine = create("Mine")
        repository.addGame(mine, "snes", "Mine.sfc")
        assertNull(importExport.decodeFromJson(V71_BACKUP).feature.jarnCollections)

        restore(V71_BACKUP)

        val after = extras.addTo(JarngreiprConfig()).feature
        assertEquals(listOf("Mine"), after.jarnCollections?.map { it.name })
    }

    @Test
    fun `a restore merges into a same name collection and adds the rest`() = runBlocking {
        val mine = create("RPGs")
        repository.addGame(mine, "snes", "Mine.sfc")

        restore(
            """{ "version": 72, "feature": { "jarnCollections": [
                { "name": "rpgs", "position": 0, "members": [{ "systemId": "snes", "relativePath": "Chrono.sfc" }] },
                { "name": "Party", "position": 1, "members": [] }
            ] } }""",
        )

        val after = extras.addTo(JarngreiprConfig()).feature.jarnCollections.orEmpty()
        assertEquals(listOf("RPGs", "Party"), after.map { it.name })
        assertEquals(
            setOf(JarnGameKeyConfig("snes", "Mine.sfc"), JarnGameKeyConfig("snes", "Chrono.sfc")),
            after.first().members.toSet(),
        )
    }

    @Test
    fun `an empty collections list keeps the device's collections`() = runBlocking {
        create("Mine")

        restore("""{ "version": 72, "feature": { "jarnCollections": [] } }""")

        assertEquals(listOf("Mine"), extras.addTo(JarngreiprConfig()).feature.jarnCollections?.map { it.name })
    }

    private suspend fun restore(json: String) {
        val result = importExport.importFromStream(ByteArrayInputStream(json.toByteArray()))
        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
    }

    private suspend fun create(name: String): Long =
        (repository.create(name) as CollectionNameResult.Saved).collectionId

    private companion object {
        const val V71_BACKUP = """{ "version": 71, "feature": { "artSource": "EsDe" } }"""
    }
}
