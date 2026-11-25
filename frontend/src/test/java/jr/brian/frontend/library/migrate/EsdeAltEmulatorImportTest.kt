package jr.brian.frontend.library.migrate

import android.app.Application
import androidx.room.Room
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.LibraryDatabase
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.db.SystemEntity
import jr.brian.frontend.library.scan.StubSystemCatalog
import jr.brian.frontend.library.setup.EsdeRomsFolderFinder
import jr.brian.frontend.library.systems.SystemDef
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

private val SnesGamelist = """
    <?xml version="1.0"?>
    <alternativeEmulator>
        <label>Snes9x - Current</label>
    </alternativeEmulator>
    <gameList>
        <game>
            <path>./Mario World.zip</path>
            <name>Super Mario World</name>
            <altemulator>bsnes</altemulator>
        </game>
        <game>
            <path>./sub/Zelda.sfc</path>
            <altemulator>Snes9x EX+ (Standalone)</altemulator>
        </game>
        <game>
            <path>./No Choice.sfc</path>
        </game>
        <game>
            <path>./Gone.sfc</path>
            <altemulator>bsnes</altemulator>
        </game>
    </gameList>
""".trimIndent()

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EsdeAltEmulatorImportTest {
    @get:Rule val temp = TemporaryFolder()

    private lateinit var db: LibraryDatabase
    private var written: EsdeAltEmulators? = null
    private lateinit var import: EsdeAltEmulatorImport

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), LibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val catalog = StubSystemCatalog(SystemDef(id = "snes", displayName = "SNES", folderAliases = listOf("sfc")))
        val finder = EsdeRomsFolderFinder({ null }, { emptyList() }, { null }, { false })
        import = EsdeAltEmulatorImport(finder, db.sourceDao(), db.gameDao(), catalog) { found ->
            written = found
            EsdeAltEmulatorResult(systemsSet = found.systems.size, gamesSet = found.games.size)
        }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `both elements are read, even with alternativeEmulator as a second root`() {
        val parsed = parseAltEmulators(SnesGamelist)
        assertEquals("Snes9x - Current", parsed.systemLabel)
        assertEquals(
            mapOf("Mario World.zip" to "bsnes", "sub/Zelda.sfc" to "Snes9x EX+ (Standalone)", "Gone.sfc" to "bsnes"),
            parsed.gameLabels,
        )
    }

    @Test
    fun `a gamelist without choices has none`() {
        val parsed = parseAltEmulators("<gameList><game><path>./a.sfc</path></game></gameList>")
        assertNull(parsed.systemLabel)
        assertTrue(parsed.gameLabels.isEmpty())
    }

    @Test
    fun `a byte order mark and an escaped label still parse`() {
        val parsed = parseAltEmulators(
            "\uFEFF<?xml version=\"1.0\"?><alternativeEmulator><label>A &amp; B</label></alternativeEmulator><gameList/>",
        )
        assertEquals("A & B", parsed.systemLabel)
    }

    @Test
    fun `games match the library and use the launch path's key`() = runBlocking {
        db.systemDao().upsert(SystemEntity(id = "snes", displayName = "SNES"))
        val fileSource = db.sourceDao().insert(SourceEntity(treeUri = "/storage/emulated/0/ROMs", displayPath = "ROMs"))
        val safSource = db.sourceDao().insert(SourceEntity(treeUri = "content://tree/x", displayPath = "SD"))
        db.gameDao().insert(
            GameEntity(
                sourceId = fileSource,
                systemId = "snes",
                relativePath = "Mario World.zip",
                absolutePath = "/storage/emulated/0/ROMs/snes/Mario World.zip",
                displayName = "Mario",
                emulatorPackage = "com.retroarch.aarch64",
            ),
        )
        db.gameDao().insert(GameEntity(sourceId = safSource, systemId = "snes", relativePath = "sub/Zelda.sfc", displayName = "Zelda"))
        db.gameDao().insert(GameEntity(sourceId = fileSource, systemId = "snes", relativePath = "No Choice.sfc", displayName = "None"))
        val gamelist = File(temp.newFolder("gamelists", "snes"), "gamelist.xml").apply { writeText(SnesGamelist) }

        val result = import.importFrom(
            listOf(GamelistJob(gamelist, fileSource, "snes"), GamelistJob(gamelist, safSource, "snes")),
        )

        val found = written!!
        assertEquals(
            listOf(
                EsdeGameAltEmulator("snes//storage/emulated/0/ROMs/snes/Mario World.zip", "snes", "bsnes", "zip"),
                EsdeGameAltEmulator("snes/sub/Zelda.sfc", "snes", "Snes9x EX+ (Standalone)", "sfc"),
            ),
            found.games,
        )
        val system = found.systems.single()
        assertEquals("snes" to "Snes9x - Current", system.esdeSystem to system.label)
        assertEquals(3, system.games.size)
        assertEquals("com.retroarch.aarch64", system.games.first { it.key.endsWith("Mario World.zip") }.rowEmulator)
        assertEquals(1, result.gamesNotInLibrary)
        assertEquals(2, result.gamesSet)
    }

    @Test
    fun `nothing is written when no gamelist has a choice`() = runBlocking {
        val gamelist = File(temp.newFolder("gamelists", "snes"), "gamelist.xml").apply { writeText("<gameList/>") }
        val result = import.importFrom(listOf(GamelistJob(gamelist, 1, "snes")))
        assertNull(written)
        assertEquals(EsdeAltEmulatorResult(), result)
    }

    @Test
    fun `a broken gamelist is skipped`() = runBlocking {
        val gamelist = File(temp.newFolder("gamelists", "snes"), "gamelist.xml").apply { writeText("<gameList><game>") }
        assertEquals(EsdeAltEmulatorResult(), import.importFrom(listOf(GamelistJob(gamelist, 1, "snes"))))
    }
}
