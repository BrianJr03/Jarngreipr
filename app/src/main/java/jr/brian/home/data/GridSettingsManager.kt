package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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

    private var _notificationShadeEnabled by mutableStateOf(loadNotificationShadeEnabled())
    val notificationShadeEnabled: Boolean
        get() = _notificationShadeEnabled

    private var _tabTransitionAnimationName by mutableStateOf(loadTabTransitionAnimationName())
    val tabTransitionAnimationName: String
        get() = _tabTransitionAnimationName

    private var _iconSnapEnabled by mutableStateOf(loadIconSnapEnabled())
    val iconSnapEnabled: Boolean
        get() = _iconSnapEnabled

    private var _snapMode by mutableStateOf(loadSnapMode())
    val snapMode: SnapMode
        get() = _snapMode

    val effectiveSnapMode: SnapMode
        get() = if (_iconSnapEnabled) _snapMode else SnapMode.OFF

    private var _bottomFlingAppDrawerEnabled by mutableStateOf(loadBottomFlingAppDrawerEnabled())
    val bottomFlingAppDrawerEnabled: Boolean
        get() = _bottomFlingAppDrawerEnabled

    private var _shadeBackgroundColorArgb by mutableLongStateOf(loadShadeBackgroundColorArgb())
    val shadeBackgroundColorArgb: Long
        get() = _shadeBackgroundColorArgb

    private var _shadeCornerRadiusDp by mutableIntStateOf(loadShadeCornerRadiusDp())
    val shadeCornerRadiusDp: Int
        get() = _shadeCornerRadiusDp

    private var _shadeBackgroundAlpha by mutableFloatStateOf(loadShadeBackgroundAlpha())
    val shadeBackgroundAlpha: Float
        get() = _shadeBackgroundAlpha

    private var _shadeAccentColorArgb by mutableLongStateOf(loadShadeAccentColorArgb())
    val shadeAccentColorArgb: Long
        get() = _shadeAccentColorArgb

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
        return prefs.getBoolean(KEY_UNLIMITED_MODE, true)
    }

    private fun loadNotificationShadeEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATION_SHADE_ENABLED, false)
    }

    fun setNotificationShadeEnabled(enabled: Boolean) {
        _notificationShadeEnabled = enabled
        prefs.edit().apply {
            putBoolean(KEY_NOTIFICATION_SHADE_ENABLED, enabled)
            apply()
        }
    }

    private fun loadTabTransitionAnimationName(): String {
        return prefs.getString(KEY_TAB_TRANSITION_ANIMATION, "") ?: ""
    }

    private fun loadIconSnapEnabled(): Boolean {
        return prefs.getBoolean(KEY_ICON_SNAP_ENABLED, true)
    }

    fun setIconSnapEnabled(enabled: Boolean) {
        _iconSnapEnabled = enabled
        prefs.edit().apply {
            putBoolean(KEY_ICON_SNAP_ENABLED, enabled)
            apply()
        }
    }

    private fun loadSnapMode(): SnapMode {
        val raw = prefs.getString(KEY_SNAP_MODE, null) ?: return SnapMode.ICON
        return runCatching { SnapMode.valueOf(raw) }.getOrDefault(SnapMode.ICON)
    }

    fun setSnapMode(mode: SnapMode) {
        if (mode == SnapMode.OFF) return
        _snapMode = mode
        prefs.edit().apply {
            putString(KEY_SNAP_MODE, mode.name)
            apply()
        }
    }

    private fun loadBottomFlingAppDrawerEnabled(): Boolean {
        return prefs.getBoolean(KEY_BOTTOM_FLING_APP_DRAWER_ENABLED, true)
    }

    fun setBottomFlingAppDrawerEnabled(enabled: Boolean) {
        _bottomFlingAppDrawerEnabled = enabled
        prefs.edit().apply {
            putBoolean(KEY_BOTTOM_FLING_APP_DRAWER_ENABLED, enabled)
            apply()
        }
    }

    private fun loadShadeBackgroundColorArgb(): Long {
        return prefs.getLong(KEY_SHADE_BACKGROUND_COLOR_ARGB, DEFAULT_SHADE_BACKGROUND_COLOR_ARGB)
    }

    fun setShadeBackgroundColorArgb(argb: Long) {
        _shadeBackgroundColorArgb = argb
        prefs.edit().apply {
            putLong(KEY_SHADE_BACKGROUND_COLOR_ARGB, argb)
            apply()
        }
    }

    private fun loadShadeCornerRadiusDp(): Int {
        return prefs.getInt(KEY_SHADE_CORNER_RADIUS_DP, DEFAULT_SHADE_CORNER_RADIUS_DP)
    }

    fun setShadeCornerRadiusDp(dp: Int) {
        val clamped = dp.coerceIn(MIN_SHADE_CORNER_RADIUS_DP, MAX_SHADE_CORNER_RADIUS_DP)
        _shadeCornerRadiusDp = clamped
        prefs.edit().apply {
            putInt(KEY_SHADE_CORNER_RADIUS_DP, clamped)
            apply()
        }
    }

    private fun loadShadeBackgroundAlpha(): Float {
        return prefs.getFloat(KEY_SHADE_BACKGROUND_ALPHA, DEFAULT_SHADE_BACKGROUND_ALPHA)
    }

    fun setShadeBackgroundAlpha(alpha: Float) {
        val clamped = alpha.coerceIn(MIN_SHADE_BACKGROUND_ALPHA, MAX_SHADE_BACKGROUND_ALPHA)
        _shadeBackgroundAlpha = clamped
        prefs.edit().apply {
            putFloat(KEY_SHADE_BACKGROUND_ALPHA, clamped)
            apply()
        }
    }

    private fun loadShadeAccentColorArgb(): Long {
        return prefs.getLong(KEY_SHADE_ACCENT_COLOR_ARGB, DEFAULT_SHADE_ACCENT_COLOR_ARGB)
    }

    fun setShadeAccentColorArgb(argb: Long) {
        _shadeAccentColorArgb = argb
        prefs.edit().apply {
            putLong(KEY_SHADE_ACCENT_COLOR_ARGB, argb)
            apply()
        }
    }

    fun setTabTransitionAnimationName(name: String) {
        _tabTransitionAnimationName = name
        prefs.edit().apply {
            putString(KEY_TAB_TRANSITION_ANIMATION, name)
            apply()
        }
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
        private const val KEY_NOTIFICATION_SHADE_ENABLED = "notification_shade_enabled"
        private const val KEY_TAB_TRANSITION_ANIMATION = "tab_transition_animation"
        private const val KEY_ICON_SNAP_ENABLED = "icon_snap_enabled"
        private const val KEY_SNAP_MODE = "snap_mode"
        private const val KEY_BOTTOM_FLING_APP_DRAWER_ENABLED = "bottom_fling_app_drawer_enabled"
        private const val KEY_SHADE_BACKGROUND_COLOR_ARGB = "shade_background_color_argb"
        const val DEFAULT_SHADE_BACKGROUND_COLOR_ARGB = 0xFF111111L
        private const val KEY_SHADE_CORNER_RADIUS_DP = "shade_corner_radius_dp"
        const val DEFAULT_SHADE_CORNER_RADIUS_DP = 20
        const val MIN_SHADE_CORNER_RADIUS_DP = 0
        const val MAX_SHADE_CORNER_RADIUS_DP = 32
        private const val KEY_SHADE_BACKGROUND_ALPHA = "shade_background_alpha"
        const val DEFAULT_SHADE_BACKGROUND_ALPHA = 1f
        const val MIN_SHADE_BACKGROUND_ALPHA = 0.3f
        const val MAX_SHADE_BACKGROUND_ALPHA = 1f
        private const val KEY_SHADE_ACCENT_COLOR_ARGB = "shade_accent_color_argb"
        const val DEFAULT_SHADE_ACCENT_COLOR_ARGB = 0L
        const val DEFAULT_COLUMN_COUNT = 4
        const val MIN_COLUMNS = 1
        const val MAX_COLUMNS = 7
        const val MIN_ROWS = 1
        const val ABSOLUTE_MAX_ROWS = 50
    }
}
