package jr.brian.home.data.config

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import jr.brian.frontend.library.collections.CollectionNameResult
import jr.brian.frontend.library.collections.CollectionsBackup
import jr.brian.frontend.library.collections.CollectionsRepository
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.LibraryDatabase
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.db.SystemEntity
import jr.brian.frontend.library.favorites.FavoritesBackup
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
    private var sourceId = 0L

    @Before
    fun setup() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { managers.ui.customAppNameManager.customNames } returns MutableStateFlow(emptyMap())
        every { managers.feature.esdePreferencesManager._state } returns MutableStateFlow(ESDEPrefsState())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LibraryDatabase::class.java).allowMainThreadQueries().build()
        repository = CollectionsRepository(db.collectionDao())
        extras = testBackupExtras(
            collections = CollectionsBackup(repository),
            favorites = FavoritesBackup(context, db.gameDao()),
        )
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
        sourceId = db.sourceDao().insert(SourceEntity(treeUri = "/old/roms", displayPath = "/old/roms"))
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `schema version covers collections and favorites`() {
        assertEquals(76, JarngreiprConfig.CONFIG_VERSION)
    }

    @Test
    fun `collections and favorites round trip through the manager json`() = runBlocking {
        val rpg = create("RPGs")
        repository.addGame(rpg, "snes", "Chrono.sfc")
        insert("Mario.sfc", favorite = true)

        val decoded = importExport.decodeFromJson(importExport.encodeToJson(extras.addTo(JarngreiprConfig()))).feature

        assertEquals(
            listOf(JarnCollectionConfig("RPGs", 0, listOf(JarnGameKeyConfig("snes", "Chrono.sfc")))),
            decoded.jarnCollections,
        )
        assertEquals(listOf(JarnGameKeyConfig("snes", "Mario.sfc")), decoded.jarnFavorites)
    }

    @Test
    fun `an older backup leaves the device's collections and favorites alone`() = runBlocking {
        val mine = create("Mine")
        repository.addGame(mine, "snes", "Mine.sfc")
        insert("Fav.sfc", favorite = true)
        assertNull(importExport.decodeFromJson(V71_BACKUP).feature.jarnCollections)

        restore(V71_BACKUP)

        val after = extras.addTo(JarngreiprConfig()).feature
        assertEquals(listOf("Mine"), after.jarnCollections?.map { it.name })
        assertEquals(listOf(JarnGameKeyConfig("snes", "Fav.sfc")), after.jarnFavorites)
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

    @Test
    fun `favorites restore onto different ROM roots, now or after the scan`() = runBlocking {
        val newCard = db.sourceDao().insert(SourceEntity(treeUri = "/new/card", displayPath = "/new/card"))
        insert("Now.sfc", source = newCard)

        restore(
            """{ "version": 72, "feature": { "jarnFavorites": [
                { "systemId": "snes", "relativePath": "Now.sfc" },
                { "systemId": "snes", "relativePath": "Later.sfc" }
            ] } }""",
        )

        assertEquals(listOf("Now.sfc"), db.gameDao().favoriteKeys().map { it.relativePath })
        val favorites = FavoritesBackup(ApplicationProvider.getApplicationContext(), db.gameDao())
        insert("Later.sfc", source = newCard)
        favorites.applyPending()
        assertEquals(setOf("Now.sfc", "Later.sfc"), db.gameDao().favoriteKeys().map { it.relativePath }.toSet())
    }

    private suspend fun restore(json: String) {
        val result = importExport.importFromStream(ByteArrayInputStream(json.toByteArray()))
        assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)
    }

    private suspend fun create(name: String): Long =
        (repository.create(name) as CollectionNameResult.Saved).collectionId

    private suspend fun insert(path: String, favorite: Boolean = false, source: Long = sourceId) {
        db.gameDao().insert(
            GameEntity(sourceId = source, systemId = "snes", relativePath = path, displayName = path, isFavorite = favorite),
        )
    }

    private companion object {
        const val V71_BACKUP = """{ "version": 71, "feature": { "artSource": "EsDe" } }"""
    }
}
