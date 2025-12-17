package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import jr.brian.home.model.AppLayout
import jr.brian.home.model.AppPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AppLayoutManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _layoutsByPage = MutableStateFlow<Map<Int, List<AppLayout>>>(emptyMap())
    val layoutsByPage: StateFlow<Map<Int, List<AppLayout>>> = _layoutsByPage.asStateFlow()

    private val _activeLayoutIdByPage = MutableStateFlow<Map<Int, String?>>(emptyMap())
    val activeLayoutIdByPage: StateFlow<Map<Int, String?>> = _activeLayoutIdByPage.asStateFlow()

    init {
        loadAllLayouts()
    }

    private fun loadAllLayouts() {
        val maxPages = 10
        val layoutsMap = mutableMapOf<Int, List<AppLayout>>()
        val activeLayoutMap = mutableMapOf<Int, String?>()

        for (pageIndex in 0 until maxPages) {
            val layouts = loadLayoutsForPage(pageIndex)
            if (layouts.isNotEmpty()) {
                layoutsMap[pageIndex] = layouts
            }
            val activeLayoutId = prefs.getString("${KEY_ACTIVE_LAYOUT}_$pageIndex", null)
            activeLayoutMap[pageIndex] = activeLayoutId
        }

        _layoutsByPage.value = layoutsMap
        _activeLayoutIdByPage.value = activeLayoutMap
    }

    private fun loadLayoutsForPage(pageIndex: Int): List<AppLayout> {
        val layoutsJson = prefs.getString("${KEY_LAYOUTS}_$pageIndex", null) ?: return emptyList()
        val layouts = mutableListOf<AppLayout>()

        try {
            layoutsJson.split(SEPARATOR_LAYOUTS).forEach { layoutData ->
                if (layoutData.isNotBlank()) {
                    val parts = layoutData.split(SEPARATOR_LAYOUT_FIELDS)
                    if (parts.size >= 2) {
                        val id = parts[0]
                        val name = parts[1]
                        val timestamp = if (parts.size >= 3) {
                            parts[2].toLongOrNull() ?: System.currentTimeMillis()
                        } else {
                            System.currentTimeMillis()
                        }
                        val positionsData = if (parts.size >= 4) parts[3] else ""
                        val positions = parsePositions(positionsData)
                        layouts.add(AppLayout(id, name, positions, timestamp))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return layouts
    }

    private fun parsePositions(positionsData: String): Map<String, AppPosition> {
        if (positionsData.isBlank()) return emptyMap()

        val positions = mutableMapOf<String, AppPosition>()
        try {
            positionsData.split(SEPARATOR_APPS).forEach { appData ->
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
                        positions[packageName] = AppPosition(packageName, x, y, iconSize)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return positions
    }

    private fun serializePositions(positions: Map<String, AppPosition>): String {
        return positions.values.joinToString(SEPARATOR_APPS) { position ->
            "${position.packageName}$SEPARATOR_COORDS${position.x}$SEPARATOR_COORDS${position.y}$SEPARATOR_COORDS${position.iconSize}"
        }
    }

    fun saveLayout(pageIndex: Int, name: String, positions: Map<String, AppPosition>): Boolean {
        val currentLayouts = _layoutsByPage.value[pageIndex] ?: emptyList()
        if (currentLayouts.size >= MAX_LAYOUTS_PER_PAGE) {
            return false
        }

        val layoutId = UUID.randomUUID().toString()
        val newLayout = AppLayout(layoutId, name, positions)
        val updatedLayouts = currentLayouts + newLayout

        _layoutsByPage.value += (pageIndex to updatedLayouts)
        saveLayoutsForPage(pageIndex)
        return true
    }

    fun updateLayoutName(pageIndex: Int, layoutId: String, newName: String) {
        val currentLayouts = _layoutsByPage.value[pageIndex] ?: return
        val updatedLayouts = currentLayouts.map { layout ->
            if (layout.id == layoutId) {
                layout.copy(name = newName)
            } else {
                layout
            }
        }

        _layoutsByPage.value += (pageIndex to updatedLayouts)
        saveLayoutsForPage(pageIndex)
    }

    fun deleteLayout(pageIndex: Int, layoutId: String) {
        val currentLayouts = _layoutsByPage.value[pageIndex] ?: return
        val updatedLayouts = currentLayouts.filter { it.id != layoutId }

        _layoutsByPage.value += (pageIndex to updatedLayouts)
        saveLayoutsForPage(pageIndex)

        val activeLayoutId = _activeLayoutIdByPage.value[pageIndex]
        if (activeLayoutId == layoutId) {
            setActiveLayout(pageIndex, null)
        }
    }

    fun setActiveLayout(pageIndex: Int, layoutId: String?) {
        _activeLayoutIdByPage.value += (pageIndex to layoutId)
        prefs.edit().apply {
            if (layoutId != null) {
                putString("${KEY_ACTIVE_LAYOUT}_$pageIndex", layoutId)
            } else {
                remove("${KEY_ACTIVE_LAYOUT}_$pageIndex")
            }
            apply()
        }
    }

    fun getActiveLayout(pageIndex: Int): AppLayout? {
        val activeLayoutId = _activeLayoutIdByPage.value[pageIndex] ?: return null
        return _layoutsByPage.value[pageIndex]?.find { it.id == activeLayoutId }
    }

    fun getLayoutsForPage(pageIndex: Int): List<AppLayout> {
        return _layoutsByPage.value[pageIndex] ?: emptyList()
    }

    private fun saveLayoutsForPage(pageIndex: Int) {
        val layouts = _layoutsByPage.value[pageIndex] ?: return
        val layoutsJson = layouts.joinToString(SEPARATOR_LAYOUTS) { layout ->
            val positionsData = serializePositions(layout.positions)
            "${layout.id}$SEPARATOR_LAYOUT_FIELDS${layout.name}$SEPARATOR_LAYOUT_FIELDS${layout.timestamp}$SEPARATOR_LAYOUT_FIELDS$positionsData"
        }
        prefs.edit().apply {
            putString("${KEY_LAYOUTS}_$pageIndex", layoutsJson)
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "app_layout_prefs"
        private const val KEY_LAYOUTS = "layouts"
        private const val KEY_ACTIVE_LAYOUT = "active_layout"
        private const val SEPARATOR_LAYOUTS = "|||"
        private const val SEPARATOR_LAYOUT_FIELDS = "~~"
        private const val SEPARATOR_APPS = "||"
        private const val SEPARATOR_COORDS = ","
        const val MAX_LAYOUTS_PER_PAGE = 5
    }
}
