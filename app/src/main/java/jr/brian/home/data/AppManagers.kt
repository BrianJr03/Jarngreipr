package jr.brian.home.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Container for app management-related managers.
 * 
 * These managers handle app state, positioning, organization, and visibility:
 * - Which apps appear on each page
 * - Where apps are positioned
 * - Folder organization
 * - Recent app tracking
 */
@Singleton
data class AppManagers @Inject constructor(
    /** Controls which apps are visible on each page */
    val appVisibilityManager: AppVisibilityManager,
    
    /** Manages app positions in grid and free-position modes */
    val appPositionManager: AppPositionManager,
    
    /** Manages folder creation and organization */
    val folderManager: FolderManager,
    
    /** Caches recently opened apps for quick access */
    val recentAppsCacheManager: RecentAppsCacheManager
)
