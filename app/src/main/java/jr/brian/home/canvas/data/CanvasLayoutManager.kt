package jr.brian.home.canvas.data

import android.content.Context
import android.content.SharedPreferences
import jr.brian.home.canvas.grid.GridSolver
import jr.brian.home.canvas.grid.PushDirection
import jr.brian.home.canvas.grid.firstFreeRect
import jr.brian.home.canvas.grid.toSnapshot
import jr.brian.home.canvas.grid.withSnapshot
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridCell
import jr.brian.home.canvas.model.GridRect
import jr.brian.home.canvas.model.defaultSpanFor
import jr.brian.home.canvas.model.satisfiesInvariant
import jr.brian.home.canvas.model.validateInvariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persistence + StateFlow for Unified Canvas layouts, one per pager [pageIndex].
 *
 * Layouts are serialized to JSON because the item list is heterogeneous
 * (apps, folders, ROMs, widgets, RSS) — the `||`/`,` string format used by
 * sibling per-page managers can't represent that shape safely.
 *
 * Content / arrangement split: content mutations ([addItem], [removeItem])
 * touch both arrangement maps so every item id is always present in both —
 * see [CanvasLayout.validateInvariant]. Layout mutations ([moveItem],
 * [resizeItem], [replaceLayout] via [withSnapshot]) only touch the
 * *active* orientation's arrangement, so the inactive grid stays exactly
 * where the user last saw it.
 */
class CanvasLayoutManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    private val _layoutsByPage = MutableStateFlow(loadAndMigrateAll())
    val layoutsByPage: StateFlow<Map<Int, CanvasLayout>> = _layoutsByPage.asStateFlow()

    fun getLayout(pageIndex: Int): CanvasLayout =
        _layoutsByPage.value[pageIndex] ?: CanvasLayout()

    fun setOrientation(pageIndex: Int, orientation: CanvasScrollOrientation) {
        update(pageIndex) { it.copy(activeOrientation = orientation) }
    }

    fun setGrid(pageIndex: Int, columns: Int, rows: Int) {
        val c = columns.coerceIn(CanvasLayout.MIN_AXIS, CanvasLayout.MAX_AXIS)
        val r = rows.coerceIn(CanvasLayout.MIN_AXIS, CanvasLayout.MAX_AXIS)
        update(pageIndex) { it.copy(verticalColumns = c, horizontalRows = r) }
    }

    fun setEditMode(pageIndex: Int, enabled: Boolean) {
        update(pageIndex) { it.copy(editMode = enabled) }
    }

    /**
     * Add or replace an item on the canvas. The item's id is the identity key.
     *
     * Phase 1 behavior:
     * - First time an id appears: append to [CanvasLayout.items] and auto-place
     *   into BOTH the vertical and horizontal arrangements at each grid's next
     *   free cell, using [colSpan] / [rowSpan] (defaults from [defaultSpanFor]).
     * - Existing id: the entity reference is updated in place; existing
     *   per-orientation placements are preserved so a replace never reflows.
     *
     * [atCellInActive] is honored when adding to the active orientation (drop
     * target). The other orientation always uses its own next-free cell.
     */
    fun addItem(
        pageIndex: Int,
        item: CanvasItem,
        colSpan: Int = defaultSpanFor(item).first,
        rowSpan: Int = defaultSpanFor(item).second,
        atCellInActive: GridCell? = null
    ) {
        update(pageIndex) { layout ->
            val existingIndex = layout.items.indexOfFirst { it.id == item.id }
            val newItems = if (existingIndex >= 0) {
                layout.items.toMutableList().also { it[existingIndex] = item }
            } else {
                layout.items + item
            }

            val isReplace = existingIndex >= 0
            val vertical = layout.verticalArrangement.placeIfAbsent(
                itemId = item.id,
                colSpan = colSpan.coerceAtLeast(1),
                rowSpan = rowSpan.coerceAtLeast(1),
                crossAxisCount = layout.verticalColumns,
                pushDirection = PushDirection.DOWN,
                preferredCell = atCellInActive
                    .takeIf { !isReplace && layout.activeOrientation == CanvasScrollOrientation.VERTICAL },
                reservedRects = reservedRectsFor(layout, CanvasScrollOrientation.VERTICAL)
            )
            val horizontal = layout.horizontalArrangement.placeIfAbsent(
                itemId = item.id,
                colSpan = colSpan.coerceAtLeast(1),
                rowSpan = rowSpan.coerceAtLeast(1),
                crossAxisCount = layout.horizontalRows,
                pushDirection = PushDirection.RIGHT,
                preferredCell = atCellInActive
                    .takeIf { !isReplace && layout.activeOrientation == CanvasScrollOrientation.HORIZONTAL },
                reservedRects = reservedRectsFor(layout, CanvasScrollOrientation.HORIZONTAL)
            )

            layout.copy(
                items = newItems,
                verticalArrangement = vertical,
                horizontalArrangement = horizontal
            )
        }
    }

    fun moveItem(pageIndex: Int, id: String, col: Int, row: Int) {
        update(pageIndex) { layout ->
            val arrangement = layout.activeArrangement
            val current = arrangement[id] ?: return@update layout
            val moved = current.withOrigin(col.coerceAtLeast(0), row.coerceAtLeast(0))
            layout.withArrangement(
                layout.activeOrientation,
                arrangement.toMutableMap().also { it[id] = moved }
            )
        }
    }

    fun resizeItem(pageIndex: Int, id: String, colSpan: Int, rowSpan: Int) {
        val cs = colSpan.coerceAtLeast(1)
        val rs = rowSpan.coerceAtLeast(1)
        update(pageIndex) { layout ->
            val arrangement = layout.activeArrangement
            val current = arrangement[id] ?: return@update layout
            val resized = current.withSize(cs, rs)
            layout.withArrangement(
                layout.activeOrientation,
                arrangement.toMutableMap().also { it[id] = resized }
            )
        }
    }

    fun removeItem(pageIndex: Int, id: String) {
        update(pageIndex) { layout ->
            layout.copy(
                items = layout.items.filter { it.id != id },
                verticalArrangement = layout.verticalArrangement - id,
                horizontalArrangement = layout.horizontalArrangement - id
            )
        }
    }

    /**
     * Pull every item in the active orientation's arrangement toward the grid
     * origin via [GridSolver.compact], closing gaps from previous moves /
     * deletes / shrinks. The inactive orientation's arrangement is untouched,
     * matching the "independent arrangements" invariant.
     *
     * Compact is the only operation allowed to close gaps — move/resize
     * solvers preserve them — so users explicitly opt in from the edit menu.
     */
    fun compactLayout(pageIndex: Int) {
        update(pageIndex) { layout ->
            if (layout.activeArrangement.isEmpty()) return@update layout
            val compacted = GridSolver.compact(layout.toSnapshot())
            layout.withSnapshot(compacted)
        }
    }

    /**
     * Reorder is a no-op in the new absolute-coordinate model — items are
     * positioned by [GridRect], not by list index. Kept for source compat with
     * the legacy LazyGrid path (no caller relies on the effect anymore).
     */
    @Suppress("UNUSED_PARAMETER")
    fun reorderItems(pageIndex: Int, fromIndex: Int, toIndex: Int) {
        // Intentionally empty.
    }

    fun clear(pageIndex: Int) {
        _layoutsByPage.value = _layoutsByPage.value - pageIndex
        prefs.edit().remove(layoutKey(pageIndex)).apply()
    }

    /**
     * Replace the entire layout for [pageIndex]. Used by the solver-driven
     * gesture commit (via [jr.brian.home.canvas.grid.withSnapshot]) and the
     * backup-restore path. Repairs the invariant if the incoming layout is
     * missing arrangement entries (legacy / imported data); validates after.
     */
    fun replaceLayout(pageIndex: Int, layout: CanvasLayout) {
        val repaired = if (layout.satisfiesInvariant()) layout else repair(layout)
        if (BuildAssert.ENABLED) repaired.validateInvariant()
        _layoutsByPage.value = _layoutsByPage.value + (pageIndex to repaired)
        persist(pageIndex, repaired)
    }

    /** Drop every layout. Used before applying an imported config. */
    fun clearAll() {
        _layoutsByPage.value = emptyMap()
        prefs.edit().clear().apply()
    }

    private inline fun update(pageIndex: Int, transform: (CanvasLayout) -> CanvasLayout) {
        val current = _layoutsByPage.value[pageIndex] ?: CanvasLayout()
        val updated = transform(current)
        if (BuildAssert.ENABLED) updated.validateInvariant()
        _layoutsByPage.value = _layoutsByPage.value + (pageIndex to updated)
        persist(pageIndex, updated)
    }

    private fun persist(pageIndex: Int, layout: CanvasLayout) {
        prefs.edit().putString(layoutKey(pageIndex), json.encodeToString(layout)).apply()
    }

    /**
     * Cold-load every page once at construction. Pages whose stored JSON is in
     * a legacy (v1) shape are migrated in place and the migrated v2 blob is
     * written back so subsequent loads are zero-cost.
     */
    private fun loadAndMigrateAll(): Map<Int, CanvasLayout> {
        val result = mutableMapOf<Int, CanvasLayout>()
        for (pageIndex in 0 until MAX_PAGES) {
            val raw = prefs.getString(layoutKey(pageIndex), null) ?: continue
            val (layout, wroteMigration) = loadOrMigrate(raw) ?: continue
            result[pageIndex] = layout
            if (wroteMigration) persist(pageIndex, layout)
        }
        return result
    }

    /**
     * Decode [raw] into a v2 [CanvasLayout]. Returns the layout plus a flag
     * indicating whether a schema migration ran (so the caller can persist
     * the migrated blob once). Branches by [CanvasLayoutMigration.classify]:
     *
     *   - V2: decode directly; repair only if the invariant doesn't hold.
     *   - V1: route through [CanvasLayoutMigration.tryMigrateV1] to preserve
     *     the user's v1 positions in the v1-orientation arrangement and
     *     auto-place into the other.
     *   - Unknown / migration fails: fall back to decoding as v2 + repair
     *     (last resort — content survives, positions reset).
     */
    private fun loadOrMigrate(raw: String): Pair<CanvasLayout, Boolean>? {
        when (CanvasLayoutMigration.classify(raw, json)) {
            CanvasLayoutMigration.Schema.V2 -> {
                val decoded = runCatching { json.decodeFromString<CanvasLayout>(raw) }
                    .getOrElse { return null }
                return if (decoded.satisfiesInvariant()) decoded to false
                else repair(decoded) to true
            }
            CanvasLayoutMigration.Schema.V1 -> {
                val migrated = CanvasLayoutMigration.tryMigrateV1(raw, json)
                if (migrated != null) return migrated to true
            }
            CanvasLayoutMigration.Schema.Unknown -> Unit
        }
        // Last resort: decode under the v2 reader, repair if needed.
        val decoded = runCatching { json.decodeFromString<CanvasLayout>(raw) }
            .getOrElse { return null }
        return if (decoded.satisfiesInvariant()) decoded to false else repair(decoded) to true
    }

    /**
     * Bring [layout] in line with the content-vs-arrangement invariant: drop
     * arrangement entries for ids not in [items], auto-place missing ids into
     * each orientation's grid at the next free cell using default spans.
     *
     * Phase 1 minimum: handles legacy data and any divergent state without
     * losing items. Phase 3 will expand this with a v1→v2 union step (when
     * legacy blobs are decoded as v1 because they lack arrangement maps).
     */
    private fun repair(layout: CanvasLayout): CanvasLayout {
        val itemIds = layout.items.mapTo(mutableSetOf()) { it.id }
        val vertical = layout.verticalArrangement.toMutableMap()
        val horizontal = layout.horizontalArrangement.toMutableMap()

        vertical.keys.retainAll(itemIds)
        horizontal.keys.retainAll(itemIds)

        layout.items.forEach { item ->
            val (cs, rs) = defaultSpanFor(item)
            if (item.id !in vertical) {
                vertical[item.id] = firstFreeRect(
                    occupied = vertical.values,
                    crossAxisCount = layout.verticalColumns,
                    pushDirection = PushDirection.DOWN,
                    colSpan = cs,
                    rowSpan = rs
                )
            }
            if (item.id !in horizontal) {
                horizontal[item.id] = firstFreeRect(
                    occupied = horizontal.values,
                    crossAxisCount = layout.horizontalRows,
                    pushDirection = PushDirection.RIGHT,
                    colSpan = cs,
                    rowSpan = rs
                )
            }
        }
        return layout.copy(
            verticalArrangement = vertical,
            horizontalArrangement = horizontal
        )
    }

    private fun layoutKey(pageIndex: Int): String = "$KEY_LAYOUT_PREFIX$pageIndex"

    companion object {
        private const val PREFS_NAME = "canvas_layout_prefs"
        private const val KEY_LAYOUT_PREFIX = "layout_"
        const val MAX_PAGES = 10
    }
}

