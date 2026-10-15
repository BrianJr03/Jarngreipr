package jr.brian.frontend.library.migrate

import android.app.Application
import android.content.Context
import androidx.room.Room
import jr.brian.frontend.library.LibraryRepository
import jr.brian.frontend.library.db.LibraryDatabase
import jr.brian.frontend.library.scan.FastWalkScanner
import jr.brian.frontend.library.scan.SafTreeScanner
import jr.brian.frontend.library.scan.ScannerDispatcher
import jr.brian.frontend.library.scan.StubSystemCatalog
import jr.brian.frontend.library.systems.SystemDef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * End-to-end Phase 2d migration from legacy esde_prefs into Room.
 *
 * The migration must be:
 *  - idempotent (safe to call on every launch),
 *  - produce one SourceEntity per unique treeUri regardless of
 *    which legacy slot it came from,
 *  - populate SystemEntity from the catalog so later FK writes
 *    always resolve.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LibraryMigrationTest {

    private val catalog = StubSystemCatalog(
        SystemDef(id = "nes", displayName = "Nintendo", extensions = setOf("nes")),
        SystemDef(id = "psx", displayName = "PlayStation", extensions = setOf("iso")),
    )

    private lateinit var app: Application
    private lateinit var db: LibraryDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var repo: LibraryRepository
    private lateinit var migration: LibraryMigration

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        app.getSharedPreferences("esde_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("library_migration_prefs", Context.MODE_PRIVATE).edit().clear().commit()

        db = Room.inMemoryDatabaseBuilder(app, LibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        repo = LibraryRepository(
            systemDao = db.systemDao(),
            sourceDao = db.sourceDao(),
            gameDao = db.gameDao(),
            mediaDao = db.mediaDao(),
            scrapeJobDao = db.scrapeJobDao(),
            playHistoryDao = db.playHistoryDao(),
            catalog = catalog,
            scanner = ScannerDispatcher(
                fastWalk = FastWalkScanner(catalog),
                saf = SafTreeScanner(app, catalog),
            ),
            scope = scope,
        )
        val legacyPrefs = EsdeLegacyPrefs(app)
        migration = LibraryMigration(
            context = app,
            legacyPrefs = legacyPrefs,
            repository = repo,
            sourceDao = db.sourceDao(),
            systemDao = db.systemDao(),
            catalog = catalog,
            romIndexImporter = RomIndexImporter(
                context = app,
                legacyPrefs = legacyPrefs,
                sourceDao = db.sourceDao(),
                gameDao = db.gameDao(),
                mediaDao = db.mediaDao(),
                catalog = catalog,
            ),
            gamelistImporter = GamelistImporter(db.gameDao()),
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
    }

    @Test
    fun `migration upserts SystemEntity rows from the catalog`() = runBlocking {
        migration.runOnce()

        val ids = db.systemDao().observeAll().first().map { it.id }.toSet()
        assertEquals(setOf("nes", "psx"), ids)
    }

    @Test
    fun `migration converts roms_paths into SourceEntity rows`() = runBlocking {
        write("roms_paths", """["/storage/emulated/0/Roms","/sdcard/Roms"]""")

        migration.runOnce()

        val sources = db.sourceDao().observeAll().first().map { it.treeUri }.toSet()
        assertEquals(setOf("/storage/emulated/0/Roms", "/sdcard/Roms"), sources)
    }

    @Test
    fun `migration converts saf_tree_uris into SourceEntity rows`() = runBlocking {
        write(
            "saf_tree_uris",
            """{"psx":"content://com.android.externalstorage.documents/tree/primary%3ARoms%2Fpsx"}""",
        )

        migration.runOnce()

        val sources = db.sourceDao().observeAll().first()
        assertEquals(1, sources.size)
        assertTrue(sources.single().treeUri.startsWith("content://"))
    }

    @Test
    fun `migration converts system_folder_mappings into SourceEntity rows`() = runBlocking {
        write(
            "system_folder_mappings",
            """[{"systemName":"ps2","treeUri":"content://foo","displayPath":"SD:/MyGames"}]""",
        )

        migration.runOnce()

        val sources = db.sourceDao().observeAll().first()
        assertEquals(1, sources.size)
        assertEquals("content://foo", sources.single().treeUri)
        assertEquals("SD:/MyGames", sources.single().displayPath)
    }

    @Test
    fun `migration is idempotent — a second runOnce writes nothing new`() = runBlocking {
        write("roms_paths", """["/A"]""")

        migration.runOnce()
        val afterFirst = db.sourceDao().observeAll().first()

        // Even if the user adds a path after migration completes, a
        // second runOnce should NOT re-import because the version
        // marker is now current.
        write("roms_paths", """["/A","/B"]""")
        migration.runOnce()

        val afterSecond = db.sourceDao().observeAll().first()
        assertEquals(afterFirst, afterSecond)
    }

    @Test
    fun `resetForTesting lets a second runOnce reprocess legacy state`() = runBlocking {
        write("roms_paths", """["/A"]""")

        migration.runOnce()
        write("roms_paths", """["/A","/B"]""")

        migration.resetForTesting()
        migration.runOnce()

        val sources = db.sourceDao().observeAll().first().map { it.treeUri }.toSet()
        assertEquals(setOf("/A", "/B"), sources)
    }

    @Test
    fun `migration with no legacy prefs still populates systems`() = runBlocking {
        migration.runOnce()

        assertTrue(db.sourceDao().observeAll().first().isEmpty())
        assertTrue(db.systemDao().observeAll().first().isNotEmpty())
    }

    @Test
    fun `migration deduplicates when the same URI appears in multiple legacy slots`() = runBlocking {
        write("roms_paths", """["content://foo"]""")
        write("saf_tree_uris", """{"psx":"content://foo"}""")

        migration.runOnce()

        val sources = db.sourceDao().observeAll().first()
        assertEquals(1, sources.size)
    }

    private fun write(key: String, value: String) {
        app.getSharedPreferences("esde_prefs", Context.MODE_PRIVATE)
            .edit().putString(key, value).commit()
    }
}
