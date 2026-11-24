package jr.brian.home.canvas.grid

import jr.brian.home.canvas.model.GridRect

/**
 * Mutable working board used by the solver. Tracks which item occupies which
 * cells, with deterministic ordering for collision queries.
 *
 * This is internal to the [GridSolver] — callers should use the pure functions
 * in [GridSolver] which take and return [LayoutSnapshot] values.
 */
internal class OccupancyGrid(val config: GridConfig) {
    private val rects: MutableMap<String, GridRect> = LinkedHashMap()

    constructor(config: GridConfig, initial: Map<String, GridRect>) : this(config) {
        rects.putAll(initial)
    }

    val ids: Set<String> get() = rects.keys

    fun place(id: String, rect: GridRect) {
        rects[id] = rect
    }

    fun remove(id: String): GridRect? = rects.remove(id)

    fun rectOf(id: String): GridRect? = rects[id]

    fun snapshot(): Map<String, GridRect> = LinkedHashMap(rects)

    /**
     * Items overlapping [rect], excluding [excludeId]. Sorted by
     * (push-axis, cross-axis, id) for determinism.
     */
    fun overlapping(rect: GridRect, excludeId: String? = null): List<Pair<String, GridRect>> {
        val dir = config.pushDirection
        return rects.entries.asSequence()
            .filter { it.key != excludeId && it.value.overlaps(rect) }
            .map { it.key to it.value }
            .sortedWith(
                compareBy(
                    { it.second.pushAxis(dir) },
                    { it.second.crossAxis(dir) },
                    { it.first }
                )
            )
            .toList()
    }
}
