package jr.brian.jarnfe.canvas

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val CANVAS_UNDO_WINDOW_MS = 6_000L

/** Long enough for the saved layout to arrive, so a finished removal never flashes back. */
internal const val CANVAS_REMOVAL_HIDE_MS = 1_000L

internal enum class CanvasUndoKind { Moved, Resized, Removed }

internal sealed interface CanvasUndoEntry {
    val pageIndex: Int
    val kind: CanvasUndoKind
}

/** Whole arrangements, so an undo also returns every tile the solver pushed. */
internal data class CanvasLayoutChange(
    override val pageIndex: Int,
    val orientation: CanvasScrollOrientation,
    val before: Map<String, GridRect>,
    val after: Map<String, GridRect>,
    override val kind: CanvasUndoKind,
) : CanvasUndoEntry

/** The tile is still saved while this exists, so losing the process simply brings it back. */
internal data class CanvasPendingRemoval(
    override val pageIndex: Int,
    val itemId: String,
) : CanvasUndoEntry {
    override val kind: CanvasUndoKind get() = CanvasUndoKind.Removed
}

/** Already saved on both pages. Undo restores them, and letting the window end changes nothing. */
internal data class CanvasTileMoveEntry(val moved: JarnCanvasTileMoved) : CanvasUndoEntry {
    override val pageIndex: Int get() = moved.move.toPage
    override val kind: CanvasUndoKind get() = CanvasUndoKind.Moved
}

internal interface CanvasUndoWrites {
    fun commit(pageIndex: Int, orientation: CanvasScrollOrientation, placements: Map<String, GridRect>)

    suspend fun remove(pageIndex: Int, itemId: String)

    suspend fun moveTile(move: JarnCanvasTileMove): JarnCanvasTileMoved? = null

    suspend fun undoTileMove(moved: JarnCanvasTileMoved): Boolean = false
}

internal class CanvasDataUndoWrites(private val data: JarnCanvasData) : CanvasUndoWrites {
    override fun commit(
        pageIndex: Int,
        orientation: CanvasScrollOrientation,
        placements: Map<String, GridRect>,
    ) = data.commitPlacements(pageIndex, orientation, placements)

    override suspend fun remove(pageIndex: Int, itemId: String) = data.removeTile(pageIndex, itemId)

    override suspend fun moveTile(move: JarnCanvasTileMove) = data.moveTile(move)

    override suspend fun undoTileMove(moved: JarnCanvasTileMoved) = data.undoTileMove(moved)
}

internal fun canvasUndoKind(
    itemId: String,
    before: Map<String, GridRect>,
    after: Map<String, GridRect>,
): CanvasUndoKind {
    val old = before[itemId]
    val new = after[itemId]
    val sameSize = old?.colSpan == new?.colSpan && old?.rowSpan == new?.rowSpan
    return if (sameSize) CanvasUndoKind.Moved else CanvasUndoKind.Resized
}

/** True when every tile both maps know about sits in the same place. */
internal fun canvasPlacementsAgree(first: Map<String, GridRect>, second: Map<String, GridRect>): Boolean =
    first.all { (id, rect) -> second[id]?.let { it == rect } ?: true }

/**
 * One level of undo, held in memory only. A removal writes nothing until its window ends, and runs
 * in [scope], which must outlive every screen so leaving Canvas can never stop it partway.
 */
@Stable
internal class CanvasUndoController(
    private val scope: CoroutineScope,
    private val windowMs: Long = CANVAS_UNDO_WINDOW_MS,
    private val removalHideMs: Long = CANVAS_REMOVAL_HIDE_MS,
) {
    var entry by mutableStateOf<CanvasUndoEntry?>(null)
        private set

    private val finishing = mutableStateListOf<CanvasPendingRemoval>()
    private var writes: CanvasUndoWrites? = null
    private var timer: Job? = null

    fun hiddenOn(pageIndex: Int): Set<String> = buildSet {
        (entry as? CanvasPendingRemoval)?.takeIf { it.pageIndex == pageIndex }?.let { add(it.itemId) }
        finishing.forEach { if (it.pageIndex == pageIndex) add(it.itemId) }
    }

    fun recordLayout(target: CanvasUndoWrites, change: CanvasLayoutChange) {
        settle()
        open(target, change)
    }

    fun removeTile(target: CanvasUndoWrites, pageIndex: Int, itemId: String) {
        settle()
        open(target, CanvasPendingRemoval(pageIndex, itemId))
    }

    /**
     * Runs in [scope], so leaving Canvas can't stop it between the two pages. [onFinished] gets the
     * saved move, or null when it failed, and an undo window opens for a move that succeeded.
     */
    fun moveTile(target: CanvasUndoWrites, move: JarnCanvasTileMove, onFinished: (JarnCanvasTileMoved?) -> Unit) {
        settle()
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val moved = runCatching { target.moveTile(move) }.getOrNull()
            if (moved != null) open(target, CanvasTileMoveEntry(moved))
            onFinished(moved)
        }
    }

    /** A move between pages is the one edit that stays undoable from another page. */
    fun settleForPageChange() {
        if (entry !is CanvasTileMoveEntry) settle()
    }

    /**
     * Returns the arrangement to show, or null when nothing needs placing. A layout that changed
     * some other way is left alone, because restoring over it would lose that newer edit.
     * [onMoveUndone] reports whether a between-page move was restored.
     */
    fun undo(
        current: Map<String, GridRect>,
        onMoveUndone: (JarnCanvasTileMoved, Boolean) -> Unit = { _, _ -> },
    ): Map<String, GridRect>? {
        val undone = entry ?: return null
        val target = writes
        close()
        if (undone is CanvasTileMoveEntry && target != null) {
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                val restored = runCatching { target.undoTileMove(undone.moved) }.getOrDefault(false)
                onMoveUndone(undone.moved, restored)
            }
            return null
        }
        if (undone !is CanvasLayoutChange || target == null) return null
        val untouched = canvasPlacementsAgree(undone.after, current) || canvasPlacementsAgree(undone.before, current)
        if (!untouched) return null
        target.commit(undone.pageIndex, undone.orientation, undone.before)
        return undone.before
    }

    /** Ends the window now. Safe to call from any exit, any number of times. */
    fun settle() {
        val ended = entry
        val target = writes
        close()
        if (ended is CanvasPendingRemoval && target != null) finishRemoval(target, ended)
    }

    private fun open(target: CanvasUndoWrites, next: CanvasUndoEntry) {
        writes = target
        entry = next
        timer = scope.launch {
            delay(windowMs)
            settle()
        }
    }

    private fun close() {
        timer?.cancel()
        timer = null
        entry = null
        writes = null
    }

    /** Starts at once, so it takes the edit lock ahead of any page edit that follows this exit. */
    private fun finishRemoval(target: CanvasUndoWrites, removal: CanvasPendingRemoval) {
        finishing += removal
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                runCatching { target.remove(removal.pageIndex, removal.itemId) }
                delay(removalHideMs)
            } finally {
                finishing -= removal
            }
        }
    }
}
