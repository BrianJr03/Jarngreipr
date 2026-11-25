package jr.brian.jarnfe.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryLetterJumpTest {
    private val titles = listOf(
        "1942", "3D Tetris", "Asteroids", "Alien", "Bomberman", "Castlevania", "Contra", "Contra III", "Zelda",
    )

    private fun target(list: List<String>, from: Int, forward: Boolean) =
        letterJumpTarget(list.size, from, forward) { letterJumpGroup(list[it]) }

    @Test
    fun `forward lands on the first game of the next letter`() {
        assertEquals(2, target(titles, 0, forward = true))
        assertEquals(4, target(titles, 3, forward = true))
        assertEquals(8, target(titles, 5, forward = true))
    }

    @Test
    fun `backward goes to the start of the letter, then the previous letter`() {
        assertEquals(5, target(titles, 7, forward = false))
        assertEquals(4, target(titles, 5, forward = false))
        assertEquals(2, target(titles, 4, forward = false))
        assertEquals(0, target(titles, 2, forward = false))
    }

    @Test
    fun `jumps stop at the ends instead of wrapping`() {
        assertNull(target(titles, 8, forward = true))
        assertNull(target(titles, 0, forward = false))
    }

    @Test
    fun `digits and symbols group as one letter`() {
        assertEquals("#", letterJumpGroup("1942"))
        assertEquals("#", letterJumpGroup("[BIOS] Something"))
        assertEquals("#", letterJumpGroup(""))
        assertEquals("A", letterJumpGroup("  alien"))
    }

    @Test
    fun `a single letter list goes nowhere forward and to its start backward`() {
        val list = listOf("Mario", "Metroid", "Mega Man")
        assertNull(target(list, 0, forward = true))
        assertNull(target(list, 2, forward = true))
        assertEquals(0, target(list, 2, forward = false))
        assertNull(target(list, 0, forward = false))
    }

    @Test
    fun `an all digit list is one group`() {
        val list = listOf("1942", "1943", "2048", "720")
        assertNull(target(list, 1, forward = true))
        assertEquals(0, target(list, 3, forward = false))
    }

    @Test
    fun `split groups jump run by run`() {
        val list = listOf("Alpha", "_Beta", "Gamma", "Éclair")
        assertEquals(1, target(list, 0, forward = true))
        assertEquals(2, target(list, 1, forward = true))
        assertEquals(3, target(list, 2, forward = true))
    }

    @Test
    fun `a custom title key decides the group`() {
        val dropThe: (String) -> String = { it.removePrefix("The ") }
        assertEquals("L", letterJumpGroup("The Legend of Zelda", dropThe))
        assertEquals("T", letterJumpGroup("The Legend of Zelda"))
        val list = listOf("Kirby", "The Legend of Zelda", "Lemmings", "Mario")
        val target = letterJumpTarget(list.size, 0, forward = true) { letterJumpGroup(list[it], dropThe) }
        assertEquals(1, target)
        assertEquals(3, letterJumpTarget(list.size, 1, forward = true) { letterJumpGroup(list[it], dropThe) })
    }

    @Test
    fun `only a Name sorted feed jumps by letter`() {
        assertTrue(JarnLibraryFeed<String>(sort = JarnGameSort.Name).letterJumpEnabled())
        assertFalse(JarnLibraryFeed<String>(sort = null).letterJumpEnabled())
        JarnGameSort.entries.filter { it != JarnGameSort.Name }.forEach {
            assertFalse(it.name, JarnLibraryFeed<String>(sort = it).letterJumpEnabled())
        }
    }

    @Test
    fun `the feed's key groups titles the way the Name sort orders them`() {
        val key = JarnLibraryFeed<String>(sort = JarnGameSort.Name).letterJumpKey()
        assertEquals("L", letterJumpGroup("The Legend of Zelda", key))
        assertEquals("T", letterJumpGroup("Theme Hospital", key))
        val sorted = listOf("Kirby", "Lemmings", "The Legend of Zelda", "Mario").sortedBy(::nameSortKey)
        assertEquals(listOf("Kirby", "The Legend of Zelda", "Lemmings", "Mario"), sorted)
        assertEquals(3, letterJumpTarget(sorted.size, 1, forward = true) { letterJumpGroup(sorted[it], key) })
    }

    @Test
    fun `out of range and empty lists go nowhere`() {
        assertNull(target(emptyList(), 0, forward = true))
        assertNull(target(titles, -1, forward = true))
    }

    @Test
    fun `pacer steps until the hold threshold, then jumps with a dwell`() {
        val pacer = LetterJumpPacer(holdMs = 1000, dwellMs = 300)
        assertEquals(LetterJumpPace.Step, pacer.onRepeat(heldMs = 500, nowMs = 500))
        assertEquals(LetterJumpPace.Jump, pacer.onRepeat(heldMs = 1000, nowMs = 1000))
        assertEquals(LetterJumpPace.Wait, pacer.onRepeat(heldMs = 1100, nowMs = 1100))
        assertEquals(LetterJumpPace.Jump, pacer.onRepeat(heldMs = 1300, nowMs = 1300))
    }

    @Test
    fun `pacer reset forgets the last jump`() {
        val pacer = LetterJumpPacer(holdMs = 0, dwellMs = 300)
        assertEquals(LetterJumpPace.Jump, pacer.onRepeat(heldMs = 0, nowMs = 0))
        pacer.reset()
        assertEquals(LetterJumpPace.Jump, pacer.onRepeat(heldMs = 0, nowMs = 10))
    }
}
