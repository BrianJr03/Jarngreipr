package jr.brian.frontend.library.play

import android.app.Application
import androidx.room.Room
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.LibraryDatabase
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.db.SystemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PlaySessionRecorderTest {
    private lateinit var db: LibraryDatabase
    private val anchors = MemoryAnchors()
    private val clock = FakeClock()
    private var pending = emptyList<BackedUpPlayStat>()
    private lateinit var backup: PlayStatsBackup
    private lateinit var recorder: PlaySessionRecorder
    private var gameId = 0L

    @Before
    fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), LibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val sourceId = db.sourceDao().insert(SourceEntity(treeUri = "tree", displayPath = "Roms"))
        db.systemDao().upsertAll(listOf(SystemEntity(id = SYSTEM, displayName = "GBA")))
        gameId = db.gameDao().insert(
            GameEntity(
                sourceId = sourceId,
                systemId = SYSTEM,
                relativePath = PATH,
                displayName = "A",
                playCount = 5,
                playTimeMinutes = 100,
            ),
        )
        backup = PlayStatsBackup(db.playHistoryDao(), { pending }, { pending = it })
        recorder = newRecorder()
    }

    @After
    fun tearDown() = db.close()

    private fun newRecorder() =
        PlaySessionRecorder(db.playHistoryDao(), anchors, clock, backup, Dispatchers.Unconfined)

    private suspend fun playFor(ms: Long, screen: String = STAGE) {
        recorder.handleLaunch(SYSTEM, PATH, clock.now)
        recorder.handleStopped(screen, clock.now + 500)
        clock.now += ms
        recorder.handleResumed(screen, clock.now)
    }

    private suspend fun game() = db.gameDao().findById(gameId)!!

    @Test
    fun `a real session adds one play and its minutes to imported totals`() = runBlocking {
        playFor(10 * MINUTE)

        assertEquals(6, game().playCount)
        assertEquals(110, game().playTimeMinutes)
        assertNull(anchors.read())
    }

    @Test
    fun `a session under the minimum adds nothing and leaves no row`() = runBlocking {
        playFor((MIN_COUNTED_SESSION_SECONDS - 1) * 1_000L)

        assertEquals(5, game().playCount)
        assertEquals(0, db.playHistoryDao().observeForGame(gameId).first().size)
    }

    @Test
    fun `a second resume does not count the session twice`() = runBlocking {
        playFor(5 * MINUTE)
        clock.now += MINUTE
        recorder.handleResumed(STAGE, clock.now)

        assertEquals(6, game().playCount)
        assertEquals(105, game().playTimeMinutes)
    }

    @Test
    fun `a session survives process death and is counted once`() = runBlocking {
        recorder.handleLaunch(SYSTEM, PATH, clock.now)
        recorder.handleStopped(STAGE, clock.now + 500)
        clock.now += 20 * MINUTE

        recorder = newRecorder()
        recorder.handleResumed(STAGE, clock.now)
        recorder = newRecorder()
        recorder.handleResumed(STAGE, clock.now + MINUTE)

        assertEquals(6, game().playCount)
        assertEquals(120, game().playTimeMinutes)
    }

    @Test
    fun `dying after the credit but before clearing the anchor does not double count`() = runBlocking {
        recorder.handleLaunch(SYSTEM, PATH, clock.now)
        recorder.handleStopped(STAGE, clock.now + 500)
        val saved = anchors.read()!!
        clock.now += 10 * MINUTE
        recorder.handleResumed(STAGE, clock.now)
        anchors.write(saved)

        recorder = newRecorder()
        recorder.handleResumed(STAGE, clock.now + MINUTE)

        assertEquals(6, game().playCount)
        assertEquals(110, game().playTimeMinutes)
        assertNull(anchors.read())
    }

    @Test
    fun `the other screen resuming does not end the session`() = runBlocking {
        recorder.handleLaunch(SYSTEM, PATH, clock.now)
        recorder.handleStopped(STAGE, clock.now + 500)
        recorder.handleStopped(DECK, clock.now + 700)
        clock.now += 10 * MINUTE
        recorder.handleResumed(DECK, clock.now)

        assertEquals(5, game().playCount)
        assertNotNull(anchors.read())
    }

    @Test
    fun `a launch that never covers a window is dropped`() = runBlocking {
        recorder.handleLaunch(SYSTEM, PATH, clock.now)
        clock.now += LAUNCH_COVER_WINDOW_MS + 1
        recorder.handleResumed(STAGE, clock.now)

        assertEquals(5, game().playCount)
        assertNull(anchors.read())
        assertEquals(0, db.playHistoryDao().observeForGame(gameId).first().size)
    }

    @Test
    fun `an implausibly long session is capped`() = runBlocking {
        playFor(3 * 24 * 60 * MINUTE)

        assertEquals(6, game().playCount)
        assertEquals(100 + MAX_COUNTED_SESSION_SECONDS / 60, game().playTimeMinutes)
    }

    @Test
    fun `a reboot during a session adds nothing`() = runBlocking {
        recorder.handleLaunch(SYSTEM, PATH, clock.now)
        recorder.handleStopped(STAGE, clock.now + 500)
        clock.boot = 2
        clock.now += 10 * MINUTE
        recorder.handleResumed(STAGE, clock.now)

        assertEquals(5, game().playCount)
        assertNull(anchors.read())
    }

    @Test
    fun `leftover seconds carry into the next session`() = runBlocking {
        playFor(90_000)
        assertEquals(101, game().playTimeMinutes)
        playFor(30_000)

        assertEquals(102, game().playTimeMinutes)
    }

    @Test
    fun `restoring only raises totals and waits for unscanned games`() = runBlocking {
        backup.restore(
            listOf(
                BackedUpPlayStat(SYSTEM, PATH, 2, 500),
                BackedUpPlayStat(SYSTEM, "later.gba", 3, 30),
            ),
        )

        assertEquals(5, game().playCount)
        assertEquals(500, game().playTimeMinutes)
        assertEquals(listOf("later.gba"), pending.map { it.relativePath })
        assertEquals(2, backup.export()!!.size)
    }

    private class MemoryAnchors : PlayAnchorStore {
        private var value: PlayAnchor? = null
        override fun read() = value
        override fun write(anchor: PlayAnchor) { value = anchor }
        override fun clear() { value = null }
    }

    private class FakeClock : PlayClock {
        var now = 1_000_000L
        var boot: Int? = 1
        override fun uptimeMs() = now
        override fun wallMs() = now
        override fun bootCount() = boot
    }

    private companion object {
        const val SYSTEM = "gba"
        const val PATH = "a.gba"
        const val STAGE = "Stage"
        const val DECK = "Deck"
        const val MINUTE = 60_000L
    }
}
