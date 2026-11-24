package jr.brian.home.ui.meetjarn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MeetJarnContentTest {
    @Test
    fun `the first line follows what the library has`() {
        assertEquals(MeetJarnLibrary.EsDeArt, meetJarnLibrary(gameCount = 840, esDeArt = true))
        assertEquals(MeetJarnLibrary.Games, meetJarnLibrary(gameCount = 840, esDeArt = false))
        assertEquals(MeetJarnLibrary.NoGames, meetJarnLibrary(gameCount = 0, esDeArt = true))
    }

    @Test
    fun `a few covers fall back to the bundled picture`() {
        val few = List(MEET_JARN_MIN_COVERS - 1) { "cover$it" }
        assertNull(meetJarnHeroArt(few, "Game", "cover0", emptyList(), null))
    }

    @Test
    fun `enough covers build the hero, trimmed to what it shows`() {
        val many = List(200) { "cover$it" }
        val logos = List(9) { "logo$it" }
        val art = meetJarnHeroArt(many, "Game", null, logos, "Nintendo DS")
        assertNotNull(art)
        art!!
        assertEquals(MEET_JARN_WALL_COVERS, art.wall.size)
        assertEquals(MEET_JARN_SHELF_SYSTEMS, art.systemLogos.size)
        assertEquals("cover0", art.continueCover)
    }

    @Test
    fun `no game to continue means the bundled picture`() {
        assertNull(meetJarnHeroArt(List(20) { "cover$it" }, null, null, emptyList(), null))
    }
}
