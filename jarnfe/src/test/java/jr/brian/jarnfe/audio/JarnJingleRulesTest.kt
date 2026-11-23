package jr.brian.jarnfe.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnJingleRulesTest {
    @Test
    fun `slug accepts owner repo and GitHub links`() {
        assertEquals("owner/repo", jarnJingleRepoSlug(" owner/repo "))
        assertEquals("owner/repo.name", jarnJingleRepoSlug("https://github.com/owner/repo.name/"))
        assertEquals("owner/repo", jarnJingleRepoSlug("https://www.github.com/owner/repo.git"))
    }

    @Test
    fun `slug rejects other hosts traversal and extras`() {
        listOf(
            "https://evil.test/owner/repo",
            "http://github.com/owner/repo",
            "https://github.com.evil.test/owner/repo",
            "owner/..",
            "../repo",
            "owner/repo/extra",
            "owner/repo?x=1",
            "owner/repo#readme",
            "-owner/repo",
            "",
        ).forEach { assertNull(it, jarnJingleRepoSlug(it)) }
    }

    @Test
    fun `entry paths stay inside the repository and name audio`() {
        assertTrue(jarnJingleEntryPathSafe("jingles/n3ds/theme.MP3"))
        listOf(
            "../../Android/data/x.mp3",
            "jingles/../../x.mp3",
            "/sdcard/x.mp3",
            "jingles\\x.mp3",
            "C:/x.mp3",
            "jingles//x.mp3",
            "jingles/./x.mp3",
            "jingles/payload.apk",
            "jingles/noextension",
            "jingles/a\u0000.mp3",
            "",
        ).forEach { assertFalse(it, jarnJingleEntryPathSafe(it)) }
    }

    @Test
    fun `one unsafe entry rejects the whole index`() {
        assertTrue(jarnJingleIndexSafe(listOf("jingles/a.mp3", "jingles/b.ogg")))
        assertFalse(jarnJingleIndexSafe(listOf("jingles/a.mp3", "../b.ogg")))
        assertFalse(jarnJingleIndexSafe(List(5_001) { "jingles/$it.mp3" }))
    }

    @Test
    fun `pack fields cannot leave their folder`() {
        assertTrue(jarnJinglePackFieldValid("My Pack"))
        assertFalse(jarnJinglePackFieldValid(".."))
        assertFalse(jarnJinglePackFieldValid("a/b"))
        assertTrue(jarnJinglePlatformValid("N3DS"))
        assertFalse(jarnJinglePlatformValid("../n3ds"))
        assertTrue(jarnJingleFileNameValid("theme"))
        assertTrue(jarnJingleFileNameValid("theme.ogg"))
        assertFalse(jarnJingleFileNameValid("theme.apk"))
    }

    @Test
    fun `game key keeps system and file name`() {
        assertEquals("n3ds/game.3ds", jarnJingleGameKey("n3ds", "/roms/3ds/sub/game.3ds"))
        assertEquals("snes/game.sfc", jarnJingleGameKey("snes", "game.sfc"))
    }
}
