package jr.brian.home.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppPositionManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.theme.managers.LocalHomeTabManager
import jr.brian.home.ui.theme.managers.LocalIconPackManager
import jr.brian.home.ui.theme.managers.LocalOnboardingManager
import jr.brian.home.ui.theme.managers.LocalPageCountManager
import jr.brian.home.ui.theme.managers.LocalPageTypeManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.ui.theme.managers.LocalWidgetPageAppManager
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

@Composable
fun ManagerContainer.ManagerCompositionLocalProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppVisibilityManager provides appVisibilityManager,
        LocalGridSettingsManager provides gridSettingsManager,
        LocalAppDisplayPreferenceManager provides appDisplayPreferenceManager,
        LocalPowerSettingsManager provides powerSettingsManager,
        LocalWidgetPageAppManager provides widgetPageAppManager,
        LocalHomeTabManager provides homeTabManager,
        LocalOnboardingManager provides onboardingManager,
        LocalAppPositionManager provides appPositionManager,
        LocalPageCountManager provides pageCountManager,
        LocalPageTypeManager provides pageTypeManager,
        LocalIconPackManager provides iconPackManager
    ) {
        content()
    }
}