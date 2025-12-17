package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

class PageCountManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _pageCount = MutableStateFlow(loadPageCount())

    private fun loadPageCount(): Int {
        return prefs.getInt(KEY_PAGE_COUNT, DEFAULT_PAGE_COUNT)
    }

    fun setPageCount(count: Int) {
        val validCount = count.coerceIn(MIN_PAGE_COUNT, MAX_PAGE_COUNT)
        _pageCount.value = validCount
        prefs.edit().apply {
            putInt(KEY_PAGE_COUNT, validCount)
            apply()
        }
    }

    fun addPage() {
        if (_pageCount.value < MAX_PAGE_COUNT) {
            setPageCount(_pageCount.value + 1)
        }
    }

    fun removePage() {
        if (_pageCount.value > MIN_PAGE_COUNT) {
            setPageCount(_pageCount.value - 1)
        }
    }

    companion object {
        private const val PREFS_NAME = "page_count_prefs"
        private const val KEY_PAGE_COUNT = "page_count"
        const val DEFAULT_PAGE_COUNT = 2
        const val MIN_PAGE_COUNT = 0
        const val MAX_PAGE_COUNT = 2
    }
}
