package jr.brian.home.canvas.data

import android.content.Context
import android.content.SharedPreferences
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.withPlacement
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
 */
class CanvasLayoutManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    private val _layoutsByPage = MutableStateFlow(loadAll())
    val layoutsByPage: StateFlow<Map<Int, CanvasLayout>> = _layoutsByPage.asStateFlow()

    fun getLayout(pageIndex: Int): CanvasLayout =
        _layoutsByPage.value[pageIndex] ?: CanvasLayout()

    fun setOrientation(pageIndex: Int, orientation: CanvasScrollOrientation) {
        update(pageIndex) { it.copy(orientation = orientation) }
    }

    fun setGrid(pageIndex: Int, columns: Int, rows: Int) {
        val c = columns.coerceIn(CanvasLayout.MIN_AXIS, CanvasLayout.MAX_AXIS)
        val r = rows.coerceIn(CanvasLayout.MIN_AXIS, CanvasLayout.MAX_AXIS)
        update(pageIndex) { it.copy(columns = c, rows = r) }
    }

    fun setEditMode(pageIndex: Int, enabled: Boolean) {
        update(pageIndex) { it.copy(editMode = enabled) }
    }

    fun addItem(pageIndex: Int, item: CanvasItem) {
        update(pageIndex) { layout ->
            val existingIndex = layout.items.indexOfFirst { it.id == item.id }
            val items = if (existingIndex >= 0) {
                layout.items.toMutableList().also { it[existingIndex] = item }
            } else {
                layout.items + item
            }
            layout.copy(items = items)
        }
    }

    fun moveItem(pageIndex: Int, id: String, col: Int, row: Int) {
        update(pageIndex) { layout ->
            val items = layout.items.map { item ->
                if (item.id == id) item.withPlacement(col = col, row = row) else item
            }
            layout.copy(items = items)
        }
    }

    fun resizeItem(pageIndex: Int, id: String, colSpan: Int, rowSpan: Int) {
        val cs = colSpan.coerceAtLeast(1)
        val rs = rowSpan.coerceAtLeast(1)
        update(pageIndex) { layout ->
            val items = layout.items.map { item ->
                if (item.id == id) item.withPlacement(colSpan = cs, rowSpan = rs) else item
            }
            layout.copy(items = items)
        }
    }

    fun removeItem(pageIndex: Int, id: String) {
        update(pageIndex) { layout ->
            layout.copy(items = layout.items.filter { it.id != id })
        }
    }

    /**
     * Move the item at [fromIndex] to [toIndex] in the layout's item list.
     * Used by drag-to-reposition; the LazyGrid renders items in this order.
     * Out-of-range indices are silently ignored.
     */
    fun reorderItems(pageIndex: Int, fromIndex: Int, toIndex: Int) {
        update(pageIndex) { layout ->
            if (fromIndex !in layout.items.indices || toIndex !in layout.items.indices ||
                fromIndex == toIndex
            ) {
                layout
            } else {
                val mutable = layout.items.toMutableList()
                val item = mutable.removeAt(fromIndex)
                mutable.add(toIndex, item)
                layout.copy(items = mutable)
            }
        }
    }

    fun clear(pageIndex: Int) {
        _layoutsByPage.value = _layoutsByPage.value - pageIndex
        prefs.edit().remove(layoutKey(pageIndex)).apply()
    }

    /**
     * Replace the entire layout for [pageIndex]. Intended for backup-restore
     * flows where the persisted blob is the source of truth.
     */
    fun replaceLayout(pageIndex: Int, layout: CanvasLayout) {
        _layoutsByPage.value = _layoutsByPage.value + (pageIndex to layout)
        persist(pageIndex, layout)
    }

    /** Drop every layout. Used before applying an imported config. */
    fun clearAll() {
        _layoutsByPage.value = emptyMap()
        prefs.edit().clear().apply()
    }

    private inline fun update(pageIndex: Int, transform: (CanvasLayout) -> CanvasLayout) {
        val current = _layoutsByPage.value[pageIndex] ?: CanvasLayout()
        val updated = transform(current)
        _layoutsByPage.value = _layoutsByPage.value + (pageIndex to updated)
        persist(pageIndex, updated)
    }

    private fun persist(pageIndex: Int, layout: CanvasLayout) {
        prefs.edit().putString(layoutKey(pageIndex), json.encodeToString(layout)).apply()
    }

    private fun loadAll(): Map<Int, CanvasLayout> {
        val result = mutableMapOf<Int, CanvasLayout>()
        for (pageIndex in 0 until MAX_PAGES) {
            loadPage(pageIndex)?.let { result[pageIndex] = it }
        }
        return result
    }

    private fun loadPage(pageIndex: Int): CanvasLayout? {
        val raw = prefs.getString(layoutKey(pageIndex), null) ?: return null
        return try {
            json.decodeFromString<CanvasLayout>(raw)
        } catch (_: Exception) {
            null
        }
    }

    private fun layoutKey(pageIndex: Int): String = "$KEY_LAYOUT_PREFIX$pageIndex"

    companion object {
        private const val PREFS_NAME = "canvas_layout_prefs"
        private const val KEY_LAYOUT_PREFIX = "layout_"
        const val MAX_PAGES = 10
    }
}
