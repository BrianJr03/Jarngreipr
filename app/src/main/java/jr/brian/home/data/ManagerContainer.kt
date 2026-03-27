package jr.brian.home.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import jr.brian.home.esde.data.LocalESDEPreferencesManager
import jr.brian.home.esde.util.LocalESDEImageLoader
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppDrawerFabManager
import jr.brian.home.ui.theme.managers.LocalAppPositionManager
import jr.brian.home.ui.theme.managers.LocalAppUpdateManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalControlPadManager
import jr.brian.home.ui.theme.managers.LocalCustomAppNameManager
import jr.brian.home.ui.theme.managers.LocalCustomIconManager
import jr.brian.home.ui.theme.managers.LocalDockManager
import jr.brian.home.ui.theme.managers.LocalBgMusicManager
import jr.brian.home.ui.theme.managers.LocalFloatyModeManager
import jr.brian.home.ui.theme.managers.LocalFolderManager
import jr.brian.home.ui.theme.managers.LocalGameKonfettiManager
import jr.brian.home.ui.theme.managers.LocalGlobalIconRefreshManager
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.theme.managers.LocalHomeTabManager
import jr.brian.home.ui.theme.managers.LocalIconPackManager
import jr.brian.home.ui.theme.managers.LocalNotificationCountManager
import jr.brian.home.ui.theme.managers.LocalOnboardingManager
import jr.brian.home.ui.theme.managers.LocalPageCountManager
import jr.brian.home.ui.theme.managers.LocalPageTypeManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.ui.theme.managers.LocalRecentAppsCacheManager
import jr.brian.home.ui.theme.managers.LocalSearchLayoutManager
import jr.brian.home.ui.theme.managers.LocalShizukuManager
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.theme.managers.LocalWhatsNewManager
import jr.brian.home.ui.theme.managers.LocalWidgetPageAppManager
import javax.inject.Inject

/**
 * Top-level container that composes all manager groups together.
 * 
 * - **UIManagers**: Display and appearance settings
 * - **AppManagers**: App organization and state
 * - **PageManagers**: Multi-page functionality
 * - **FeatureManagers**: Specific launcher features
 * - **SystemManagers**: System integration and UX
 * 
 * All managers are injected via Hilt and scoped as Singletons at the application level.
 * Managers are accessed through CompositionLocal providers (e.g., `LocalXxxManager.current`)
 */
data class ManagerContainer @Inject constructor(
    val ui: UIManagers,
    val app: AppManagers,
    val page: PageManagers,
    val feature: FeatureManagers,
    val system: SystemManagers
)

/**
 * Extension function that provides all managers to the Compose tree via CompositionLocal.
 * 
 * This allows any Composable function in the tree to access managers without
 * passing them through multiple layers of function parameters.
 * 
 * The managers are organized by domain for better maintainability:
 * - UI managers control appearance and display
 * - App managers handle app state and organization
 * - Page managers control multi-page functionality
 * - Feature managers provide specific launcher features
 * - System managers integrate with Android system and handle UX
 * 
 * Usage:
 * ```
 * managerContainer.ManagerCompositionLocalProvider {
 *     // Your composable content here
 *     // Access managers via: val manager = LocalXxxManager.current
 * }
 * ```
 */
@Composable
fun ManagerContainer.ManagerCompositionLocalProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        // UI & Display
        LocalGridSettingsManager provides ui.gridSettingsManager,
        LocalAppDisplayPreferenceManager provides ui.appDisplayPreferenceManager,
        LocalPowerSettingsManager provides ui.powerSettingsManager,
        LocalIconPackManager provides ui.iconPackManager,
        LocalCustomIconManager provides ui.customIconManager,
        LocalGlobalIconRefreshManager provides ui.globalIconRefreshManager,
        LocalWallpaperManager provides ui.wallpaperManager,
        LocalSearchLayoutManager provides ui.searchLayoutManager,
        LocalCustomAppNameManager provides ui.customAppNameManager,
        
        // App Management
        LocalAppVisibilityManager provides app.appVisibilityManager,
        LocalAppPositionManager provides app.appPositionManager,
        LocalFolderManager provides app.folderManager,
        LocalRecentAppsCacheManager provides app.recentAppsCacheManager,
        
        // Page Management
        LocalPageCountManager provides page.pageCountManager,
        LocalPageTypeManager provides page.pageTypeManager,
        LocalHomeTabManager provides page.homeTabManager,
        LocalWidgetPageAppManager provides page.widgetPageAppManager,
        
        // Feature Management
        LocalDockManager provides feature.dockManager,
        LocalControlPadManager provides feature.controlPadManager,
        LocalESDEPreferencesManager provides feature.esdePreferencesManager,
        LocalESDEImageLoader provides feature.esdeImageLoader,
        LocalAppDrawerFabManager provides feature.appDrawerFabManager,
        LocalGameKonfettiManager provides feature.gameKonfettiManager,
        LocalFloatyModeManager provides feature.floatyModeManager,
        LocalJinglesManager provides feature.jinglesManager,
        LocalBgMusicManager provides feature.bgMusicManager,
        
        // System Integration
        LocalNotificationCountManager provides system.notificationCountManager,
        LocalShizukuManager provides system.shizukuManager,
        LocalAppUpdateManager provides system.appUpdateManager,
        LocalOnboardingManager provides system.onboardingManager,
        LocalWhatsNewManager provides system.whatsNewManager
    ) {
        content()
    }
}