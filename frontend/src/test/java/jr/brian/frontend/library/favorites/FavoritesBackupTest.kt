package jr.brian.frontend.library.favorites

import android.app.Application
import androidx.room.Room
import jr.brian.frontend.library.LibraryState
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.LibraryDatabase
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.db.SystemEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class FavoritesBackupTest {
    private lateinit var db: LibraryDatabase
    private lateinit var backup: FavoritesBackup
    private var pending = emptyList<BackedUpFavorite>()
    private var sourceId = 0L

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), LibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        backup = FavoritesBackup(db.gameDao(), { pending }, { pending = it })
        db.systemDao().upsert(SystemEntity(id = "snes", displayName = "SNES"))
        sourceId = addSource("/roms")
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `export lists favorites, hidden ones too, sorted and without repeats`() = runBlocking {
        insert("B.sfc", favorite = true)
        insert("A.sfc", favorite = true, hidden = true)
        insert("C.sfc")
        insert("B.sfc", favorite = true, source = addSource("/other"))

        assertEquals(listOf(fav("A.sfc"), fav("B.sfc")), backup.export())
    }

    @Test
    fun `a game already in the library is favorited at once`() = runBlocking {
        insert("A.sfc")

        backup.restore(listOf(fav("A.sfc")))

        assertTrue(favorite("A.sfc"))
        assertTrue(pending.isEmpty())
    }

    @Test
    fun `a game not scanned yet waits and is favorited after the scan`() = runBlocking {
        backup.restore(listOf(fav("Later.sfc")))
        assertEquals(listOf(fav("Later.sfc")), pending)
        assertEquals(listOf(fav("Later.sfc")), backup.export())

        insert("Later.sfc")
        backup.applyPending()

        assertTrue(favorite("Later.sfc"))
        assertTrue(pending.isEmpty())
    }

    @Test
    fun `a different ROM folder still matches by system and path`() = runBlocking {
        val newCard = addSource("/new/card")
        db.gameDao().insert(
            GameEntity(
                sourceId = newCard,
                systemId = "snes",
                relativePath = "A.sfc",
                absolutePath = "/new/card/snes/A.sfc",
                displayName = "A",
            ),
        )

        backup.restore(listOf(fav("A.sfc")))

        assertTrue(favorite("A.sfc"))
    }

    @Test
    fun `a restore never unfavorites`() = runBlocking {
        insert("Mine.sfc", favorite = true)

        backup.restore(listOf(fav("Other.sfc")))
        backup.restore(emptyList())

        assertTrue(favorite("Mine.sfc"))
    }

    @Test
    fun `blank keys are dropped`() = runBlocking {
        backup.restore(listOf(BackedUpFavorite("", "A.sfc"), BackedUpFavorite("snes", " ")))

        assertTrue(pending.isEmpty())
    }

    @Test
    fun `scan ends fire once at the start and after each scan`() = runBlocking {
        val states = flowOf(
            LibraryState.Idle,
            LibraryState.Scanning(0, 1),
            LibraryState.Scanning(1, 1),
            LibraryState.Idle,
            LibraryState.Scanning(0, 1),
            LibraryState.Error("boom"),
        )

        assertEquals(3, states.scanEnds().toList().size)
    }

    private fun fav(path: String) = BackedUpFavorite("snes", path)

    private suspend fun favorite(path: String): Boolean =
        db.gameDao().favoriteKeys().any { it.relativePath == path }

    private suspend fun addSource(path: String) =
        db.sourceDao().insert(SourceEntity(treeUri = path, displayPath = path))

    private suspend fun insert(path: String, favorite: Boolean = false, hidden: Boolean = false, source: Long = sourceId) {
        db.gameDao().insert(
            GameEntity(
                sourceId = source,
                systemId = "snes",
                relativePath = path,
                displayName = path,
                isFavorite = favorite,
                isHidden = hidden,
            ),
        )
    }
}
