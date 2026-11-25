package jr.brian.jarnfe.deck

import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.JarnScreen.Deck
import jr.brian.jarnfe.input.JarnScreen.Stage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NowPlayingStateTest {
    private val game = NowPlayingGame("gc", "/roms/gc/Melee.rvz", "Melee")
    private val launched = NowPlayingSession(game, launchedAtUptimeMs = 1_000L, startedAtWallMs = 50_000L)

    /** The usual Thor launch: the game takes the top display, which holds the Stage window. */
    private val onTop = launched.copy(gameHost = Stage)

    private fun role(
        session: NowPlayingSession?,
        swapped: Boolean = false,
        enabled: Boolean = true,
        twoScreens: Boolean = true,
        osFocus: JarnScreen? = null,
    ) = nowPlayingRole(session, enabled, twoScreens, swapped, osFocus)

    @Before
    fun reset() = JarnNowPlaying.reset()

    @After
    fun tearDown() = JarnNowPlaying.reset()

    @Test
    fun `the first window to stop soon after a launch is the one the game covers`() {
        assertEquals(Stage, launched.onWindowStopped(Stage, 5_000L).gameHost)
        assertEquals(Deck, launched.onWindowStopped(Deck, 5_000L).gameHost)
    }

    @Test
    fun `a stop long after the launch is not the game`() {
        assertNull(launched.onWindowStopped(Stage, 1_000L + NOW_PLAYING_COVER_GRACE_MS + 1).gameHost)
    }

    @Test
    fun `a later stop of the free window, such as a companion over it, keeps the game window`() {
        assertEquals(Stage, onTop.onWindowStopped(Deck, 6_000L).gameHost)
    }

    @Test
    fun `only the covered window coming back ends the session`() {
        assertNull(onTop.onWindowResumed(Stage, 10_000_000L))
        assertNotNull(onTop.onWindowResumed(Deck, 10_000_000L))
        val onBottom = launched.copy(gameHost = Deck)
        assertNull(onBottom.onWindowResumed(Deck, 10_000_000L))
        assertNotNull(onBottom.onWindowResumed(Stage, 10_000_000L))
    }

    @Test
    fun `a launch that never covered a window ends once the grace is over`() {
        assertNotNull(launched.onWindowResumed(Stage, 2_000L))
        assertNull(launched.onWindowResumed(Deck, 1_000L + NOW_PLAYING_COVER_GRACE_MS + 1))
    }

    @Test
    fun `a tap on the free window never ends the session`() {
        JarnNowPlaying.onGameLaunched(game, nowUptimeMs = 1_000L, nowWallMs = 0L)
        JarnNowPlaying.onWindowStopped(Stage, nowUptimeMs = 3_000L)
        JarnNowPlaying.onWindowResumed(Deck, nowUptimeMs = 4_000_000L)
        assertTrue(JarnNowPlaying.session.isGameInFront)
        JarnNowPlaying.onWindowResumed(Stage, nowUptimeMs = 4_000_001L)
        assertNull(JarnNowPlaying.session)
    }

    @Test
    fun `unswapped, the game covers the Stage window and the page shows on the Deck`() {
        assertEquals(Deck, role(onTop))
        assertEquals(Deck, role(onTop, osFocus = Deck))
        assertNull(role(onTop, osFocus = Stage))
    }

    @Test
    fun `swapped, the top window shows the Deck role, so the page shows on the Stage role below`() {
        assertEquals(Stage, role(onTop, swapped = true))
        assertEquals(Stage, role(onTop, swapped = true, osFocus = Stage))
        assertNull(role(onTop, swapped = true, osFocus = Deck))
    }

    @Test
    fun `a game on the bottom display leaves the top window free, in both orientations`() {
        val onBottom = launched.copy(gameHost = Deck)
        assertEquals(Stage, role(onBottom))
        assertEquals(Deck, role(onBottom, swapped = true))
    }

    @Test
    fun `the free window is the one the game left, in both orientations`() {
        assertEquals(Deck, nowPlayingFreeRole(onTop, deckDisplayAvailable = true, rolesSwapped = false))
        assertEquals(Stage, nowPlayingFreeRole(onTop, deckDisplayAvailable = true, rolesSwapped = true))
        assertNull(nowPlayingFreeRole(launched, deckDisplayAvailable = true, rolesSwapped = true))
    }

    @Test
    fun `the page never shows when off, on one screen, dismissed, or before the game covers a window`() {
        listOf(false, true).forEach { swapped ->
            assertNull(role(onTop, swapped = swapped, enabled = false))
            assertNull(role(onTop, swapped = swapped, twoScreens = false))
            assertNull(role(onTop.copy(dismissed = true), swapped = swapped))
            assertNull(role(launched, swapped = swapped))
            assertNull(role(null, swapped = swapped))
        }
    }

    @Test
    fun `the pill sits on the free role only once the player left the page`() {
        val left = onTop.copy(dismissed = true)
        assertEquals(Deck, nowPlayingPillRole(left, enabled = true, deckDisplayAvailable = true, rolesSwapped = false))
        assertEquals(Stage, nowPlayingPillRole(left, enabled = true, deckDisplayAvailable = true, rolesSwapped = true))
        assertNull(nowPlayingPillRole(onTop, enabled = true, deckDisplayAvailable = true, rolesSwapped = false))
        assertNull(nowPlayingPillRole(left, enabled = true, deckDisplayAvailable = false, rolesSwapped = false))
    }

    @Test
    fun `a new launch replaces the old session and shows the page again`() {
        JarnNowPlaying.onGameLaunched(game, nowUptimeMs = 1_000L, nowWallMs = 0L)
        JarnNowPlaying.dismiss()
        JarnNowPlaying.onGameLaunched(game.copy(title = "Kirby"), nowUptimeMs = 9_000L, nowWallMs = 0L)
        assertEquals("Kirby", JarnNowPlaying.session?.game?.title)
        assertEquals(false, JarnNowPlaying.session?.dismissed)
    }

    @Test
    fun `companions offered after the launch stay with the session`() {
        JarnNowPlaying.onGameLaunched(game, nowUptimeMs = 1_000L, nowWallMs = 0L)
        JarnNowPlaying.onCompanionsOffered(listOf("a.maps", "b.wiki"))
        assertEquals(listOf("a.maps", "b.wiki"), JarnNowPlaying.session?.companions)
    }

    @Test
    fun `a companion opening first on the free screen is not taken for the game`() {
        val known = launched.copy(expectedHost = Stage)
        val afterCompanion = known.onWindowStopped(Deck, 2_000L)
        assertNull(afterCompanion.gameHost)
        assertEquals(Stage, afterCompanion.onWindowStopped(Stage, 3_000L).gameHost)
    }

    @Test
    fun `the launcher's display maps to the window on it`() {
        assertEquals(Stage, gameHostOn(0, primaryDisplayId = 0, deckDisplayId = 4))
        assertEquals(Deck, gameHostOn(4, primaryDisplayId = 0, deckDisplayId = 4))
        assertNull(gameHostOn(null, primaryDisplayId = 0, deckDisplayId = 4))
        assertNull(gameHostOn(7, primaryDisplayId = 0, deckDisplayId = 4))
    }

    @Test
    fun `a single companion auto-opened before the game is not mistaken for the game window`() {
        val expected = gameHostOn(displayId = 0, primaryDisplayId = 0, deckDisplayId = 4)
        JarnNowPlaying.onGameLaunched(game, expectedHost = expected, nowUptimeMs = 1_000L, nowWallMs = 0L)
        JarnNowPlaying.onWindowStopped(Deck, nowUptimeMs = 1_200L)
        JarnNowPlaying.onWindowStopped(Stage, nowUptimeMs = 1_900L)
        assertEquals(Stage, JarnNowPlaying.session?.gameHost)
        assertEquals(Deck, role(JarnNowPlaying.session))
        JarnNowPlaying.onWindowResumed(Deck, nowUptimeMs = 600_000L)
        assertTrue(JarnNowPlaying.session.isGameInFront)
        JarnNowPlaying.onWindowResumed(Stage, nowUptimeMs = 900_000L)
        assertNull(JarnNowPlaying.session)
    }
}
