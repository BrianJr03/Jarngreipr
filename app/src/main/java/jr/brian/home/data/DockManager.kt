package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import jr.brian.home.ui.theme.AlmostBlack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DockManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _dockApps = MutableStateFlow(loadDockApps())
    val dockApps: StateFlow<List<String>> = _dockApps.asStateFlow()

    private val _dockColor = MutableStateFlow(loadDockColor())
    val dockColor: StateFlow<Color> = _dockColor.asStateFlow()

    private val _dockSize = MutableStateFlow(loadDockSize())
    val dockSize: StateFlow<DockSize> = _dockSize.asStateFlow()

    private val _isDockVisible = MutableStateFlow(loadDockVisibility())
    val isDockVisible: StateFlow<Boolean> = _isDockVisible.asStateFlow()

    private val _dockVisiblePages = MutableStateFlow(loadDockVisiblePages())
    val dockVisiblePages: StateFlow<Set<Int>> = _dockVisiblePages.asStateFlow()

    private val _maxDockApps = MutableStateFlow(loadMaxDockApps())
    val maxDockApps: StateFlow<Int> = _maxDockApps.asStateFlow()

    private fun loadDockApps(): List<String> {
        val slotCount = prefs.getInt(KEY_DOCK_SLOT_COUNT, 0)
        if (slotCount == 0) return emptyList()

        return (0 until slotCount).map { dockIndex ->
            prefs.getString("${KEY_DOCK_APP_PREFIX}$dockIndex", null) ?: ""
        }
    }

    fun addEmptySlot(position: Int) {
        if (position !in 0 until _maxDockApps.value) return

        val currentDockApps = _dockApps.value.toMutableList()

        while (currentDockApps.size <= position) {
            currentDockApps.add("")
        }

        _dockApps.value = currentDockApps
        saveDockApps(currentDockApps)
    }

    fun addAppToDock(
        packageName: String,
        position: Int
    ) {
        if (position !in 0 until _maxDockApps.value) return

        val currentDockApps = _dockApps.value.toMutableList()

        while (currentDockApps.size <= position) {
            currentDockApps.add("")
        }

        if (currentDockApps.count { it.isNotEmpty() } >= _maxDockApps.value) return
        if (currentDockApps.contains(packageName)) return

        currentDockApps[position] = packageName
        _dockApps.value = currentDockApps
        saveDockApps(currentDockApps)
    }

    fun removeAppFromDock(packageName: String) {
        val currentDockApps = _dockApps.value.toMutableList()

        val index = currentDockApps.indexOf(packageName)
        if (index == -1) return

        currentDockApps[index] = ""
        _dockApps.value = currentDockApps
        saveDockApps(currentDockApps)
    }

    fun removeEmptySlot(position: Int) {
        val currentDockApps = _dockApps.value.toMutableList()

        if (position !in currentDockApps.indices) return
        if (currentDockApps[position].isNotEmpty()) return

        currentDockApps.removeAt(position)
        _dockApps.value = currentDockApps

        if (currentDockApps.isEmpty()) {
            prefs.edit().apply {
                for (i in 0 until MAX_DOCK_APPS) {
                    remove("${KEY_DOCK_APP_PREFIX}$i")
                }
                remove(KEY_DOCK_SLOT_COUNT)
                apply()
            }
        } else {
            saveDockApps(currentDockApps)
        }
    }

    fun isAppInDock(packageName: String): Boolean {
        return _dockApps.value.contains(packageName)
    }

    private fun loadDockColor(): Color {
        val colorInt = prefs.getInt(KEY_DOCK_COLOR, AlmostBlack.toArgb())
        return Color(colorInt)
    }

    private fun loadDockSize(): DockSize {
        val sizeOrdinal = prefs.getInt(KEY_DOCK_SIZE, DockSize.COMPACT.ordinal)
        return DockSize.fromOrdinal(sizeOrdinal)
    }

    private fun loadDockVisibility(): Boolean {
        return prefs.getBoolean(KEY_DOCK_VISIBLE, true)
    }

    private fun loadDockVisiblePages(): Set<Int> {
        val pagesString = prefs.getString(KEY_DOCK_VISIBLE_PAGES, null)
        return if (pagesString.isNullOrEmpty()) {
            emptySet()
        } else {
            pagesString.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        }
    }

    private fun loadMaxDockApps(): Int {
        return prefs.getInt(KEY_MAX_DOCK_APPS, MAX_DOCK_APPS)
    }

    fun setDockColor(color: Color) {
        _dockColor.value = color
        prefs.edit().putInt(KEY_DOCK_COLOR, color.toArgb()).apply()
    }

    fun setDockSize(size: DockSize) {
        _dockSize.value = size
        prefs.edit().putInt(KEY_DOCK_SIZE, size.ordinal).apply()
    }

    fun setDockVisibility(visible: Boolean) {
        _isDockVisible.value = visible
        prefs.edit().putBoolean(KEY_DOCK_VISIBLE, visible).apply()
    }

    fun setDockVisiblePages(pages: Set<Int>) {
        _dockVisiblePages.value = pages
        val pagesString = pages.joinToString(",")
        prefs.edit().putString(KEY_DOCK_VISIBLE_PAGES, pagesString).apply()
    }

    fun setMaxDockApps(max: Int) {
        if (max !in 2..MAX_DOCK_APPS) return
        _maxDockApps.value = max
        prefs.edit().putInt(KEY_MAX_DOCK_APPS, max).apply()
        
        val currentDockApps = _dockApps.value
        if (currentDockApps.size > max) {
            val trimmedDockApps = currentDockApps.take(max)
            _dockApps.value = trimmedDockApps
            saveDockApps(trimmedDockApps)
        }
    }

    fun togglePageVisibility(pageIndex: Int, totalPages: Int) {
        val currentPages = _dockVisiblePages.value.toMutableSet()

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

        if (currentPages.isEmpty()) {
            setDockVisibility(false)
        }

        setDockVisiblePages(currentPages)
    }

    fun isDockVisibleOnPage(pageIndex: Int): Boolean {
        if (_dockVisiblePages.value.isEmpty()) return true
        return _dockVisiblePages.value.contains(pageIndex)
    }

    fun swapDockApps(fromPosition: Int, toPosition: Int) {
        val currentDockApps = _dockApps.value.toMutableList()
        
        if (fromPosition !in currentDockApps.indices || toPosition !in currentDockApps.indices) return
        if (fromPosition == toPosition) return
        
        val temp = currentDockApps[fromPosition]
        currentDockApps[fromPosition] = currentDockApps[toPosition]
        currentDockApps[toPosition] = temp
        
        _dockApps.value = currentDockApps
        saveDockApps(currentDockApps)
    }

    private fun saveDockApps(apps: List<String>) {
        prefs.edit().apply {
            putInt(KEY_DOCK_SLOT_COUNT, apps.size)

            for (i in 0 until MAX_DOCK_APPS) {
                remove("${KEY_DOCK_APP_PREFIX}$i")
            }

            apps.forEachIndexed { index, packageName ->
                if (packageName.isNotEmpty()) {
                    putString("${KEY_DOCK_APP_PREFIX}$index", packageName)
                }
            }
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "dock_prefs"
        private const val KEY_DOCK_APP_PREFIX = "dock_app_"
        private const val KEY_DOCK_SLOT_COUNT = "dock_slot_count"
        private const val KEY_DOCK_COLOR = "dock_color"
        private const val KEY_DOCK_SIZE = "dock_size"
        private const val KEY_DOCK_VISIBLE = "dock_visible"
        private const val KEY_DOCK_VISIBLE_PAGES = "dock_visible_pages"
        private const val KEY_MAX_DOCK_APPS = "max_dock_apps"
        const val MAX_DOCK_APPS = 5
    }
}
