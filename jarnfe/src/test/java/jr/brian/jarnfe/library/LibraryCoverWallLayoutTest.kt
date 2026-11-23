package jr.brian.jarnfe.library

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryCoverWallLayoutTest {
    private val palette = LibraryCoverWallPalette(base = 1, card = 2, text = 3, overlay = 4)

    private fun art(count: Int) = (1..count).map { LibraryWallCover("/art/$it.png", "Game $it") }

    @Test
    fun `fewer than three real covers means no wall`() {
        val covers = art(2) + LibraryWallCover(null, "No art") + LibraryWallCover("  ", "Blank")
        assertTrue(libraryCoverWallSelection(covers).isEmpty())
    }

    @Test
    fun `duplicate paths count once`() {
        val covers = List(5) { LibraryWallCover("/art/same.png", "Game $it") } + art(1)
        assertTrue(libraryCoverWallSelection(covers).isEmpty())
    }

    @Test
    fun `selection caps at twelve and prefers real covers`() {
        val missing = (1..4).map { LibraryWallCover(null, "Missing $it") }
        val selection = libraryCoverWallSelection(missing + art(20))
        assertEquals(LIBRARY_WALL_MAX_COVERS, selection.size)
        assertTrue(selection.all { it.path != null })
    }

    @Test
    fun `missing covers fill spare slots as title cards`() {
        val selection = libraryCoverWallSelection(listOf(LibraryWallCover(null, "Missing")) + art(3))
        assertEquals(4, selection.size)
        assertNull(selection.last().path)
    }

    @Test
    fun `bitmap size keeps display size until the cap`() {
        assertEquals(LibraryWallSize(1240, 1080), libraryCoverWallBitmapSize(1240, 1080))
        assertEquals(LibraryWallSize(1920, 1080), libraryCoverWallBitmapSize(3840, 2160))
        assertNull(libraryCoverWallBitmapSize(0, 1080))
    }

    @Test
    fun `key changes with system, covers, size and palette`() {
        val covers = art(4)
        val key = libraryCoverWallKey("snes", covers, 1240, 1080, palette)
        assertNotNull(key)
        assertEquals(key, libraryCoverWallKey("snes", art(4), 1240, 1080, palette.copy()))
        assertNotEquals(key, libraryCoverWallKey("nes", covers, 1240, 1080, palette))
        assertNotEquals(key, libraryCoverWallKey("snes", covers.reversed(), 1240, 1080, palette))
        assertNotEquals(key, libraryCoverWallKey("snes", covers, 1920, 1080, palette))
        assertNotEquals(key, libraryCoverWallKey("snes", covers, 1240, 1080, palette.copy(card = 9)))
    }

    @Test
    fun `no key without enough covers or a measured size`() {
        assertNull(libraryCoverWallKey("snes", art(2), 1240, 1080, palette))
        assertNull(libraryCoverWallKey("snes", art(4), 0, 0, palette))
    }

    @Test
    fun `neighboring cells never share a cover`() {
        for (coverCount in LIBRARY_WALL_MIN_COVERS..LIBRARY_WALL_MAX_COVERS) {
            for (row in -6..6) {
                val neighborShift = if (row % 2 != 0) 0..1 else -1..0
                for (column in -8..8) {
                    val index = libraryWallCoverIndex(column, row, coverCount)
                    assertNotEquals(index, libraryWallCoverIndex(column + 1, row, coverCount))
                    for (shift in neighborShift) {
                        assertNotEquals(index, libraryWallCoverIndex(column + shift, row + 1, coverCount))
                    }
                }
            }
        }
    }

    @Test
    fun `wall leaves no uncovered area on any display shape`() {
        listOf(
            LibraryWallSize(1920, 1080),
            LibraryWallSize(1240, 1080),
            LibraryWallSize(1080, 1920),
            LibraryWallSize(640, 480),
        ).forEach { size ->
            val layout = libraryCoverWallLayout(size, coverCount = 5)
            val radians = Math.toRadians(-layout.rotationDegrees.toDouble())
            for (xStep in 0..24) for (yStep in 0..24) {
                val x = size.width * (xStep / 24f - 0.5f)
                val y = size.height * (yStep / 24f - 0.5f)
                val wallX = (x * cos(radians) - y * sin(radians)).toFloat()
                val wallY = (x * sin(radians) + y * cos(radians)).toFloat()
                val covered = layout.cells.any { cell ->
                    abs(wallX - cell.centerX) <= (layout.cellWidth + layout.gap) / 2f + 1f &&
                        abs(wallY - cell.centerY) <= (layout.cellHeight + layout.gap) / 2f + 1f
                }
                assertTrue("gap at $x,$y on $size", covered)
            }
        }
    }

    @Test
    fun `cell count stays bounded and indices stay in range`() {
        val layout = libraryCoverWallLayout(LibraryWallSize(1920, 1080), coverCount = 7)
        assertTrue(layout.cells.size in 1..120)
        assertTrue(layout.cells.all { it.coverIndex in 0 until 7 })
    }

    @Test
    fun `no covers means no cells`() {
        assertTrue(libraryCoverWallLayout(LibraryWallSize(1920, 1080), coverCount = 0).cells.isEmpty())
    }
}
