package jr.brian.frontend.library.art

import android.app.Application
import androidx.room.Room
import jr.brian.core.model.ArtSource
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.LibraryDatabase
import jr.brian.frontend.library.db.MediaEntity
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.db.SystemEntity
import jr.brian.frontend.library.media.MediaKind
import jr.brian.frontend.library.scan.StubSystemCatalog
import jr.brian.frontend.library.systems.SystemDef
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CustomArtPriorityTest {
    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var db: LibraryDatabase
    private lateinit var esde: File
    private lateinit var retroHrai: File
    private lateinit var scraped: File
    private lateinit var resolver: GameArtResolver
    private var custom = emptyMap<String, CustomGameArt>()

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(app, LibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        esde = temp.newFolder("esde")
        retroHrai = temp.newFolder("retrohrai")
        scraped = temp.newFolder("scraped")
        val shared = temp.newFolder("shared")
        val roots = ArtRoots(
            esdeRoot = { esde },
            retroHraiRoot = retroHrai,
            sharedScraped = { shared },
            jarnScrapedRoot = scraped,
        )
        val catalog = StubSystemCatalog(SystemDef(id = "gba", displayName = "GBA", extensions = setOf("gba")))
        resolver = GameArtResolver(
            ArtIndexes(roots, catalog),
            roots,
            db.mediaDao(),
            ArtSourceStore(app),
        ) { systemId -> if (systemId == "gba") custom else emptyMap() }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `custom art beats every preferred source and the library row`() = runBlocking {
        val game = gameWithArtEverywhere()
        val mine = temp.newFile("mine.png").path
        custom = mapOf("Metroid" to CustomGameArt(cover = mine))

        ArtSource.entries.forEach { preferred ->
            val slot = resolver.resolve(game, preferred).cover!!
            assertEquals(mine, slot.path)
            assertNull(slot.source)
            val tile = resolver.tileArt(listOf(game.toLibraryGame()), preferred)
            assertEquals(mine, tile[game.id])
        }
    }

    @Test
    fun `each custom kind fills its own slot only`() = runBlocking {
        val game = gameWithArtEverywhere()
        val marquee = temp.newFile("m.png").path
        val fanart = temp.newFile("f.png").path
        val shot = temp.newFile("s.png").path
        custom = mapOf("Metroid" to CustomGameArt(marquee = marquee, fanart = fanart, screenshot = shot))

        val art = resolver.resolve(game, ArtSource.EsDe)

        assertEquals(marquee, art[MediaKind.Marquee]?.path)
        assertEquals(fanart, art[MediaKind.Fanart]?.path)
        assertEquals(shot, art[MediaKind.Screenshot]?.path)
        assertEquals(ArtSource.EsDe, art.cover?.source)
    }

    @Test
    fun `a deleted custom image falls back to the normal winner`() = runBlocking {
        val game = gameWithArtEverywhere()
        custom = mapOf("Metroid" to CustomGameArt(cover = File(temp.root, "gone.png").path))

        assertEquals(ArtSource.RetroHrai, resolver.resolve(game, ArtSource.RetroHrai).cover?.source)
    }

    @Test
    fun `another game's custom art is not applied`() = runBlocking {
        val game = gameWithArtEverywhere()
        custom = mapOf("Zelda" to CustomGameArt(cover = temp.newFile("z.png").path))

        assertEquals(ArtSource.EsDe, resolver.resolve(game, ArtSource.EsDe).cover?.source)
    }

    @Test
    fun `resolving custom art writes nothing and leaves the library row alone`() = runBlocking {
        val game = gameWithArtEverywhere()
        custom = mapOf("Metroid" to CustomGameArt(cover = temp.newFile("mine2.png").path))
        val rowBefore = db.mediaDao().findByGameId(game.id)
        val filesBefore = temp.root.walkTopDown().map { it.path to it.lastModified() }.toList()

        resolver.resolve(game, ArtSource.EsDe)

        assertEquals(rowBefore, db.mediaDao().findByGameId(game.id))
        assertEquals(filesBefore, temp.root.walkTopDown().map { it.path to it.lastModified() }.toList())
    }

    private suspend fun gameWithArtEverywhere(): GameEntity {
        write(esde, "gba/covers/Metroid.png")
        write(retroHrai, "gba/covers/Metroid.png")
        write(scraped, "gba/covers/Metroid.png")
        val sourceId = db.sourceDao().insert(SourceEntity(treeUri = "/roms", displayPath = "/roms"))
        db.systemDao().upsertAll(listOf(SystemEntity(id = "gba", displayName = "GBA")))
        val id = db.gameDao().insert(
            GameEntity(sourceId = sourceId, systemId = "gba", relativePath = "Metroid.gba", displayName = "Metroid"),
        )
        val rowCover = write(temp.root, "row/Metroid.png").path
        db.mediaDao().replaceForSystem(emptyList(), listOf(MediaEntity(gameId = id, coverPath = rowCover)))
        return db.gameDao().findById(id)!!
    }

    private fun write(root: File, path: String): File =
        File(root, path).apply {
            parentFile?.mkdirs()
            writeText("x")
        }
}
