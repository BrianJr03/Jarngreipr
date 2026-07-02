package jr.brian.home.canvas.grid

import jr.brian.home.canvas.model.GridCell
import jr.brian.home.canvas.model.GridRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GridSolverTest {

    private fun snapshot(
        crossAxisCount: Int = 4,
        pushDirection: PushDirection = PushDirection.DOWN,
        vararg items: Pair<String, GridRect>
    ): LayoutSnapshot = LayoutSnapshot(
        GridConfig(crossAxisCount, pushDirection),
        items.toMap()
    )

    private fun r(col: Int, row: Int, colSpan: Int = 1, rowSpan: Int = 1) =
        GridRect(col, row, colSpan, rowSpan)

    private fun LayoutSnapshot.hasOverlaps(): Boolean {
        val list = placements.entries.toList()
        for (i in list.indices) {
            for (j in i + 1 until list.size) {
                if (list[i].value.overlaps(list[j].value)) return true
            }
        }
        return false
    }

    // ----------------------------------------------------------------------
    // MOVE
    // ----------------------------------------------------------------------

    @Test
    fun `move into empty cell does not displace neighbors`() {
        val base = snapshot(
            items = arrayOf("anchor" to r(0, 0), "other" to r(3, 3))
        )
        val result = GridSolver.solveMove(base, "anchor", GridCell(1, 0))
        assertEquals(r(1, 0), result.snapshot.placements["anchor"])
        assertEquals(r(3, 3), result.snapshot.placements["other"])
        assertEquals(setOf("anchor"), result.movedIds)
        assertFalse(result.snapshot.hasOverlaps())
    }

    @Test
    fun `move that overlaps neighbor pushes it along push axis`() {
        val base = snapshot(items = arrayOf("anchor" to r(0, 0), "n" to r(2, 0)))
        val result = GridSolver.solveMove(base, "anchor", GridCell(2, 0))
        assertEquals(r(2, 0), result.snapshot.placements["anchor"])
        assertEquals(r(2, 1), result.snapshot.placements["n"])
        assertTrue(result.movedIds.contains("anchor"))
        assertTrue(result.movedIds.contains("n"))
        assertFalse(result.snapshot.hasOverlaps())
    }

    @Test
    fun `move cascades through chain of three neighbors`() {
        val base = snapshot(
            items = arrayOf(
                "anchor" to r(0, 0),
                "a" to r(0, 1),
                "b" to r(0, 2),
                "c" to r(0, 3)
            )
        )
        val result = GridSolver.solveMove(base, "anchor", GridCell(0, 1))
        assertEquals(r(0, 1), result.snapshot.placements["anchor"])
        assertEquals(r(0, 2), result.snapshot.placements["a"])
        assertEquals(r(0, 3), result.snapshot.placements["b"])
        assertEquals(r(0, 4), result.snapshot.placements["c"])
        assertFalse(result.snapshot.hasOverlaps())
    }

    @Test
    fun `move to current position is a no-op`() {
        val base = snapshot(items = arrayOf("anchor" to r(0, 0), "other" to r(2, 0)))
        val result = GridSolver.solveMove(base, "anchor", GridCell(0, 0))
        assertEquals(base.placements, result.snapshot.placements)
        assertEquals(emptySet<String>(), result.movedIds)
    }

    @Test
    fun `move with unknown id is a no-op`() {
        val base = snapshot(items = arrayOf("a" to r(0, 0)))
        val result = GridSolver.solveMove(base, "missing", GridCell(2, 2))
        assertEquals(base.placements, result.snapshot.placements)
        assertEquals(emptySet<String>(), result.movedIds)
    }

    @Test
    fun `move clamps target to cross-axis bounds`() {
        val base = snapshot(
            crossAxisCount = 4,
            items = arrayOf("anchor" to r(0, 0, colSpan = 2))
        )
        val result = GridSolver.solveMove(base, "anchor", GridCell(10, 0))
        assertEquals(r(2, 0, colSpan = 2), result.snapshot.placements["anchor"])
    }

    @Test
    fun `move negative target origin is clamped to zero`() {
        val base = snapshot(items = arrayOf("anchor" to r(2, 2)))
        val result = GridSolver.solveMove(base, "anchor", GridCell(-3, -5))
        assertEquals(r(0, 0), result.snapshot.placements["anchor"])
    }

    // ----------------------------------------------------------------------
    // RESIZE
    // ----------------------------------------------------------------------

    @Test
    fun `resize pushes single overlapped neighbor`() {
        val base = snapshot(
            items = arrayOf("anchor" to r(0, 0), "n" to r(0, 1))
        )
        val result = GridSolver.solveResize(base, "anchor", r(0, 0, 1, 2))
        assertEquals(r(0, 0, 1, 2), result.snapshot.placements["anchor"])
        assertEquals(r(0, 2), result.snapshot.placements["n"])
    }

    @Test
    fun `resize cascades through three or more neighbors`() {
        val base = snapshot(
            items = arrayOf(
                "anchor" to r(0, 0),
                "a" to r(0, 1),
                "b" to r(0, 2),
                "c" to r(0, 3)
            )
        )
        val result = GridSolver.solveResize(base, "anchor", r(0, 0, 1, 2))
        assertEquals(r(0, 0, 1, 2), result.snapshot.placements["anchor"])
        assertEquals(r(0, 2), result.snapshot.placements["a"])
        assertEquals(r(0, 3), result.snapshot.placements["b"])
        assertEquals(r(0, 4), result.snapshot.placements["c"])
    }

    @Test
    fun `resize clamps to cross-axis bound`() {
        val base = snapshot(crossAxisCount = 4, items = arrayOf("a" to r(0, 0)))
        val result = GridSolver.solveResize(base, "a", r(0, 0, colSpan = 10, rowSpan = 1))
        assertEquals(r(0, 0, colSpan = 4, rowSpan = 1), result.snapshot.placements["a"])
    }

    @Test
    fun `resize honors minimum span`() {
        val base = snapshot(items = arrayOf("a" to r(0, 0, 3, 3)))
        val result = GridSolver.solveResize(
            base,
            "a",
            r(0, 0, colSpan = 1, rowSpan = 1),
            minColSpan = 2,
            minRowSpan = 2
        )
        assertEquals(r(0, 0, 2, 2), result.snapshot.placements["a"])
    }

    @Test
    fun `resize minimum span is capped by cross-axis count`() {
        val base = snapshot(crossAxisCount = 3, items = arrayOf("a" to r(0, 0)))
        val result = GridSolver.solveResize(
            base, "a", r(0, 0, 1, 1), minColSpan = 5, minRowSpan = 1
        )
        assertEquals(3, result.snapshot.placements["a"]?.colSpan)
    }

    @Test
    fun `resize smaller leaves gap and does not pull neighbors back`() {
        val base = snapshot(
            items = arrayOf("anchor" to r(0, 0, 1, 3), "n" to r(0, 3))
        )
        val result = GridSolver.solveResize(base, "anchor", r(0, 0, 1, 1))
        assertEquals(r(0, 0, 1, 1), result.snapshot.placements["anchor"])
        assertEquals(r(0, 3), result.snapshot.placements["n"])
    }

    @Test
    fun `resize over multiple same-row neighbors pushes them all once`() {
        val base = snapshot(
            crossAxisCount = 4,
            items = arrayOf(
                "anchor" to r(0, 0, 1, 1),
                "a" to r(0, 1, 2, 1),
                "b" to r(2, 1, 2, 1)
            )
        )
        val result = GridSolver.solveResize(base, "anchor", r(0, 0, 4, 2))
        assertEquals(r(0, 0, 4, 2), result.snapshot.placements["anchor"])
        assertEquals(r(0, 2, 2, 1), result.snapshot.placements["a"])
        assertEquals(r(2, 2, 2, 1), result.snapshot.placements["b"])
        assertFalse(result.snapshot.hasOverlaps())
    }

    @Test
    fun `resize over neighbors at different rows resolves cascade`() {
        val base = snapshot(
            items = arrayOf(
                "anchor" to r(0, 0, 2, 1),
                "a" to r(0, 1, 1, 1),
                "b" to r(0, 2, 1, 1)
            )
        )
        val result = GridSolver.solveResize(base, "anchor", r(0, 0, 2, 3))
        assertEquals(r(0, 0, 2, 3), result.snapshot.placements["anchor"])
        // a pushed to row 3; b also pushed to row 3, then cascade past a
        assertEquals(r(0, 3), result.snapshot.placements["a"])
        assertEquals(r(0, 4), result.snapshot.placements["b"])
        assertFalse(result.snapshot.hasOverlaps())
    }

    // ----------------------------------------------------------------------
    // ORIENTATION
    // ----------------------------------------------------------------------

    @Test
    fun `horizontal scroll pushes right`() {
        val base = snapshot(
            crossAxisCount = 4,
            pushDirection = PushDirection.RIGHT,
            items = arrayOf("anchor" to r(0, 0), "n" to r(1, 0))
        )
        val result = GridSolver.solveMove(base, "anchor", GridCell(1, 0))
        assertEquals(r(1, 0), result.snapshot.placements["anchor"])
        assertEquals(r(2, 0), result.snapshot.placements["n"])
    }

    @Test
    fun `horizontal resize cascades right`() {
        val base = snapshot(
            crossAxisCount = 4,
            pushDirection = PushDirection.RIGHT,
            items = arrayOf("anchor" to r(0, 0), "a" to r(1, 0), "b" to r(2, 0))
        )
        val result = GridSolver.solveResize(base, "anchor", r(0, 0, 2, 1))
        assertEquals(r(0, 0, 2, 1), result.snapshot.placements["anchor"])
        assertEquals(r(2, 0), result.snapshot.placements["a"])
        assertEquals(r(3, 0), result.snapshot.placements["b"])
    }

    @Test
    fun `horizontal scroll clamps row instead of column`() {
        val base = snapshot(
            crossAxisCount = 3,
            pushDirection = PushDirection.RIGHT,
            items = arrayOf("a" to r(0, 0, rowSpan = 2))
        )
        val result = GridSolver.solveMove(base, "a", GridCell(0, 10))
        assertEquals(1, result.snapshot.placements["a"]?.row)
    }

    // ----------------------------------------------------------------------
    // DETERMINISM, BACK-OFF, IDEMPOTENCE
    // ----------------------------------------------------------------------

    @Test
    fun `solveResize is referentially transparent`() {
        val base = snapshot(
            items = arrayOf("anchor" to r(0, 0), "a" to r(0, 1), "b" to r(0, 2))
        )
        val r1 = GridSolver.solveResize(base, "anchor", r(0, 0, 1, 2))
        val r2 = GridSolver.solveResize(base, "anchor", r(0, 0, 1, 2))
        assertEquals(r1.snapshot.placements, r2.snapshot.placements)
        assertEquals(r1.movedIds, r2.movedIds)
    }

    @Test
    fun `backing off mid-gesture restores baseline`() {
        val base = snapshot(items = arrayOf("anchor" to r(0, 0), "a" to r(0, 1)))
        val expanded = GridSolver.solveResize(base, "anchor", r(0, 0, 1, 2))
        assertEquals(r(0, 2), expanded.snapshot.placements["a"])
        // Re-solve from baseline with original rect — everything reverts.
        val restored = GridSolver.solveResize(base, "anchor", r(0, 0, 1, 1))
        assertEquals(base.placements, restored.snapshot.placements)
        assertEquals(emptySet<String>(), restored.movedIds)
    }

    @Test
    fun `repeated solves do not drift`() {
        val base = snapshot(items = arrayOf("anchor" to r(0, 0), "a" to r(0, 1)))
        val s1 = GridSolver.solveResize(base, "anchor", r(0, 0, 1, 2))
        val s2 = GridSolver.solveResize(base, "anchor", r(0, 0, 1, 2))
        val s3 = GridSolver.solveResize(base, "anchor", r(0, 0, 1, 2))
        assertEquals(s1.snapshot.placements, s2.snapshot.placements)
        assertEquals(s2.snapshot.placements, s3.snapshot.placements)
    }

    @Test
    fun `pushed item never returns to its starting cell within a single solve`() {
        // tightly packed column: any push past one cell must keep going
        val base = snapshot(
            items = arrayOf(
                "anchor" to r(0, 0),
                "a" to r(0, 1),
                "b" to r(0, 2),
                "c" to r(0, 3),
                "d" to r(0, 4)
            )
        )
        val result = GridSolver.solveResize(base, "anchor", r(0, 0, 1, 3))
        val final = result.snapshot.placements
        // Anchor occupies rows 0..2 → a,b,c,d must all sit at row >= 3
        listOf("a", "b", "c", "d").forEach { id ->
            val row = final[id]?.row ?: error("missing $id")
            assertTrue("$id row=$row should be >= 3", row >= 3)
        }
        assertFalse(result.snapshot.hasOverlaps())
    }

    // ----------------------------------------------------------------------
    // COMPACT
    // ----------------------------------------------------------------------

    @Test
    fun `compact closes gaps along push axis`() {
        val base = snapshot(
            items = arrayOf("a" to r(0, 0), "b" to r(0, 3), "c" to r(0, 5))
        )
        val compacted = GridSolver.compact(base)
        assertEquals(r(0, 0), compacted.placements["a"])
        assertEquals(r(0, 1), compacted.placements["b"])
        assertEquals(r(0, 2), compacted.placements["c"])
    }

    @Test
    fun `compact preserves cross-axis position`() {
        val base = snapshot(items = arrayOf("a" to r(0, 2), "b" to r(2, 5)))
        val compacted = GridSolver.compact(base)
        assertEquals(r(0, 0), compacted.placements["a"])
        assertEquals(r(2, 0), compacted.placements["b"])
    }

    @Test
    fun `compact preserves stable order at same push-axis position`() {
        val base = snapshot(
            items = arrayOf("a" to r(0, 4), "b" to r(0, 5), "c" to r(0, 6))
        )
        val compacted = GridSolver.compact(base)
        assertEquals(r(0, 0), compacted.placements["a"])
        assertEquals(r(0, 1), compacted.placements["b"])
        assertEquals(r(0, 2), compacted.placements["c"])
    }

    @Test
    fun `compact is idempotent`() {
        val base = snapshot(
            items = arrayOf("a" to r(0, 3), "b" to r(0, 7), "c" to r(1, 4))
        )
        val once = GridSolver.compact(base)
        val twice = GridSolver.compact(once)
        assertEquals(once.placements, twice.placements)
    }

    @Test
    fun `compact pulls left on horizontal scroll`() {
        val base = snapshot(
            crossAxisCount = 4,
            pushDirection = PushDirection.RIGHT,
            items = arrayOf("a" to r(3, 0), "b" to r(7, 0))
        )
        val compacted = GridSolver.compact(base)
        assertEquals(r(0, 0), compacted.placements["a"])
        assertEquals(r(1, 0), compacted.placements["b"])
    }

    @Test
    fun `compact respects multi-cell rects`() {
        val base = snapshot(
            crossAxisCount = 4,
            items = arrayOf(
                "wide" to r(0, 5, colSpan = 4, rowSpan = 1),
                "tall" to r(0, 8, colSpan = 1, rowSpan = 2)
            )
        )
        val compacted = GridSolver.compact(base)
        assertEquals(r(0, 0, 4, 1), compacted.placements["wide"])
        assertEquals(r(0, 1, 1, 2), compacted.placements["tall"])
    }

    // ----------------------------------------------------------------------
    // INVARIANTS
    // ----------------------------------------------------------------------

    @Test
    fun `final layout from every solve is overlap-free`() {
        val base = snapshot(
            crossAxisCount = 4,
            items = arrayOf(
                "anchor" to r(0, 0),
                "a" to r(0, 1),
                "b" to r(2, 1),
                "c" to r(0, 2, colSpan = 3),
                "d" to r(3, 2)
            )
        )
        val resize = GridSolver.solveResize(base, "anchor", r(0, 0, 3, 2))
        assertFalse(resize.snapshot.hasOverlaps())
        val move = GridSolver.solveMove(base, "anchor", GridCell(2, 1))
        assertFalse(move.snapshot.hasOverlaps())
    }

    @Test
    fun `moved set lists exactly the displaced ids`() {
        val base = snapshot(
            items = arrayOf("anchor" to r(0, 0), "a" to r(0, 1), "far" to r(3, 5))
        )
        val result = GridSolver.solveResize(base, "anchor", r(0, 0, 1, 2))
        assertEquals(setOf("anchor", "a"), result.movedIds)
        assertEquals(r(3, 5), result.snapshot.placements["far"])
    }
}
