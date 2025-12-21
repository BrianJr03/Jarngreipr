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
        val positionsKey = "${KEY_POSITIONS}_$pageIndex"

        val isFreeMode = prefs.getBoolean(freeModeKey, false)
        val isDragLocked = prefs.getBoolean(dragLockedKey, true)

        _isFreeModeByPage.value += (pageIndex to isFreeMode)
        _isDragLockedByPage.value += (pageIndex to isDragLocked)

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

    companion object {
        private const val PREFS_NAME = "app_position_prefs"
        private const val KEY_POSITIONS = "positions"
        private const val KEY_FREE_MODE = "free_mode"
        private const val KEY_DRAG_LOCKED = "drag_locked"
        private const val SEPARATOR_APPS = "||"
        private const val SEPARATOR_COORDS = ","
    }
}
