package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import jr.brian.home.model.AppPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPositionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _positions: SnapshotStateMap<String, AppPosition> = mutableStateMapOf()
    val positions: Map<String, AppPosition>
        get() = _positions

    private val _isFreeModeEnabled = MutableStateFlow(loadFreeMode())
    val isFreeModeEnabled: StateFlow<Boolean> = _isFreeModeEnabled.asStateFlow()

    init {
        loadPositions()
    }

    private fun loadFreeMode(): Boolean {
        return prefs.getBoolean(KEY_FREE_MODE, false)
    }

    fun setFreeMode(enabled: Boolean) {
        _isFreeModeEnabled.value = enabled
        prefs.edit().apply {
            putBoolean(KEY_FREE_MODE, enabled)
            apply()
        }
    }

    private fun loadPositions() {
        val positionsJson = prefs.getString(KEY_POSITIONS, null) ?: return

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
                        _positions[packageName] = AppPosition(packageName, x, y, iconSize)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun savePosition(position: AppPosition) {
        _positions[position.packageName] = position
        savePositions()
    }

    fun removePosition(packageName: String) {
        _positions.remove(packageName)
        savePositions()
    }

    fun getPosition(packageName: String): AppPosition? {
        return _positions[packageName]
    }

    fun clearAllPositions() {
        _positions.clear()
        savePositions()
    }

    private fun savePositions() {
        val positionsJson = _positions.values.joinToString(SEPARATOR_APPS) { position ->
            "${position.packageName}$SEPARATOR_COORDS${position.x}$SEPARATOR_COORDS${position.y}$SEPARATOR_COORDS${position.iconSize}"
        }
        prefs.edit().apply {
            putString(KEY_POSITIONS, positionsJson)
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "app_position_prefs"
        private const val KEY_POSITIONS = "positions"
        private const val KEY_FREE_MODE = "free_mode"
        private const val SEPARATOR_APPS = "||"
        private const val SEPARATOR_COORDS = ","
    }
}
