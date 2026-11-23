package jr.brian.jarnfe.canvas

import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CanvasUndoTest {
    private val before = mapOf("a" to GridRect(0, 0, 1, 1), "b" to GridRect(1, 0, 1, 1))
    private val after = mapOf("a" to GridRect(1, 0, 1, 1), "b" to GridRect(2, 0, 1, 1))

    private class FakeWrites : CanvasUndoWrites {
        val commits = mutableListOf<Map<String, GridRect>>()
        val removals = mutableListOf<Pair<Int, String>>()
        var failRemoval = false

        override fun commit(
            pageIndex: Int,
            orientation: CanvasScrollOrientation,
            placements: Map<String, GridRect>,
        ) {
            commits += placements
        }

        override suspend fun remove(pageIndex: Int, itemId: String) {
            check(!failRemoval)
            removals += pageIndex to itemId
        }
    }

    private fun change() = CanvasLayoutChange(
        pageIndex = 0,
        orientation = CanvasScrollOrientation.HORIZONTAL,
        before = before,
        after = after,
        kind = CanvasUndoKind.Moved,
    )

    private fun TestScope.controller() = CanvasUndoController(backgroundScope)

    @Test
    fun `removal writes nothing until the window ends`() = runTest {
        val writes = FakeWrites()
        val undo = controller()
        undo.removeTile(writes, 0, "a")
        advanceTimeBy(CANVAS_UNDO_WINDOW_MS - 1)
        runCurrent()
        assertTrue(writes.removals.isEmpty())
        assertEquals(setOf("a"), undo.hiddenOn(0))
        advanceTimeBy(2)
        runCurrent()
        assertEquals(listOf(0 to "a"), writes.removals)
        assertNull(undo.entry)
    }

    @Test
    fun `undo of a removal never touches storage`() = runTest {
        val writes = FakeWrites()
        val undo = controller()
        undo.removeTile(writes, 0, "a")
        undo.undo(before)
        advanceTimeBy(CANVAS_UNDO_WINDOW_MS * 2)
        runCurrent()
        assertTrue(writes.removals.isEmpty())
        assertTrue(writes.commits.isEmpty())
        assertTrue(undo.hiddenOn(0).isEmpty())
    }

    @Test
    fun `every exit finishes the removal exactly once`() = runTest {
        val writes = FakeWrites()
        val undo = controller()
        undo.removeTile(writes, 0, "a")
        undo.settle()
        undo.settle()
        advanceTimeBy(CANVAS_UNDO_WINDOW_MS * 2)
        runCurrent()
        assertEquals(listOf(0 to "a"), writes.removals)
    }

    @Test
    fun `a second removal finishes the first`() = runTest {
        val writes = FakeWrites()
        val undo = controller()
        undo.removeTile(writes, 0, "a")
        undo.removeTile(writes, 0, "b")
        runCurrent()
        assertEquals(listOf(0 to "a"), writes.removals)
        assertEquals(setOf("a", "b"), undo.hiddenOn(0))
    }

    @Test
    fun `a new layout change finishes a pending removal`() = runTest {
        val writes = FakeWrites()
        val undo = controller()
        undo.removeTile(writes, 0, "a")
        undo.recordLayout(writes, change())
        runCurrent()
        assertEquals(listOf(0 to "a"), writes.removals)
    }

    @Test
    fun `a finished tile stays hidden until the saved layout can arrive`() = runTest {
        val writes = FakeWrites()
        val undo = controller()
        undo.removeTile(writes, 0, "a")
        undo.settle()
        runCurrent()
        assertEquals(setOf("a"), undo.hiddenOn(0))
        advanceTimeBy(CANVAS_REMOVAL_HIDE_MS + 1)
        runCurrent()
        assertTrue(undo.hiddenOn(0).isEmpty())
    }

    @Test
    fun `a failed removal brings the tile back`() = runTest {
        val writes = FakeWrites().apply { failRemoval = true }
        val undo = controller()
        undo.removeTile(writes, 0, "a")
        undo.settle()
        advanceTimeBy(CANVAS_REMOVAL_HIDE_MS + 1)
        runCurrent()
        assertTrue(undo.hiddenOn(0).isEmpty())
    }

    @Test
    fun `undo of a move restores pushed neighbors`() = runTest {
        val writes = FakeWrites()
        val undo = controller()
        undo.recordLayout(writes, change())
        assertEquals(before, undo.undo(after))
        assertEquals(listOf(before), writes.commits)
    }

    @Test
    fun `undo before the saved layout arrives still restores`() = runTest {
        val writes = FakeWrites()
        val undo = controller()
        undo.recordLayout(writes, change())
        assertEquals(before, undo.undo(before))
        assertEquals(listOf(before), writes.commits)
    }

    @Test
    fun `undo leaves a layout that changed some other way`() = runTest {
        val writes = FakeWrites()
        val undo = controller()
        undo.recordLayout(writes, change())
        assertNull(undo.undo(mapOf("a" to GridRect(5, 5, 1, 1), "b" to GridRect(6, 5, 1, 1))))
        assertTrue(writes.commits.isEmpty())
    }

    @Test
    fun `undo expires with its window`() = runTest {
        val writes = FakeWrites()
        val undo = controller()
        undo.recordLayout(writes, change())
        advanceTimeBy(CANVAS_UNDO_WINDOW_MS + 1)
        runCurrent()
        assertNull(undo.entry)
        assertNull(undo.undo(after))
    }

    @Test
    fun `undo kind tells a resize from a move`() {
        assertEquals(CanvasUndoKind.Moved, canvasUndoKind("a", before, after))
        val grown = before + ("a" to GridRect(0, 0, 2, 1))
        assertEquals(CanvasUndoKind.Resized, canvasUndoKind("a", before, grown))
    }
}
