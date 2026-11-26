package jr.brian.frontend.library.collections

import android.app.Application
import androidx.room.Room
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.LibraryDatabase
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.db.SystemEntity
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CollectionsBackupTest {
    private lateinit var db: LibraryDatabase
    private lateinit var repository: CollectionsRepository
    private lateinit var backup: CollectionsBackup

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), LibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = CollectionsRepository(db.collectionDao())
        backup = CollectionsBackup(repository)
        db.systemDao().upsert(SystemEntity(id = "snes", displayName = "SNES"))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `export keeps order and members whose ROM is missing`() = runBlocking {
        val rpg = create("RPGs")
        repository.addGame(rpg, "snes", "Chrono.sfc")
        create("Party")

        val exported = backup.export()!!

        assertEquals(listOf("RPGs", "Party"), exported.map { it.name })
        assertEquals(listOf(member("Chrono.sfc")), exported.first().members)
    }

    @Test
    fun `a backup round trips onto an empty device`() = runBlocking {
        val saved = listOf(
            BackedUpCollection("RPGs", 0, listOf(member("Chrono.sfc"), member("Secret.sfc"))),
            BackedUpCollection("Party", 1, emptyList()),
        )

        backup.restore(saved)

        assertEquals(saved, backup.export())
    }

    @Test
    fun `a same name collection ignoring case gains members and keeps its own`() = runBlocking {
        val mine = create("RPGs")
        repository.addGame(mine, "snes", "Mine.sfc")

        backup.restore(listOf(BackedUpCollection("rpgs", 0, listOf(member("Chrono.sfc"), member("Mine.sfc")))))

        val after = backup.export()!!
        assertEquals(listOf("RPGs"), after.map { it.name })
        assertEquals(setOf(member("Mine.sfc"), member("Chrono.sfc")), after.single().members.toSet())
    }

    @Test
    fun `new collections go after the device's own in the backup's order`() = runBlocking {
        create("Mine")

        backup.restore(listOf(BackedUpCollection("Second", 1, emptyList()), BackedUpCollection("First", 0, emptyList())))

        assertEquals(listOf("Mine", "First", "Second"), backup.export()!!.map { it.name })
    }

    @Test
    fun `an empty backup list changes nothing`() = runBlocking {
        val mine = create("Mine")
        repository.addGame(mine, "snes", "Mine.sfc")
        val before = backup.export()

        backup.restore(emptyList())

        assertEquals(before, backup.export())
    }

    @Test
    fun `a blank name or blank member is skipped`() = runBlocking {
        backup.restore(
            listOf(
                BackedUpCollection("  ", 0, listOf(member("A.sfc"))),
                BackedUpCollection("Ok", 1, listOf(member(""), member("B.sfc"))),
            ),
        )

        val after = backup.export()!!
        assertEquals(listOf("Ok"), after.map { it.name })
        assertEquals(listOf(member("B.sfc")), after.single().members)
    }

    @Test
    fun `members show once a scan finds them under a different ROM folder`() = runBlocking {
        backup.restore(listOf(BackedUpCollection("RPGs", 0, listOf(member("Chrono.sfc")))))
        assertTrue(repository.presentMembers.first().isEmpty())

        val source = db.sourceDao().insert(SourceEntity(treeUri = "/new/card/roms", displayPath = "/new/card/roms"))
        db.gameDao().insert(
            GameEntity(
                sourceId = source,
                systemId = "snes",
                relativePath = "Chrono.sfc",
                absolutePath = "/new/card/roms/snes/Chrono.sfc",
                displayName = "Chrono",
            ),
        )

        val present = repository.presentMembers.first().single()
        assertEquals("/new/card/roms/snes/Chrono.sfc", present.absolutePath)
    }

    private fun member(path: String) = BackedUpCollectionMember("snes", path)

    private suspend fun create(name: String): Long =
        (repository.create(name) as CollectionNameResult.Saved).collectionId
}
