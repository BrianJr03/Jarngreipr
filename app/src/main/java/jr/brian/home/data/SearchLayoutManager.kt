package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SearchLayoutManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var _isHorizontalLayout by mutableStateOf(loadIsHorizontalLayout())
    val isHorizontalLayout: Boolean
        get() = _isHorizontalLayout

    private var _hasCompletedOnboarding by mutableStateOf(loadOnboardingState())
    val hasCompletedOnboarding: Boolean
        get() = _hasCompletedOnboarding

    private fun loadIsHorizontalLayout(): Boolean {
        return prefs.getBoolean(KEY_IS_HORIZONTAL_LAYOUT, false)
    }

    private fun loadOnboardingState(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setHorizontalLayout(isHorizontal: Boolean) {
        _isHorizontalLayout = isHorizontal
        prefs.edit().apply {
            putBoolean(KEY_IS_HORIZONTAL_LAYOUT, isHorizontal)
            apply()
        }
    }

    fun toggleLayout() {
        setHorizontalLayout(!_isHorizontalLayout)
    }

    fun markOnboardingComplete() {
        _hasCompletedOnboarding = true
        prefs.edit().apply {
            putBoolean(KEY_ONBOARDING_COMPLETED, true)
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "search_layout_prefs"
        private const val KEY_IS_HORIZONTAL_LAYOUT = "is_horizontal_layout"
        private const val KEY_ONBOARDING_COMPLETED = "search_onboarding_completed"
    }
}
