package jr.brian.home.data

import javax.inject.Inject

/**
 * Container class that groups all manager dependencies together.
 */
data class ManagerContainer @Inject constructor(
    val appVisibilityManager: AppVisibilityManager,
    val gridSettingsManager: GridSettingsManager,
    val appDisplayPreferenceManager: AppDisplayPreferenceManager,
    val powerSettingsManager: PowerSettingsManager,
    val widgetPageAppManager: WidgetPageAppManager,
    val homeTabManager: HomeTabManager,
    val onboardingManager: OnboardingManager,
    val appPositionManager: AppPositionManager,
    val pageCountManager: PageCountManager,
    val pageTypeManager: PageTypeManager,
    val iconPackManager: IconPackManager
)