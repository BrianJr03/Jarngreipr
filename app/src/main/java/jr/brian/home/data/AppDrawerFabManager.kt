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

/**
 * Manages the App Drawer FAB (Floating Action Button) settings.
 * Controls visibility per page and the FAB color.
 */
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

    fun togglePageVisibility(pageIndex: Int, totalPages: Int) {
        val currentPages = _fabVisiblePages.value.toMutableSet()

        if (currentPages.isEmpty()) {
            for (i in 0 until totalPages) {
                currentPages.add(i)
            }
        }

        if (currentPages.contains(pageIndex)) {
            currentPages.remove(pageIndex)
        } else {
            currentPages.add(pageIndex)
        }

        setFabVisiblePages(currentPages)
    }

    fun isFabVisibleOnPage(pageIndex: Int): Boolean {
        if (!_isFabEnabled.value) return false
        if (_fabVisiblePages.value.isEmpty()) return true // Empty means all pages
        return _fabVisiblePages.value.contains(pageIndex)
    }

    companion object {
        private const val PREFS_NAME = "app_drawer_fab_prefs"
        private const val KEY_FAB_COLOR = "fab_color"
        private const val KEY_FAB_ENABLED = "fab_enabled"
        private const val KEY_FAB_VISIBLE_PAGES = "fab_visible_pages"
    }
}
