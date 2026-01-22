package jr.brian.home.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import jr.brian.home.ui.theme.managers.GlobalIconRefreshManager
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppPositionManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalCustomIconManager
import jr.brian.home.ui.theme.managers.LocalFolderManager
import jr.brian.home.ui.theme.managers.LocalGlobalIconRefreshManager
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.theme.managers.LocalHomeTabManager
import jr.brian.home.ui.theme.managers.LocalIconPackManager
import jr.brian.home.ui.theme.managers.LocalNotificationCountManager
import jr.brian.home.ui.theme.managers.LocalOnboardingManager
import jr.brian.home.ui.theme.managers.LocalRecentAppsCacheManager
import jr.brian.home.ui.theme.managers.LocalAppUpdateManager
import jr.brian.home.ui.theme.managers.LocalShizukuManager
import jr.brian.home.ui.theme.managers.LocalPageCountManager
import jr.brian.home.ui.theme.managers.LocalPageTypeManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.ui.theme.managers.LocalWhatsNewManager
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
    val iconPackManager: IconPackManager,
    val whatsNewManager: WhatsNewManager,
    val customIconManager: CustomIconManager,
    val globalIconRefreshManager: GlobalIconRefreshManager,
    val folderManager: FolderManager,
    val notificationCountManager: NotificationCountManager,
    val recentAppsCacheManager: RecentAppsCacheManager,
    val shizukuManager: ShizukuManager,
    val appUpdateManager: AppUpdateManager
) {
    init {
        NotificationCountManager.setInstance(notificationCountManager)
        shizukuManager.initialize()
    }
}

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
        LocalIconPackManager provides iconPackManager,
        LocalWhatsNewManager provides whatsNewManager,
        LocalCustomIconManager provides customIconManager,
        LocalGlobalIconRefreshManager provides globalIconRefreshManager,
        LocalFolderManager provides folderManager,
        LocalNotificationCountManager provides notificationCountManager,
        LocalRecentAppsCacheManager provides recentAppsCacheManager,
        LocalShizukuManager provides shizukuManager,
        LocalAppUpdateManager provides appUpdateManager
    ) {
        content()
    }
}