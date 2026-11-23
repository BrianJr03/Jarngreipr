package jr.brian.jarnfe.library

import jr.brian.frontend.library.media.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NowGameFactsTest {
    @Test
    fun `missing and blank metadata shows no facts`() {
        val facts = JarnGameMetadata(
            genre = "  ",
            players = "0",
            releaseDate = "unknown",
            rating = "",
            playtimeMinutes = 0,
            lastPlayedAtMs = 0,
        ).nowFacts()
        assertTrue(facts.isEmpty)
    }

    @Test
    fun `present facts are trimmed and kept`() {
        val facts = JarnGameMetadata(
            genre = " Racing ",
            players = "1-2",
            releaseDate = "19950101T000000",
            rating = "80%",
            playtimeMinutes = 90,
            lastPlayedAtMs = 5,
        ).nowFacts()
        assertFalse(facts.isEmpty)
        assertEquals(NowGameFacts("Racing", "1995", "1-2", 90, 5, "80%"), facts)
    }

    @Test
    fun `the year is found in common date formats`() {
        assertEquals("1995", releaseYear("19950101T000000"))
        assertEquals("2004", releaseYear("2004-03-01"))
        assertEquals("1998", releaseYear("03/01/1998"))
        assertNull(releaseYear("00000000T000000"))
        assertNull(releaseYear(null))
    }

    @Test
    fun `players are worded by their shape`() {
        assertEquals(NowPlayersKind.One, nowPlayersKind("1"))
        assertEquals(NowPlayersKind.Count, nowPlayersKind("4"))
        assertEquals(NowPlayersKind.Count, nowPlayersKind("1-2"))
        assertEquals(NowPlayersKind.Count, nowPlayersKind("2+"))
        assertEquals(NowPlayersKind.Text, nowPlayersKind("Single player"))
    }

    @Test
    fun `art shown full screen is not repeated`() {
        val slots = mapOf(
            MediaKind.Marquee to "marquee",
            MediaKind.Cover to "cover",
            MediaKind.PhysicalMedia to "disc",
        )
        assertEquals(NowGameArt("marquee", "cover", "disc", true), nowGameArt(slots, "fanart"))
        assertNull(nowGameArt(slots, "marquee").marqueePath)
        assertNull(nowGameArt(slots, "disc").mediaPath)
    }

    @Test
    fun `no art still counts as loaded`() {
        assertEquals(NowGameArt(isLoaded = true), nowGameArt(mapOf(MediaKind.Cover to " "), null))
    }
}
