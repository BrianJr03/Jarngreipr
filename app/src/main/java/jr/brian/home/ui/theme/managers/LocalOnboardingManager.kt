package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.OnboardingManager

val LocalOnboardingManager = staticCompositionLocalOf<OnboardingManager> {
    error("No OnboardingManager provided")
}
