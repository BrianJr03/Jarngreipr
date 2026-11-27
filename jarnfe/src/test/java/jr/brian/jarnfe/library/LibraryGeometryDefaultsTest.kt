package jr.brian.jarnfe.library

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** The params a theme will drive start at Jarn's own look, so callers that pass none are unchanged. */
class LibraryGeometryDefaultsTest {
    @Test
    fun `systems shelf defaults match the mockup`() {
        assertEquals(
            LibraryShelfGeometry(0.30f, 16f / 10f, 0.62f, 0.02f, SHELF_ONE_LINE_CAPTION, 2, 2, 0.42f),
            LibraryShelfKind.Systems.geometry,
        )
    }

    @Test
    fun `games shelf defaults match the mockup`() {
        assertEquals(
            LibraryShelfGeometry(0.17f, 3f / 4f, 0.70f, 0.016f, SHELF_TWO_LINE_CAPTION, 2, 2, 0.42f),
            LibraryShelfKind.Games.geometry,
        )
    }

    @Test
    fun `default neighbors compose and dim exactly as before`() {
        assertEquals((-3..3).toList(), shelfComposedIndices(center = 3, count = 100).map { it - 3 })
        assertEquals(listOf(0f, 0.55f, 0.55f, 1f, 0.55f, 0.55f, 0f), (-3..3).map { shelfSlotAlpha(it) })
    }

    @Test
    fun `grid defaults keep adaptive columns, the column gap and the theme row gap`() {
        val geometry = LibraryGridGeometry(112.dp)
        assertEquals(GridCells.Adaptive(112.dp), geometry.cells)
        assertEquals(12.dp, geometry.columnSpacing)
        assertEquals(Dp.Unspecified, geometry.rowSpacing)
        assertEquals(GridCells.Fixed(5), geometry.copy(columns = 5).cells)
    }
}
