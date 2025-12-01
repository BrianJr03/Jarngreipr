package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeTabManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _homeTabIndex = MutableStateFlow(loadHomeTabIndex())
    val homeTabIndex: StateFlow<Int> = _homeTabIndex.asStateFlow()

    private fun loadHomeTabIndex(): Int {
        return prefs.getInt(KEY_HOME_TAB_INDEX, DEFAULT_HOME_TAB_INDEX)
    }

    fun setHomeTabIndex(index: Int) {
        _homeTabIndex.value = index
        prefs.edit().apply {
            putInt(KEY_HOME_TAB_INDEX, index)
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "home_tab_prefs"
        private const val KEY_HOME_TAB_INDEX = "home_tab_index"
        private const val DEFAULT_HOME_TAB_INDEX = 0
    }
}