private object BuildAssert {
    // Mirror of BuildConfig.DEBUG without forcing a build-feature dependency on
    // this class. Invariant validation throws so a bad mutation surfaces in dev
    // builds; release builds skip the cost (~items.size set comparison).
    val ENABLED: Boolean = isAssertEnabled()

    private fun isAssertEnabled(): Boolean {
        var enabled = false
        @Suppress("AssertWithSideEffects")
        assert(run { enabled = true; enabled })
        return enabled
    }
}

private fun Map<String, GridRect>.placeIfAbsent(
    itemId: String,
    colSpan: Int,
    rowSpan: Int,
    crossAxisCount: Int,
    pushDirection: PushDirection,
    preferredCell: GridCell?,
    reservedRects: Collection<GridRect> = emptyList()
): Map<String, GridRect> {
    if (containsKey(itemId)) return this
    val occupied = values
    val rect = if (preferredCell != null) {
        val candidate = GridRect(
            col = preferredCell.col.coerceAtLeast(0),
            row = preferredCell.row.coerceAtLeast(0),
            colSpan = colSpan,
            rowSpan = rowSpan
        )
        val collidesOccupied = occupied.any { it.overlaps(candidate) }
        val collidesReserved = reservedRects.any { it.overlaps(candidate) }
        if (!collidesOccupied && !collidesReserved) candidate
        else firstFreeRect(occupied, crossAxisCount, pushDirection, colSpan, rowSpan, reservedRects)
    } else {
        firstFreeRect(occupied, crossAxisCount, pushDirection, colSpan, rowSpan, reservedRects)
    }
    return toMutableMap().also { it[itemId] = rect }
}

/**
 * The UI-reserved cells for [orientation] — the floating add-icon corner.
 * Returned for the active orientation only; the inactive one stays empty so
 * the brief's invariant about not perturbing the inactive arrangement still
 * holds, and stored placements aren't retroactively invalidated. Horizontal
 * mode returns empty: its icon position is scroll-dependent, and auto-place
 * naturally fills from col 0 (well away from the right-edge icon).
 */
internal fun reservedRectsFor(layout: CanvasLayout, orientation: CanvasScrollOrientation): List<GridRect> =
    when (orientation) {
        CanvasScrollOrientation.VERTICAL -> {
            val col = (layout.verticalColumns - 1).coerceAtLeast(0)
            listOf(GridRect(col, 0, 1, 1))
        }
        CanvasScrollOrientation.HORIZONTAL -> emptyList()
    }
