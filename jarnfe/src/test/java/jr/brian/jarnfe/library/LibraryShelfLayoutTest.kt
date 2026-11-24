package jr.brian.jarnfe.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryShelfLayoutTest {
    @Test
    fun `four or more items wrap both ways`() {
        assertEquals(4, shelfStep(0, -1, 5))
        assertEquals(0, shelfStep(4, 1, 5))
        assertEquals(0, shelfStep(3, 1, 4))
    }

    @Test
    fun `fewer than four items stop at the ends`() {
        assertEquals(0, shelfStep(0, -1, 3))
        assertEquals(2, shelfStep(2, 1, 3))
        assertEquals(0, shelfStep(0, 1, 1))
    }

    @Test
    fun `distance takes the short way round a wrapping shelf`() {
        assertEquals(-1, shelfDistance(9, 0, 10))
        assertEquals(1, shelfDistance(0, 9, 10))
        assertEquals(2, shelfDistance(2, 0, 3))
        assertEquals(-2, shelfDistance(0, 2, 3))
    }

    @Test
    fun `only items near the center are composed`() {
        val indices = shelfComposedIndices(center = 500, count = 5000)
        assertEquals((497..503).toList(), indices)
        assertEquals(listOf(4997, 4998, 4999, 0, 1, 2, 3), shelfComposedIndices(0, 5000))
    }

    @Test
    fun `short shelves list each item once and never repeat ghosts`() {
        assertEquals(listOf(0, 1, 2), shelfComposedIndices(0, 3))
        assertEquals(setOf(0, 1, 2, 3), shelfComposedIndices(0, 4).toSet())
        assertEquals(4, shelfComposedIndices(0, 4).size)
        assertEquals(listOf(0), shelfComposedIndices(0, 1))
    }

    @Test
    fun `neighbors sit beside the center card, smaller and dimmer`() {
        val kind = LibraryShelfKind.Games
        assertEquals(0f, shelfSlotX(0, 1000f, kind, card = 170f), 0f)
        val first = shelfSlotX(1, 1000f, kind, card = 170f)
        assertEquals(85f + 16f + 59.5f, first, 0.01f)
        assertEquals(-first, shelfSlotX(-1, 1000f, kind, card = 170f), 0.01f)
        assertEquals(kind.neighborScale, shelfSlotScale(2, kind))
        assertEquals(SHELF_NEIGHBOR_ALPHA, shelfSlotAlpha(-2))
        assertEquals(0f, shelfSlotAlpha(3))
    }

    @Test
    fun `a tall shelf keeps the mockup width and sits near the mockup center`() {
        val fit = shelfFit(1000f, 600f, LibraryShelfKind.Games, narrow = false, topMargin = 20f, captionReserve = 72f)
        assertEquals(170f, fit.cardWidth, 0.01f)
        assertEquals(600f * SHELF_CENTER_Y, fit.centerY, 0.01f)
    }

    @Test
    fun `a short shelf under the continue card shrinks so card and caption stay inside`() {
        val height = 200f
        val fit = shelfFit(1000f, height, LibraryShelfKind.Systems, narrow = false, topMargin = 20f, captionReserve = 72f)
        assertTrue(fit.centerY - fit.cardHeight / 2 >= 20f - 0.01f)
        assertTrue(fit.centerY + fit.cardHeight / 2 + 72f <= height + 0.01f)
        assertEquals(fit.cardHeight * LibraryShelfKind.Systems.aspectRatio, fit.cardWidth, 0.01f)
    }

    @Test
    fun `no room at all leaves an empty card rather than overlapping`() {
        val fit = shelfFit(1000f, 60f, LibraryShelfKind.Games, narrow = false, topMargin = 20f, captionReserve = 72f)
        assertEquals(0f, fit.cardHeight, 0f)
    }

    @Test
    fun `a wrap jumps instead of sliding across the row`() {
        assertTrue(shelfSlotJumps(-2, 2))
        assertFalse(shelfSlotJumps(1, 0))
        assertFalse(shelfSlotJumps(null, 3))
    }

    @Test
    fun `badge letter is upper case, anything else reads as a hash`() {
        assertEquals("Z", shelfBadgeLetter("zelda"))
        assertEquals("#", shelfBadgeLetter("1942"))
        assertEquals("#", shelfBadgeLetter(""))
    }
}
