package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class GridSettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var _totalAppsCount by mutableIntStateOf(0)

    private var _columnCount by mutableIntStateOf(loadColumnCount())
    val columnCount: Int
        get() = _columnCount

    private var _rowCount by mutableIntStateOf(loadRowCount())
    val rowCount: Int
        get() = _rowCount

    private var _unlimitedMode by mutableStateOf(loadUnlimitedMode())
    val unlimitedMode: Boolean
        get() = _unlimitedMode

    fun setTotalAppsCount(count: Int) {
        _totalAppsCount = count
    }

    fun getMaxRows(): Int {
        if (_unlimitedMode) return Int.MAX_VALUE
        if (_totalAppsCount == 0) return ABSOLUTE_MAX_ROWS
        val requiredRows = (_totalAppsCount + _columnCount - 1) / _columnCount
        return requiredRows.coerceAtMost(ABSOLUTE_MAX_ROWS)
    }

    private fun loadUnlimitedMode(): Boolean {
        return prefs.getBoolean(KEY_UNLIMITED_MODE, true) // Default to unlimited
    }

    fun setUnlimitedMode(enabled: Boolean) {
        _unlimitedMode = enabled
        prefs.edit().apply {
            putBoolean(KEY_UNLIMITED_MODE, enabled)
            apply()
        }
    }

    private fun loadColumnCount(): Int {
        return prefs.getInt(KEY_COLUMN_COUNT, DEFAULT_COLUMN_COUNT)
    }

    private fun loadRowCount(): Int {
        val savedRows = prefs.getInt(KEY_ROW_COUNT, -1)
        if (savedRows == -1) {
            // First time load - calculate rows to fit all apps
            return if (_totalAppsCount > 0) {
                val requiredRows =
                    (_totalAppsCount + DEFAULT_COLUMN_COUNT - 1) / DEFAULT_COLUMN_COUNT
                requiredRows.coerceIn(MIN_ROWS, ABSOLUTE_MAX_ROWS)
            } else {
                ABSOLUTE_MAX_ROWS // Default to max rows if app count not yet known
            }
        }
        return savedRows
    }

    fun updateColumnCount(count: Int) {
        if (count in MIN_COLUMNS..MAX_COLUMNS) {
            _columnCount = count
            _unlimitedMode = false // Disable unlimited when manually adjusting
            prefs.edit().apply {
                putInt(KEY_COLUMN_COUNT, count)
                putBoolean(KEY_UNLIMITED_MODE, false)
                apply()
            }
        }
    }

    fun updateRowCount(count: Int) {
        val maxRows = getMaxRows()
        if (count in MIN_ROWS..maxRows) {
            _rowCount = count
            _unlimitedMode = false // Disable unlimited when manually adjusting
            prefs.edit().apply {
                putInt(KEY_ROW_COUNT, count)
                putBoolean(KEY_UNLIMITED_MODE, false)
                apply()
            }
        }
    }

    fun resetToDefault(totalAppsCount: Int) {
        _columnCount = DEFAULT_COLUMN_COUNT
        val requiredRows = (totalAppsCount + DEFAULT_COLUMN_COUNT - 1) / DEFAULT_COLUMN_COUNT
        val rows = requiredRows.coerceIn(MIN_ROWS, ABSOLUTE_MAX_ROWS)
        _rowCount = rows
        _unlimitedMode = true // Enable unlimited mode on reset

        prefs.edit().apply {
            putInt(KEY_COLUMN_COUNT, DEFAULT_COLUMN_COUNT)
            putInt(KEY_ROW_COUNT, rows)
            putBoolean(KEY_UNLIMITED_MODE, true)
            apply()
        }
    }

    fun initializeDefaultRows(totalAppsCount: Int) {
        if (prefs.getInt(KEY_ROW_COUNT, -1) == -1 && totalAppsCount > 0) {
            val requiredRows = (totalAppsCount + DEFAULT_COLUMN_COUNT - 1) / DEFAULT_COLUMN_COUNT
            val rows = requiredRows.coerceIn(MIN_ROWS, ABSOLUTE_MAX_ROWS)
            _rowCount = rows

            prefs.edit().apply {
                putInt(KEY_ROW_COUNT, rows)
                apply()
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "grid_settings_prefs"
        private const val KEY_COLUMN_COUNT = "column_count"
        private const val KEY_ROW_COUNT = "row_count"
        private const val KEY_UNLIMITED_MODE = "unlimited_mode"
        const val DEFAULT_COLUMN_COUNT = 4
        const val MIN_COLUMNS = 1
        const val MAX_COLUMNS = 7
        const val MIN_ROWS = 1
        const val ABSOLUTE_MAX_ROWS = 30
    }
}
