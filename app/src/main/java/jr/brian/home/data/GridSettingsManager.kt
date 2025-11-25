package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

class GridSettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var _columnCount by mutableIntStateOf(loadColumnCount())
    val columnCount: Int
        get() = _columnCount

    private var _rowCount by mutableIntStateOf(loadRowCount())
    val rowCount: Int
        get() = _rowCount

    private fun loadColumnCount(): Int {
        return prefs.getInt(KEY_COLUMN_COUNT, DEFAULT_COLUMN_COUNT)
    }

    private fun loadRowCount(): Int {
        return prefs.getInt(KEY_ROW_COUNT, DEFAULT_ROW_COUNT)
    }

    fun updateColumnCount(count: Int) {
        if (count in MIN_COLUMNS..MAX_COLUMNS) {
            _columnCount = count
            prefs.edit().apply {
                putInt(KEY_COLUMN_COUNT, count)
                apply()
            }
        }
    }

    fun updateRowCount(count: Int) {
        if (count in MIN_ROWS..MAX_ROWS) {
            _rowCount = count
            prefs.edit().apply {
                putInt(KEY_ROW_COUNT, count)
                apply()
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "grid_settings_prefs"
        private const val KEY_COLUMN_COUNT = "column_count"
        private const val KEY_ROW_COUNT = "row_count"
        const val DEFAULT_COLUMN_COUNT = 4
        const val DEFAULT_ROW_COUNT = 3
        const val MIN_COLUMNS = 1
        const val MAX_COLUMNS = 10
        const val MIN_ROWS = 1
        const val MAX_ROWS = 10
    }
}
