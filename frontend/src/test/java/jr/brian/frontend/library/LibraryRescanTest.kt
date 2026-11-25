package jr.brian.frontend.library

import android.app.Application
import androidx.room.Room
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.LibraryDatabase
import jr.brian.frontend.library.db.MediaEntity
import jr.brian.frontend.library.db.PlayHistoryEntity
import jr.brian.frontend.library.migrate.GamelistImporter
import jr.brian.frontend.library.scan.FastWalkScanner
import jr.brian.frontend.library.scan.SafTreeScanner
import jr.brian.frontend.library.scan.ScanRules
import jr.brian.frontend.library.scan.ScannerDispatcher
import jr.brian.frontend.library.scan.StubSystemCatalog
import jr.brian.frontend.library.systems.SystemDef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

/** Rescans against a real Room database and real files (#279). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LibraryRescanTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val catalog = StubSystemCatalog(
        SystemDef(
            id = "nes",
            displayName = "Nintendo",
            folderAliases = listOf("famicom"),
            extensions = setOf("nes"),
            screenScraperSystemId = 3,
        ),
    )

    private lateinit var db: LibraryDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var repo: LibraryRepository
    private lateinit var roms: File

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication()
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
        roms = tempFolder.newFolder("Roms")
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
    }

    @Test
    fun `rescan keeps every user field and the row id`() = runBlocking {
        writeRom("Mario.nes")
        scan()
        val original = game("Mario.nes")
        val edited = original.withUserData()
        db.gameDao().update(edited)
        db.playHistoryDao().insert(PlayHistoryEntity(gameId = original.id, startedAt = 5L))

        scan()

        assertEquals(edited, game("Mario.nes"))
        assertEquals(1, db.playHistoryDao().observeForGame(original.id).first().size)
    }

    @Test
    fun `rescan refreshes size and clears hashes when the file changed`() = runBlocking {
        val rom = writeRom("Mario.nes")
        scan()
        val edited = game("Mario.nes").withUserData()
        db.gameDao().update(edited)
        rom.writeText("a much longer stub")

        scan()

        val after = game("Mario.nes")
        assertEquals(rom.length(), after.fileSizeBytes)
        assertNull(after.crc32)
        assertNull(after.md5)
        assertEquals(edited.copy(fileSizeBytes = rom.length(), crc32 = null, md5 = null), after)
    }

    @Test
    fun `new game is added and removed game is deleted`() = runBlocking {
        writeRom("Mario.nes")
        val zelda = writeRom("Zelda.nes")
        scan()
        val marioId = game("Mario.nes").id
        val zeldaId = game("Zelda.nes").id
        db.mediaDao().upsert(MediaEntity(gameId = zeldaId, coverPath = "/zelda.png"))
        zelda.delete()
        writeRom("Metroid.nes")

        scan()

        assertEquals(
            setOf("Mario.nes", "Metroid.nes"),
            db.gameDao().observeAll().first().map { it.relativePath }.toSet(),
        )
        assertEquals(marioId, game("Mario.nes").id)
        assertNull(db.mediaDao().findByGameId(zeldaId))
    }

    @Test
    fun `unmounted source keeps its games and user data`() = runBlocking {
        writeRom("Mario.nes")
        scan()
        val edited = game("Mario.nes").withUserData()
        db.gameDao().update(edited)
        val unmounted = File(roms.parentFile, "Unmounted")
        assertTrue(roms.renameTo(unmounted))

        scan()

        assertEquals(edited, game("Mario.nes"))
    }

    @Test
    fun `emptied source keeps its games`() = runBlocking {
        val rom = writeRom("Mario.nes")
        scan()
        val edited = game("Mario.nes").withUserData()
        db.gameDao().update(edited)
        rom.delete()

        scan()

        assertEquals(edited, game("Mario.nes"))
    }

    @Test
    fun `unreadable subfolder keeps its games while the rest updates`() = runBlocking {
        writeRom("Mario.nes")
        writeRom("sub/Hidden.nes")
        scan()
        val hidden = game("sub/Hidden.nes").withUserData()
        db.gameDao().update(hidden)
        val subfolder = File(roms, "nes/sub")
        assertTrue(subfolder.setReadable(false))
        try {
            writeRom("Metroid.nes")
            scan()
        } finally {
            subfolder.setReadable(true)
        }

        assertEquals(hidden, game("sub/Hidden.nes"))
        assertNotNull(findGame("Metroid.nes"))
    }

    @Test
    fun `shared storage listing is not trusted without all-files access`() {
        assertFalse(ScanRules.isListingTrusted("/storage/emulated/0/Roms", { false }))
        assertFalse(ScanRules.isListingTrusted("/storage/1234-ABCD/Roms", { false }))
        assertTrue(ScanRules.isListingTrusted("/storage/1234-ABCD/Roms", { true }))
        assertTrue(ScanRules.isListingTrusted("/data/user/0/app/Roms", { false }))
    }

    @Test
    fun `existing media row is updated in place with fresh paths`() = runBlocking {
        val mediaRoot = tempFolder.newFolder("ES-DE")
        writeRom("Mario.nes")
        val oldCover = writeFile(mediaRoot, "downloaded_media/nes/covers/Mario.png")
        repo.setMediaRoots(listOf(mediaRoot))
        scan()
        val game = game("Mario.nes")
        val before = db.mediaDao().findByGameId(game.id)!!
        oldCover.delete()
        writeFile(mediaRoot, "downloaded_media/nes/marquees/Mario.png")

        scan()

        val after = db.mediaDao().findByGameId(game.id)!!
        assertEquals(before.id, after.id)
        assertNull(after.coverPath)
        assertTrue(after.marqueePath?.endsWith("marquees/Mario.png") == true)
    }

    @Test
    fun `media row is removed when art is really gone`() = runBlocking {
        val mediaRoot = tempFolder.newFolder("ES-DE")
        writeRom("Mario.nes")
        val cover = writeFile(mediaRoot, "downloaded_media/nes/covers/Mario.png")
        repo.setMediaRoots(listOf(mediaRoot))
        scan()
        val id = game("Mario.nes").id
        assertNotNull(db.mediaDao().findByGameId(id))
        cover.delete()

        scan()

        assertNull(db.mediaDao().findByGameId(id))
    }

    @Test
    fun `media row survives while its media root is missing`() = runBlocking {
        val mediaRoot = tempFolder.newFolder("ES-DE")
        writeRom("Mario.nes")
        writeFile(mediaRoot, "downloaded_media/nes/covers/Mario.png")
        repo.setMediaRoots(listOf(mediaRoot))
        scan()
        val id = game("Mario.nes").id
        val before = db.mediaDao().findByGameId(id)
        assertTrue(mediaRoot.renameTo(File(mediaRoot.parentFile, "Ejected")))

        scan()

        assertEquals(before, db.mediaDao().findByGameId(id))
    }

    @Test
    fun `gamelist off returns to filename titles but keeps user data`() = runBlocking {
        writeRom("Mario.nes")
        scan()
        val edited = game("Mario.nes").withUserData()
        db.gameDao().update(edited)

        repo.setKeepImportedMetadata(false)
        scan()

        assertEquals(
            edited.copy(
                displayName = "Mario",
                description = null,
                rating = null,
                releaseDate = null,
                developer = null,
                publisher = null,
                genre = null,
                players = null,
            ),
            game("Mario.nes"),
        )
    }

    @Test
    fun `stale gamelist cannot clear a favorite or lower play stats`() = runBlocking {
        writeRom("Mario.nes")
        scan()
        val edited = game("Mario.nes").withUserData()
        db.gameDao().update(edited)

        importGamelist(
            """
            <gameList><game>
              <path>./Mario.nes</path>
              <favorite>false</favorite>
              <rating>0</rating>
              <playcount>1</playcount>
              <playtime>60</playtime>
              <lastplayed>20000101T000000</lastplayed>
            </game></gameList>
            """.trimIndent(),
        )

        assertEquals(edited, game("Mario.nes"))
    }

    @Test
    fun `gamelist favorite applies once and respects a later unfavorite`() = runBlocking {
        writeRom("Mario.nes")
        scan()
        val xml = "<gameList><game><path>./Mario.nes</path><favorite>true</favorite></game></gameList>"
        val seen = mutableSetOf<String>()

        importGamelist(xml, seen)
        assertTrue(game("Mario.nes").isFavorite)

        db.gameDao().setFavorite(game("Mario.nes").id, false)
        importGamelist(xml, seen)
        assertFalse(game("Mario.nes").isFavorite)
    }

    private suspend fun importGamelist(xml: String, seen: MutableSet<String>? = null) {
        val sourceId = db.sourceDao().getAll().single().id
        GamelistImporter(db.gameDao(), catalog).importStream(xml.byteInputStream(), sourceId, "nes", seen)
    }

    private fun GameEntity.withUserData() = copy(
        displayName = "Super Mario Bros",
        description = "Imported description",
        rating = 0.9f,
        releaseDate = "19851018T000000",
        developer = "Nintendo",
        publisher = "Nintendo",
        genre = "Platformer",
        players = "1-2",
        isFavorite = true,
        isHidden = true,
        playCount = 12,
        playTimeMinutes = 340,
        lastPlayed = 1_700_000_000_000L,
        crc32 = "ABCD1234",
        md5 = "d41d8cd98f00b204e9800998ecf8427e",
        emulatorPackage = "org.example.emulator",
        launchCommand = "custom-command",
        emulatorCore = "fceumm",
    )

    private suspend fun scan() {
        if (db.sourceDao().getAll().isEmpty()) {
            repo.addSource(roms.absolutePath, roms.absolutePath)
        }
        repo.scanAll()
        withTimeout(5_000) { repo.state.first { it is LibraryState.Idle } }
    }

    private suspend fun findGame(relativePath: String): GameEntity? =
        db.gameDao().findByTriple(db.sourceDao().getAll().single().id, "nes", relativePath)

    private suspend fun game(relativePath: String): GameEntity = findGame(relativePath)!!

    private fun writeRom(relative: String): File = writeFile(roms, "nes/$relative")

    private fun writeFile(root: File, relative: String): File {
        val file = File(root, relative)
        file.parentFile?.mkdirs()
        file.writeText("stub")
        return file
    }
}
