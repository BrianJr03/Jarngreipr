package jr.brian.frontend.library.setup

import android.app.Application
import androidx.room.Room
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.LibraryDatabase
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.db.SystemEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LibraryFolderBackupTest {
    private lateinit var db: LibraryDatabase
    private var grants = emptySet<String>()
    private var readablePaths = emptySet<String>()
    private lateinit var backup: LibraryFolderBackup

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), LibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        backup = LibraryFolderBackup(db.sourceDao(), { grants }, { it in readablePaths })
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `a restored folder without a live grant needs reconnecting`() = runBlocking {
        backup.restore(listOf(BackedUpFolder(ROMS, "Roms")))

        val restored = db.sourceDao().findByTreeUri(ROMS)!!
        assertFalse(restored.hasPersistedGrant)
        assertEquals(0L, restored.lastScannedAt)
        val access = classifySources(db.sourceDao().getAll(), grants) { it in readablePaths }
        assertEquals(listOf(ROMS), access.unreadable.map { it.treeUri })
        assertTrue(access.scannable.isEmpty())
    }

    @Test
    fun `a restored folder with a live grant is usable`() = runBlocking {
        grants = setOf(ROMS)
        backup.restore(listOf(BackedUpFolder(ROMS, "Roms")))

        assertTrue(db.sourceDao().findByTreeUri(ROMS)!!.hasPersistedGrant)
    }

    @Test
    fun `restoring keeps existing rows, games, and favorites`() = runBlocking {
        val sourceId = db.sourceDao().insert(SourceEntity(treeUri = ROMS, displayPath = "Mine", lastScannedAt = 7L))
        db.systemDao().upsertAll(listOf(SystemEntity(id = "gba", displayName = "GBA")))
        val gameId = db.gameDao().insert(
            GameEntity(sourceId = sourceId, systemId = "gba", relativePath = "a.gba", displayName = "A", isFavorite = true, playCount = 3),
        )
        val before = db.sourceDao().findById(sourceId)

        val added = backup.restore(listOf(BackedUpFolder(ROMS, "Other"), BackedUpFolder(SD, "SD")))

        assertEquals(1, added)
        assertEquals(before, db.sourceDao().findById(sourceId))
        val game = db.gameDao().findById(gameId)!!
        assertTrue(game.isFavorite)
        assertEquals(3, game.playCount)
    }

    @Test
    fun `restoring twice does not duplicate folders`() = runBlocking {
        val folders = listOf(BackedUpFolder(ROMS, "Roms"), BackedUpFolder(SD, "SD"))
        assertEquals(2, backup.restore(folders))
        assertEquals(0, backup.restore(folders))
        assertEquals(2, db.sourceDao().getAll().size)
    }

    @Test
    fun `the same location in another form is not added again`() = runBlocking {
        db.sourceDao().insert(SourceEntity(treeUri = "/storage/emulated/0/ROMs", displayPath = "Roms"))

        assertEquals(0, backup.restore(listOf(BackedUpFolder(ROMS, "Roms"))))
    }

    @Test
    fun `an empty list removes nothing`() = runBlocking {
        db.sourceDao().insert(SourceEntity(treeUri = ROMS, displayPath = "Roms"))

        assertEquals(0, backup.restore(emptyList()))
        assertEquals(1, db.sourceDao().getAll().size)
    }

    @Test
    fun `malformed entries are skipped without failing`() = runBlocking {
        val added = backup.restore(
            listOf(
                BackedUpFolder("", ""),
                BackedUpFolder("/", ""),
                BackedUpFolder("relative/path", ""),
                BackedUpFolder("/storage/../data", ""),
                BackedUpFolder("file:///storage/emulated/0/ROMs", ""),
                BackedUpFolder("content://com.android.externalstorage.documents/document/x", ""),
                BackedUpFolder(SD, ""),
            ),
        )

        assertEquals(1, added)
        assertEquals(SD, db.sourceDao().findByTreeUri(SD)!!.displayPath)
    }

    @Test
    fun `export carries folder identity only`() = runBlocking {
        db.sourceDao().insert(
            SourceEntity(treeUri = ROMS, displayPath = "Roms", volumeId = "primary", lastScannedAt = 9L),
        )

        assertEquals(listOf(BackedUpFolder(ROMS, "Roms", "primary")), backup.export())
    }

    private companion object {
        const val ROMS = "content://com.android.externalstorage.documents/tree/primary%3AROMs"
        const val SD = "content://com.android.externalstorage.documents/tree/3A0D-5000%3AGames"
    }
}
