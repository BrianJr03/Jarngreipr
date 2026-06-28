package jr.brian.home.canvas.model

import kotlinx.serialization.Serializable

@Serializable
enum class CanvasScrollOrientation {
    HORIZONTAL,
    VERTICAL
}

/**
 * Persisted layout for one Unified Canvas page.
 *
 * Content vs. arrangement:
 * - [items] is the shared content set — every entity that exists on this page,
 *   orientation-agnostic.
 * - [verticalArrangement] and [horizontalArrangement] each map an item id to
 *   its placement (col / row / colSpan / rowSpan) within that orientation's
 *   grid. The two arrangements are independent — moving or resizing in one
 *   does not perturb the other.
 *
 * The cross-axis bound differs by orientation: vertical pages are bounded by
 * [verticalColumns] (content scrolls down); horizontal pages are bounded by
 * [horizontalRows] (content scrolls right). [activeOrientation] selects which
 * arrangement renders.
 *
 * Invariant: every id in [items] MUST have a placement in BOTH arrangements.
 * Validate with [validateInvariant] after every mutation in debug builds.
 *
 * [version] is bumped whenever the persisted schema changes so callers can
 * migrate older blobs. v2 introduced [verticalArrangement] /
 * [horizontalArrangement] (Phase 3 migrates v1 blobs at load time).
 */
@Serializable
data class CanvasLayout(
    val activeOrientation: CanvasScrollOrientation = CanvasScrollOrientation.HORIZONTAL,
    val verticalColumns: Int = DEFAULT_COLUMNS,
    val horizontalRows: Int = DEFAULT_ROWS,
    val items: List<CanvasItem> = emptyList(),
    val verticalArrangement: Map<String, GridRect> = emptyMap(),
    val horizontalArrangement: Map<String, GridRect> = emptyMap(),
    /** When true, long-press starts a drag-to-reorder gesture; when false it opens the remove dialog. */
    val editMode: Boolean = false,
    val version: Int = CURRENT_VERSION
) {
    /** Arrangement map for the orientation currently rendered. */
    val activeArrangement: Map<String, GridRect>
        get() = arrangementFor(activeOrientation)

    /** Cross-axis cell count for the orientation currently rendered. */
    val activeCrossAxis: Int
        get() = crossAxisFor(activeOrientation)

    fun arrangementFor(orientation: CanvasScrollOrientation): Map<String, GridRect> =
        when (orientation) {
            CanvasScrollOrientation.VERTICAL -> verticalArrangement
            CanvasScrollOrientation.HORIZONTAL -> horizontalArrangement
        }

    fun crossAxisFor(orientation: CanvasScrollOrientation): Int =
        when (orientation) {
            CanvasScrollOrientation.VERTICAL -> verticalColumns
            CanvasScrollOrientation.HORIZONTAL -> horizontalRows
        }

    fun withArrangement(
        orientation: CanvasScrollOrientation,
        arrangement: Map<String, GridRect>
    ): CanvasLayout = when (orientation) {
        CanvasScrollOrientation.VERTICAL -> copy(verticalArrangement = arrangement)
        CanvasScrollOrientation.HORIZONTAL -> copy(horizontalArrangement = arrangement)
    }

    companion object {
        const val CURRENT_VERSION = 2
        const val DEFAULT_COLUMNS = 4
        const val DEFAULT_ROWS = 6
        const val MIN_AXIS = 1
        const val MAX_AXIS = 12
    }
}

/**
 * Throws [IllegalStateException] if any id in [CanvasLayout.items] is missing
 * from either arrangement, or if either arrangement contains a placement for
 * an id not in [CanvasLayout.items]. Call after every content mutation in
 * debug builds — manager and migration code rely on this never firing.
 */
fun CanvasLayout.validateInvariant() {
    val itemIds = items.mapTo(mutableSetOf()) { it.id }
    val vIds = verticalArrangement.keys
    val hIds = horizontalArrangement.keys
    check(itemIds == vIds) {
        "verticalArrangement keys diverge from items: missing=${itemIds - vIds}, extra=${vIds - itemIds}"
    }
    check(itemIds == hIds) {
        "horizontalArrangement keys diverge from items: missing=${itemIds - hIds}, extra=${hIds - itemIds}"
    }
}

/**
 * Returns true iff every item id has a placement in both arrangements and
 * neither arrangement carries orphan keys.
 */
fun CanvasLayout.satisfiesInvariant(): Boolean {
    val itemIds = items.mapTo(mutableSetOf()) { it.id }
    return verticalArrangement.keys == itemIds && horizontalArrangement.keys == itemIds
}
