package jr.brian.jarnfe.canvas

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.GridCell
import jr.brian.home.canvas.model.GridRect

/** What the Arrange banner should tell the user right now. */
internal enum class CanvasArrangeStep { Choose, HoldingWithController, HoldingWithTouch }

/**
 * Edit state, owned above the page so one grab survives a page change. It is never saved, so
 * Classic's edit flag stays untouched. [navigate] moves the visible page on the grab's behalf.
 */
@Stable
internal class CanvasEditor(private val navigate: (Int) -> Unit = {}) {
    var editing by mutableStateOf(false)
        private set

    var grab by mutableStateOf<CanvasGrab?>(null)
        private set

    /** True in the lasting Arrange mode, false during a one-shot move that returns to browsing. */
    val arranging: Boolean
        get() = editing && grab?.leaveEditOnEnd != true

    val arrangeStep: CanvasArrangeStep?
        get() {
            if (!arranging) return null
            return when (grab?.driver) {
                null -> CanvasArrangeStep.Choose
                CanvasGrabDriver.Controller -> CanvasArrangeStep.HoldingWithController
                CanvasGrabDriver.Touch -> CanvasArrangeStep.HoldingWithTouch
            }
        }

    /** Holds a dropped layout on screen until the saved one arrives, so tiles never jump back. */
    var settled by mutableStateOf<Map<String, GridRect>?>(null)

    /** Changes only when the page changed without a carried tile, so a held finger survives a flip. */
    var surfaceKey by mutableIntStateOf(0)
        private set

    private var shownPage by mutableStateOf<Int?>(null)
    private var visiblePushCells by mutableIntStateOf(0)

    fun placements(layout: CanvasLayout): Map<String, GridRect> =
        grab?.preview?.placements ?: settled ?: layout.activeArrangement

    /**
     * Called while composing, before anything reads this editor, so a new page never draws a frame
     * of the old page's state. A page change the grab didn't ask for ends the edit session.
     */
    fun showPage(index: Int?) = Snapshot.withoutReadObservation {
        if (index == shownPage) return@withoutReadObservation
        val carried = grab?.takeIf { it.pageIndex == index }
        shownPage = index
        settled = null
        if (carried == null) {
            surfaceKey += 1
            grab = null
            editing = false
            visiblePushCells = 0
        }
    }

    /** Whole cells the viewport shows along the scroll axis. A grab away from home tracks them. */
    fun reportVisibleCells(cells: Int) {
        visiblePushCells = cells
        grab?.takeIf { it.isAwayFromHome && it.visiblePushCells != cells }?.let {
            grab = it.copy(visiblePushCells = cells)
        }
    }

    fun enter() {
        editing = true
    }

    fun exit() {
        cancelGrab()
        editing = false
    }

    fun begin(layout: CanvasLayout, itemId: String, minColSpan: Int, minRowSpan: Int) {
        if (editing && settled == null) {
            grab = beginCanvasGrab(layout, itemId, minColSpan, minRowSpan, visiblePushCells, shownPage ?: 0)
        }
    }

    /** Picks up one tile from the tile menu. Browsing comes back when the grab ends. */
    fun beginMove(layout: CanvasLayout, itemId: String, minColSpan: Int, minRowSpan: Int) {
        beginOneShot(layout, itemId, minColSpan, minRowSpan)
    }

    /** Starts a finger grab from browse or edit. Browse comes back when the grab ends. */
    fun beginTouch(
        layout: CanvasLayout,
        itemId: String,
        minColSpan: Int,
        minRowSpan: Int,
        resizeCorner: CanvasResizeCorner? = null,
    ): Boolean {
        if (grab != null || settled != null) return false
        val started = beginCanvasGrab(
            layout = layout,
            itemId = itemId,
            minColSpan = minColSpan,
            minRowSpan = minRowSpan,
            visiblePushCells = visiblePushCells,
            pageIndex = shownPage ?: 0,
            driver = CanvasGrabDriver.Touch,
        ) ?: return false
        grab = started.copy(leaveEditOnEnd = !editing, resizeCorner = resizeCorner)
        editing = true
        return true
    }

    /** Ends the grab with nothing saved and hands it over, so the caller can remove its tile. */
    fun takeForRemoval(): CanvasGrab? {
        val taken = grab ?: return null
        endGrab(taken)
        returnHome(taken)
        return taken
    }

