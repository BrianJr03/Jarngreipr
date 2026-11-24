package jr.brian.frontend.library.setup

import android.app.Application
import androidx.room.Room
import jr.brian.frontend.library.LibraryRepository
import jr.brian.frontend.library.LibraryState
import jr.brian.frontend.library.SourceScanTarget
import jr.brian.frontend.library.db.LibraryDatabase
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.scan.FastWalkScanner
import jr.brian.frontend.library.scan.SafTreeScanner
import jr.brian.frontend.library.scan.ScannerDispatcher
import jr.brian.frontend.library.scan.StubSystemCatalog
import jr.brian.frontend.library.systems.SystemDef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
class LibrarySetupTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val catalog = StubSystemCatalog(
        SystemDef(id = "nds", displayName = "DS", extensions = setOf("nds")),
        SystemDef(id = "n3ds", displayName = "3DS", folderAliases = listOf("3ds"), extensions = setOf("3ds")),
    )
    private val roms = tree("primary%3AROMs")
    private val nds = tree("primary%3AROMs%2Fnds")
    private val sd = tree("3A0D-5000%3AAyn%20Thor")

    @Test
    fun `the stored flag is ignored when no real grant exists`() {
        val source = source(roms, flag = true)
        assertEquals(listOf(source), classifySources(listOf(source), emptySet()) { true }.unreadable)
    }

    @Test
    fun `a real grant is scannable even when the stored flag is false`() {
        val source = source(roms, flag = false)
        assertEquals(listOf(source), classifySources(listOf(source), setOf(roms)) { false }.scannable)
    }

    @Test
    fun `internal and sd card folders are independent`() {
        val internal = source(roms)
        val card = source(sd)
        val access = classifySources(listOf(internal, card), setOf(roms, sd)) { false }
        assertEquals(listOf(internal, card), access.scannable)
    }

    @Test
    fun `a folder inside a readable source is covered, even with its own grant`() {
        val parent = source(roms)
        val child = source(nds)
        val access = classifySources(listOf(child, parent), setOf(roms, nds)) { false }
        assertEquals(listOf(parent), access.scannable)
        assertEquals(listOf(child), access.covered)
    }

    @Test
    fun `a grant that is not a source does not cover a folder`() {
        val child = source(nds)
        assertEquals(listOf(child), classifySources(listOf(child), setOf(roms)) { false }.unreadable)
    }

    @Test
    fun `a tree and a path to the same folder scan only once`() {
        val path = source("/storage/emulated/0/ROMs")
        val saf = source(roms)
        val access = classifySources(listOf(path, saf), setOf(roms)) { true }
        assertEquals(listOf(path), access.scannable)
        assertEquals(listOf(saf), access.covered)
    }

    @Test
    fun `locations map volumes and never match a sibling prefix`() {
        assertEquals("/storage/emulated/0/ROMs", sourceLocation(roms))
        assertEquals("/storage/3A0D-5000/Ayn Thor", sourceLocation(sd))
        assertTrue(locationContains("/storage/emulated/0/ROMs", "/storage/emulated/0/ROMs/nds"))
        assertFalse(locationContains("/storage/emulated/0/ROMs", "/storage/emulated/0/ROMs2/nds"))
    }

    @Test
    fun `detection prefers subfolders, then the folder's own name`() {
        val parent = detectSystems("Ayn Thor", listOf("nds", "3ds", "Notes"), catalog)
        assertEquals(setOf("nds", "n3ds"), parent.systemIds)
        assertNull(parent.singleSystem)

        val single = detectSystems("3ds", listOf("Saves"), catalog)
        assertEquals(setOf("n3ds"), single.systemIds)
        assertEquals("n3ds", single.singleSystem?.id)

        assertTrue(detectSystems("Music", listOf("Albums"), catalog).systemIds.isEmpty())
    }

    @Test
    fun `setup states follow the library`() {
        val none = SourceAccess(emptyList(), emptyList(), emptyList())
        val lost = none.copy(unreadable = listOf(source(roms)))
        val readable = none.copy(scannable = listOf(source(roms)))

        assertEquals(LibrarySetupState.NoSources, reduceSetup(none, LibraryState.Idle, false, false))
        assertEquals(
            LibrarySetupState.NeedsReconnect("/storage/emulated/0/ROMs", roms, 1),
            reduceSetup(lost, LibraryState.Idle, false, false),
        )
        assertEquals(LibrarySetupState.Checking, reduceSetup(readable, LibraryState.Idle, false, true))
        assertEquals(LibrarySetupState.NoGamesFound, reduceSetup(readable, LibraryState.Idle, false, false))
        assertEquals(
            LibrarySetupState.Scanning(1, 4),
            reduceSetup(readable, LibraryState.Scanning(1, 4), false, false),
        )
        assertEquals(LibrarySetupState.ScanFailed, reduceSetup(readable, LibraryState.Error("x"), false, false))
        assertEquals(LibrarySetupState.Ready, reduceSetup(lost, LibraryState.Error("x"), true, false))
    }

    @Test
    fun `a parent, a single system folder, and existing favorites all work`() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(app, LibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val repo = LibraryRepository(
                systemDao = db.systemDao(),
                sourceDao = db.sourceDao(),
                gameDao = db.gameDao(),
                mediaDao = db.mediaDao(),
                scrapeJobDao = db.scrapeJobDao(),
                playHistoryDao = db.playHistoryDao(),
                catalog = catalog,
                scanner = ScannerDispatcher(FastWalkScanner(catalog), SafTreeScanner(app, catalog)),
                scope = scope,
            )
            val parent = temp.newFolder("Internal")
            write(parent, "nds/Mario.nds")
            val single = temp.newFolder("card", "3ds")
            write(single, "Zelda.3ds")

            suspend fun scan(path: String, def: SystemDef? = null) {
                val id = repo.addSource(path, path)
                val source = db.sourceDao().findById(id)!!
                assertTrue(repo.scanSources(listOf(SourceScanTarget(source, def))))
                withTimeout(10_000) {
                    while (repo.state.value is LibraryState.Scanning) delay(10)
                }
            }

            scan(parent.path)
            val mario = db.gameDao().observeBySystem("nds").first().single()
            db.gameDao().setFavorite(mario.id, true)
            scan(single.path, catalog.findById("n3ds"))

            assertEquals("Zelda", db.gameDao().observeBySystem("n3ds").first().single().displayName)
            assertTrue(db.gameDao().observeBySystem("nds").first().single().isFavorite)
        } finally {
            scope.cancel()
            db.close()
        }
    }

    private fun write(root: File, path: String) {
        File(root, path).apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1))
        }
    }

    private fun tree(documentId: String) =
        "content://com.android.externalstorage.documents/tree/$documentId"

    private fun source(uri: String, flag: Boolean = true) =
        SourceEntity(treeUri = uri, displayPath = uri, hasPersistedGrant = flag)
}
