package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PageType {
    APPS_TAB,
    APPS_AND_WIDGETS_TAB
}

class PageTypeManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _pageTypes = MutableStateFlow(loadPageTypes())
    val pageTypes: StateFlow<List<PageType>> = _pageTypes.asStateFlow()

    private fun loadPageTypes(): List<PageType> {
        val count = prefs.getInt(KEY_PAGE_COUNT, 0)
        if (count == 0) {
            val defaultType = PageType.APPS_TAB
            prefs.edit().apply {
                putInt(KEY_PAGE_COUNT, 1)
                putString("${KEY_PAGE_TYPE_PREFIX}0", defaultType.name)
                apply()
            }
            return listOf(defaultType)
        }
        return (0 until count).map { index ->
            val typeString = prefs.getString("${KEY_PAGE_TYPE_PREFIX}$index", null)
            if (typeString != null) {
                try {
                    PageType.valueOf(typeString)
                } catch (_: IllegalArgumentException) {
                    PageType.APPS_TAB
                }
            } else {
                PageType.APPS_TAB
            }
        }
    }

    fun addPage(type: PageType) {
        val currentTypes = _pageTypes.value.toMutableList()
        currentTypes.add(type)
        _pageTypes.value = currentTypes
        val newIndex = currentTypes.size - 1
        prefs.edit().apply {
            putInt(KEY_PAGE_COUNT, currentTypes.size)
            putString("${KEY_PAGE_TYPE_PREFIX}$newIndex", type.name)
            apply()
        }
    }

    fun removePage(index: Int) {
        val currentTypes = _pageTypes.value.toMutableList()
        if (index in currentTypes.indices) {
            currentTypes.removeAt(index)
            _pageTypes.value = currentTypes
            prefs.edit().apply {
                putInt(KEY_PAGE_COUNT, currentTypes.size)
                currentTypes.forEachIndexed { idx, pageType ->
                    putString("${KEY_PAGE_TYPE_PREFIX}$idx", pageType.name)
                }
                remove("${KEY_PAGE_TYPE_PREFIX}${currentTypes.size}")
                apply()
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "page_type_prefs"
        private const val KEY_PAGE_TYPE_PREFIX = "page_type_"
        private const val KEY_PAGE_COUNT = "page_count"
    }
}
