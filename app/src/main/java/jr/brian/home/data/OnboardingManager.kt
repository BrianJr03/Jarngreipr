package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OnboardingManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _hasCompletedOnboarding = MutableStateFlow(loadOnboardingState())
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _hasSeenSliderHint = MutableStateFlow(loadSliderHintState())
    val hasSeenSliderHint: StateFlow<Boolean> = _hasSeenSliderHint.asStateFlow()

    private fun loadOnboardingState(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    private fun loadSliderHintState(): Boolean {
        return prefs.getBoolean(KEY_SLIDER_HINT_SEEN, false)
    }

    fun markOnboardingComplete() {
        _hasCompletedOnboarding.value = true
        prefs.edit().apply {
            putBoolean(KEY_ONBOARDING_COMPLETED, true)
            apply()
        }
    }

    fun markSliderHintSeen() {
        _hasSeenSliderHint.value = true
        prefs.edit().apply {
            putBoolean(KEY_SLIDER_HINT_SEEN, true)
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "onboarding_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_SLIDER_HINT_SEEN = "slider_hint_seen"
    }
}
