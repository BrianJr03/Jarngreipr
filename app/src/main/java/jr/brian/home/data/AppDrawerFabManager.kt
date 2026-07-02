package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.ui.theme.AppRed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

enum class FabPosition {
    LEFT, RIGHT
}

@Singleton
class AppDrawerFabManager @Inject constructor(@ApplicationContext context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _fabColor = MutableStateFlow(loadFabColor())
    val fabColor: StateFlow<Color> = _fabColor.asStateFlow()

    private val _isFabEnabled = MutableStateFlow(loadFabEnabled())
    val isFabEnabled: StateFlow<Boolean> = _isFabEnabled.asStateFlow()

    private val _fabVisiblePages = MutableStateFlow(loadFabVisiblePages())
    val fabVisiblePages: StateFlow<Set<Int>> = _fabVisiblePages.asStateFlow()

    private val _fabExplicitPages = MutableStateFlow(loadFabExplicitPages())
    val fabExplicitPages: StateFlow<Boolean> = _fabExplicitPages.asStateFlow()

    private val _fabPosition = MutableStateFlow(loadFabPosition())
    val fabPosition: StateFlow<FabPosition> = _fabPosition.asStateFlow()

    private fun loadFabColor(): Color {
        val colorInt = prefs.getInt(KEY_FAB_COLOR, AppRed.toArgb())
        return Color(colorInt)
    }

    private fun loadFabEnabled(): Boolean {
        return prefs.getBoolean(KEY_FAB_ENABLED, true)
    }

    private fun loadFabVisiblePages(): Set<Int> {
        val pagesString = prefs.getString(KEY_FAB_VISIBLE_PAGES, null)
        return if (pagesString.isNullOrEmpty()) {
            emptySet()
        } else {
            pagesString.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        }
    }

    private fun loadFabExplicitPages(): Boolean {
        val savedPagesStr = prefs.getString(KEY_FAB_VISIBLE_PAGES, null)
        val hasExplicitPages = !savedPagesStr.isNullOrEmpty()
        return prefs.getBoolean(KEY_FAB_EXPLICIT_PAGES, hasExplicitPages)
    }

    private fun loadFabPosition(): FabPosition {
        val positionName = prefs.getString(KEY_FAB_POSITION, FabPosition.LEFT.name)
        return try {
            FabPosition.valueOf(positionName ?: FabPosition.LEFT.name)
        } catch (_: Exception) {
            FabPosition.LEFT
        }
    }

    fun setFabColor(color: Color) {
        _fabColor.value = color
        prefs.edit { putInt(KEY_FAB_COLOR, color.toArgb()) }
    }

    fun setFabEnabled(enabled: Boolean) {
        _isFabEnabled.value = enabled
        prefs.edit { putBoolean(KEY_FAB_ENABLED, enabled) }
    }

    fun setFabVisiblePages(pages: Set<Int>) {
        _fabVisiblePages.value = pages
        val pagesString = pages.joinToString(",")
        prefs.edit { putString(KEY_FAB_VISIBLE_PAGES, pagesString) }
    }

    fun setFabExplicitPages(explicit: Boolean) {
        _fabExplicitPages.value = explicit
        prefs.edit { putBoolean(KEY_FAB_EXPLICIT_PAGES, explicit) }
    }

    fun setFabPosition(position: FabPosition) {
        _fabPosition.value = position
        prefs.edit { putString(KEY_FAB_POSITION, position.name) }
    }

    fun togglePageVisibility(pageIndex: Int, totalPages: Int) {
        if (!_fabExplicitPages.value) {
            setFabExplicitPages(true)
            val allPages = (0 until totalPages).toMutableSet()
            allPages.remove(pageIndex)
            setFabVisiblePages(allPages)
            return
        }

        val currentPages = _fabVisiblePages.value.toMutableSet()
        if (currentPages.contains(pageIndex)) {
            currentPages.remove(pageIndex)
        } else {
            currentPages.add(pageIndex)
        }
        setFabVisiblePages(currentPages)
    }

    fun isFabVisibleOnPage(pageIndex: Int): Boolean {
        if (!_isFabEnabled.value) return false
        if (!_fabExplicitPages.value) return true // Default: all pages
        return _fabVisiblePages.value.contains(pageIndex)
    }

    fun reorderPages(oldIndicesInNewOrder: Map<Int, Int>) {
        if (!_fabExplicitPages.value) return
        val oldPages = _fabVisiblePages.value
        val oldToNew = oldIndicesInNewOrder.entries.associate { (newIdx, oldIdx) -> oldIdx to newIdx }
        val newPages = oldPages.mapNotNull { oldToNew[it] }.toSet()
        setFabVisiblePages(newPages)
    }

    fun removePage(pageIndex: Int) {
        if (!_fabExplicitPages.value) return
        val oldPages = _fabVisiblePages.value
        val newPages = oldPages.mapNotNull { idx ->
            when {
                idx < pageIndex -> idx
                idx > pageIndex -> idx - 1
                else -> null
            }
        }.toSet()
        setFabVisiblePages(newPages)
    }

    companion object {
        private const val PREFS_NAME = "app_drawer_fab_prefs"
        private const val KEY_FAB_COLOR = "fab_color"
        private const val KEY_FAB_ENABLED = "fab_enabled"
        private const val KEY_FAB_VISIBLE_PAGES = "fab_visible_pages"
        private const val KEY_FAB_EXPLICIT_PAGES = "fab_explicit_pages"
        private const val KEY_FAB_POSITION = "fab_position"
    }
}