    /** Ends the grab where it is with nothing saved, so a folder on this page stays on screen. */
    fun takeForFolder(): CanvasGrab? {
        val taken = grab ?: return null
        endGrab(taken)
        return taken
    }

    /** Grabs a tile that was just added, at [cell] if given, so B can take it back off the page. */
    fun beginAdded(layout: CanvasLayout, itemId: String, minColSpan: Int, minRowSpan: Int, cell: GridCell? = null) {
        val started = beginOneShot(layout, itemId, minColSpan, minRowSpan) ?: return
        val added = started.copy(isNewTile = true)
        grab = if (cell == null) added else added.movedTo(cell.col, cell.row)
    }

    /** Leaves the mode untouched when the grab can't start, so a refused move never strands Arrange. */
    private fun beginOneShot(layout: CanvasLayout, itemId: String, minColSpan: Int, minRowSpan: Int): CanvasGrab? {
        if (grab != null) return null
        val wasEditing = editing
        editing = true
        begin(layout, itemId, minColSpan, minRowSpan)
        val started = grab?.copy(leaveEditOnEnd = !wasEditing)
        if (started == null) editing = wasEditing
        grab = started
        return started
    }

    /** Only ever removes a tile from [beginAdded], and returns to the mode the add started in. */
    fun undoAdd(remove: (String) -> Unit) {
        val added = grab?.takeIf { it.isNewTile } ?: return
        grab = null
        returnHome(added)
        if (added.leaveEditOnEnd) editing = false
        remove(added.itemId)
    }

    fun step(transform: CanvasGrab.() -> CanvasGrab) = drive(CanvasGrabDriver.Controller, transform)

    /** Applies [transform] only for the input that holds the grab. */
    fun drive(driver: CanvasGrabDriver, transform: CanvasGrab.() -> CanvasGrab) {
        grab = grab?.let { if (it.driver == driver) it.transform() else it }
    }

    /** Hands the grab to [driver], but only for the tile that is actually moving. */
    fun takeOver(driver: CanvasGrabDriver, itemId: String): Boolean {
        val current = grab?.takeIf { it.itemId == itemId } ?: return false
        grab = current.takenOverBy(driver)
        return true
    }

    /** False when the grab can't go to [target], such as a folder leaving its page. */
    fun changePage(target: JarnCanvasPage): Boolean {
        val current = grab ?: return false
        val next = current.changedPage(target) ?: return false
        grab = next
        if (target.index != current.pageIndex) {
            shownPage = target.index
            settled = null
            navigate(target.index)
        }
        return true
    }

    fun cancelGrab() {
        val cancelled = grab ?: return
        grab = null
        returnHome(cancelled)
        if (cancelled.leaveEditOnEnd) editing = false
    }

    /** With no [moveBetweenPages] a between-page drop cancels, so a tile is never half moved. */
    fun drop(
        moveBetweenPages: ((CanvasGrabDrop.BetweenPages) -> Unit)? = null,
        commit: (Map<String, GridRect>) -> Unit,
    ) {
        val finished = grab ?: return
        when (val result = finished.dropResult()) {
            CanvasGrabDrop.Unchanged ->
                if (finished.isNewTile && !finished.isAwayFromHome) {
                    place(finished, finished.preview.placements, commit)
                } else {
                    cancelGrab()
                }
            is CanvasGrabDrop.SamePage -> place(finished, result.write.placements, commit)
            is CanvasGrabDrop.BetweenPages ->
                if (moveBetweenPages == null) {
                    cancelGrab()
                } else {
                    endGrab(finished)
                    moveBetweenPages(result)
                }
        }
    }

    private fun place(
        finished: CanvasGrab,
        placements: Map<String, GridRect>,
        commit: (Map<String, GridRect>) -> Unit,
    ) {
        endGrab(finished)
        settled = placements
        commit(placements)
    }

    private fun endGrab(finished: CanvasGrab) {
        grab = null
        if (finished.leaveEditOnEnd) editing = false
    }

    private fun returnHome(ended: CanvasGrab) {
        if (ended.isAwayFromHome) {
            shownPage = ended.home.pageIndex
            settled = null
            navigate(ended.home.pageIndex)
        }
    }
}
