package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import jr.brian.home.model.app.AppPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPositionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _positionsByPage: SnapshotStateMap<Int, SnapshotStateMap<String, AppPosition>> =
        mutableStateMapOf()

    private val _isFreeModeByPage = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val isFreeModeByPage: StateFlow<Map<Int, Boolean>> = _isFreeModeByPage.asStateFlow()

    private val _isDragLockedByPage = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val isDragLockedByPage: StateFlow<Map<Int, Boolean>> = _isDragLockedByPage.asStateFlow()

    private val _isScrollDisabledByPage = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val isScrollDisabledByPage: StateFlow<Map<Int, Boolean>> = _isScrollDisabledByPage.asStateFlow()

    private val _isBottomFlingDisabledByPage = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val isBottomFlingDisabledByPage: StateFlow<Map<Int, Boolean>> = _isBottomFlingDisabledByPage.asStateFlow()

    init {
        loadAllPageData()
        repeat(3) {
            setDragLock(
                it,
                true
            )
        }
    }

    fun getPositions(pageIndex: Int): Map<String, AppPosition> {
        return _positionsByPage[pageIndex] ?: emptyMap()
    }

    private fun loadAllPageData() {
        val maxPages = 10
        for (pageIndex in 0 until maxPages) {
            loadPageData(pageIndex)
        }
    }

    private fun loadPageData(pageIndex: Int) {
        val freeModeKey = "${KEY_FREE_MODE}_$pageIndex"
        val dragLockedKey = "${KEY_DRAG_LOCKED}_$pageIndex"
        val scrollDisabledKey = "${KEY_SCROLL_DISABLED}_$pageIndex"
        val bottomFlingDisabledKey = "${KEY_BOTTOM_FLING_DISABLED}_$pageIndex"
        val positionsKey = "${KEY_POSITIONS}_$pageIndex"

        val isFreeMode = prefs.getBoolean(freeModeKey, false)
        val isDragLocked = prefs.getBoolean(dragLockedKey, true)
        val isScrollDisabled = prefs.getBoolean(scrollDisabledKey, false)
        val isBottomFlingDisabled = prefs.getBoolean(bottomFlingDisabledKey, false)

        _isFreeModeByPage.value += (pageIndex to isFreeMode)
        _isDragLockedByPage.value += (pageIndex to isDragLocked)
        _isScrollDisabledByPage.value += (pageIndex to isScrollDisabled)
        _isBottomFlingDisabledByPage.value += (pageIndex to isBottomFlingDisabled)

        val positionsJson = prefs.getString(positionsKey, null) ?: return
        val pagePositions = mutableStateMapOf<String, AppPosition>()

        try {
            positionsJson.split(SEPARATOR_APPS).forEach { appData ->
                if (appData.isNotBlank()) {
                    val parts = appData.split(SEPARATOR_COORDS)
                    if (parts.size >= 3) {
                        val packageName = parts[0]
                        val x = parts[1].toFloatOrNull() ?: return@forEach
                        val y = parts[2].toFloatOrNull() ?: return@forEach
                        val iconSize = if (parts.size >= 4) {
                            parts[3].toFloatOrNull() ?: 64f
                        } else {
                            64f
                        }
                        pagePositions[packageName] = AppPosition(packageName, x, y, iconSize)
                    }
                }
            }
            if (pagePositions.isNotEmpty()) {
                _positionsByPage[pageIndex] = pagePositions
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setFreeMode(pageIndex: Int, enabled: Boolean) {
        _isFreeModeByPage.value += (pageIndex to enabled)
        prefs.edit().apply {
            putBoolean("${KEY_FREE_MODE}_$pageIndex", enabled)
            apply()
        }
    }

    fun setDragLock(pageIndex: Int, locked: Boolean) {
        _isDragLockedByPage.value += (pageIndex to locked)
        prefs.edit().apply {
            putBoolean("${KEY_DRAG_LOCKED}_$pageIndex", locked)
            apply()
        }
    }

    fun setScrollDisabled(pageIndex: Int, disabled: Boolean) {
        _isScrollDisabledByPage.value += (pageIndex to disabled)
        prefs.edit().apply {
            putBoolean("${KEY_SCROLL_DISABLED}_$pageIndex", disabled)
            apply()
        }
    }

    fun setBottomFlingDisabled(pageIndex: Int, disabled: Boolean) {
        _isBottomFlingDisabledByPage.value += (pageIndex to disabled)
        prefs.edit().apply {
            putBoolean("${KEY_BOTTOM_FLING_DISABLED}_$pageIndex", disabled)
            apply()
        }
    }

    fun savePosition(pageIndex: Int, position: AppPosition) {
        val pagePositions = _positionsByPage.getOrPut(pageIndex) { mutableStateMapOf() }
        pagePositions[position.packageName] = position
        savePositionsForPage(pageIndex)
    }

    fun getPosition(pageIndex: Int, packageName: String): AppPosition? {
        return _positionsByPage[pageIndex]?.get(packageName)
    }

    fun removePosition(pageIndex: Int, packageName: String) {
        _positionsByPage[pageIndex]?.remove(packageName)
        savePositionsForPage(pageIndex)
    }

    fun clearAllPositions(pageIndex: Int) {
        _positionsByPage[pageIndex]?.clear()
        savePositionsForPage(pageIndex)
    }

    private fun savePositionsForPage(pageIndex: Int) {
        val positions = _positionsByPage[pageIndex] ?: return
        val positionsJson = positions.values.joinToString(SEPARATOR_APPS) { position ->
            "${position.packageName}$SEPARATOR_COORDS${position.x}$SEPARATOR_COORDS${position.y}$SEPARATOR_COORDS${position.iconSize}"
        }
        prefs.edit().apply {
            putString("${KEY_POSITIONS}_$pageIndex", positionsJson)
            apply()
        }
    }

    fun reorderPages(oldIndicesInNewOrder: Map<Int, Int>) {
        val oldPositions = _positionsByPage.toMap()
        val oldFreeMode = _isFreeModeByPage.value
        val oldDragLocked = _isDragLockedByPage.value
        val oldScrollDisabled = _isScrollDisabledByPage.value
        val oldBottomFling = _isBottomFlingDisabledByPage.value

        val newPositions = mutableMapOf<Int, SnapshotStateMap<String, AppPosition>>()
        val newFreeMode = mutableMapOf<Int, Boolean>()
        val newDragLocked = mutableMapOf<Int, Boolean>()
        val newScrollDisabled = mutableMapOf<Int, Boolean>()
        val newBottomFling = mutableMapOf<Int, Boolean>()

        oldIndicesInNewOrder.forEach { (newIndex, oldIndex) ->
            oldPositions[oldIndex]?.let { newPositions[newIndex] = it }
            oldFreeMode[oldIndex]?.let { newFreeMode[newIndex] = it }
            oldDragLocked[oldIndex]?.let { newDragLocked[newIndex] = it }
            oldScrollDisabled[oldIndex]?.let { newScrollDisabled[newIndex] = it }
            oldBottomFling[oldIndex]?.let { newBottomFling[newIndex] = it }
        }

        replacePageState(
            oldPageIndices = oldPositions.keys + oldFreeMode.keys + oldDragLocked.keys +
                oldScrollDisabled.keys + oldBottomFling.keys,
            newPositions = newPositions,
            newFreeMode = newFreeMode,
            newDragLocked = newDragLocked,
            newScrollDisabled = newScrollDisabled,
            newBottomFling = newBottomFling
        )
    }

    fun removePage(pageIndex: Int) {
        val oldPositions = _positionsByPage.toMap()
        val oldFreeMode = _isFreeModeByPage.value
        val oldDragLocked = _isDragLockedByPage.value
        val oldScrollDisabled = _isScrollDisabledByPage.value
        val oldBottomFling = _isBottomFlingDisabledByPage.value

        val newPositions = mutableMapOf<Int, SnapshotStateMap<String, AppPosition>>()
        val newFreeMode = mutableMapOf<Int, Boolean>()
        val newDragLocked = mutableMapOf<Int, Boolean>()
        val newScrollDisabled = mutableMapOf<Int, Boolean>()
        val newBottomFling = mutableMapOf<Int, Boolean>()

        fun <V> shiftInto(src: Map<Int, V>, dst: MutableMap<Int, V>) {
            src.forEach { (idx, v) ->
                when {
                    idx < pageIndex -> dst[idx] = v
                    idx > pageIndex -> dst[idx - 1] = v
                }
            }
        }

        shiftInto(oldPositions, newPositions)
        shiftInto(oldFreeMode, newFreeMode)
        shiftInto(oldDragLocked, newDragLocked)
        shiftInto(oldScrollDisabled, newScrollDisabled)
        shiftInto(oldBottomFling, newBottomFling)

        replacePageState(
            oldPageIndices = oldPositions.keys + oldFreeMode.keys + oldDragLocked.keys +
                oldScrollDisabled.keys + oldBottomFling.keys,
            newPositions = newPositions,
            newFreeMode = newFreeMode,
            newDragLocked = newDragLocked,
            newScrollDisabled = newScrollDisabled,
            newBottomFling = newBottomFling
        )
    }

    private fun replacePageState(
        oldPageIndices: Set<Int>,
        newPositions: Map<Int, SnapshotStateMap<String, AppPosition>>,
        newFreeMode: Map<Int, Boolean>,
        newDragLocked: Map<Int, Boolean>,
        newScrollDisabled: Map<Int, Boolean>,
        newBottomFling: Map<Int, Boolean>
    ) {
        _positionsByPage.clear()
        newPositions.forEach { (idx, positions) -> _positionsByPage[idx] = positions }
        _isFreeModeByPage.value = newFreeMode
        _isDragLockedByPage.value = newDragLocked
        _isScrollDisabledByPage.value = newScrollDisabled
        _isBottomFlingDisabledByPage.value = newBottomFling

        prefs.edit().apply {
            oldPageIndices.forEach { idx ->
                remove("${KEY_POSITIONS}_$idx")
                remove("${KEY_FREE_MODE}_$idx")
                remove("${KEY_DRAG_LOCKED}_$idx")
                remove("${KEY_SCROLL_DISABLED}_$idx")
                remove("${KEY_BOTTOM_FLING_DISABLED}_$idx")
            }
            newPositions.forEach { (idx, positions) ->
                val positionsJson = positions.values.joinToString(SEPARATOR_APPS) { p ->
                    "${p.packageName}$SEPARATOR_COORDS${p.x}$SEPARATOR_COORDS${p.y}$SEPARATOR_COORDS${p.iconSize}"
                }
                putString("${KEY_POSITIONS}_$idx", positionsJson)
            }
            newFreeMode.forEach { (idx, v) -> putBoolean("${KEY_FREE_MODE}_$idx", v) }
            newDragLocked.forEach { (idx, v) -> putBoolean("${KEY_DRAG_LOCKED}_$idx", v) }
            newScrollDisabled.forEach { (idx, v) -> putBoolean("${KEY_SCROLL_DISABLED}_$idx", v) }
            newBottomFling.forEach { (idx, v) -> putBoolean("${KEY_BOTTOM_FLING_DISABLED}_$idx", v) }
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "app_position_prefs"
        private const val KEY_POSITIONS = "positions"
        private const val KEY_FREE_MODE = "free_mode"
        private const val KEY_DRAG_LOCKED = "drag_locked"
        private const val KEY_SCROLL_DISABLED = "scroll_disabled"
        private const val KEY_BOTTOM_FLING_DISABLED = "bottom_fling_disabled"
        private const val SEPARATOR_APPS = "||"
        private const val SEPARATOR_COORDS = ","
    }
}
