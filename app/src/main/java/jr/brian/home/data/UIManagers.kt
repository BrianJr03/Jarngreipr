package jr.brian.home.data

import jr.brian.home.ui.theme.managers.GlobalIconRefreshManager
import jr.brian.home.ui.theme.managers.WallpaperManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Container for UI and display-related managers.
 * 
 * These managers control how the user interface appears and behaves:
 * - Grid layouts and dimensions
 * - Icon display preferences
 * - Power state visualization
 * - Icon pack management
 * - Wallpaper management
 */
@Singleton
data class UIManagers @Inject constructor(
    /** Controls grid layout settings (rows, columns, unlimited mode) */
    val gridSettingsManager: GridSettingsManager,
    
    /** Manages how app icons are displayed (icon only, name only, both) */
    val appDisplayPreferenceManager: AppDisplayPreferenceManager,
    
    /** Manages power state (screen on/off simulation) */
    val powerSettingsManager: PowerSettingsManager,
    
    /** Manages icon pack selection and application */
    val iconPackManager: IconPackManager,
    
    /** Manages custom icons for individual apps */
    val customIconManager: CustomIconManager,
    
    /** Triggers global icon refresh when icon pack changes */
    val globalIconRefreshManager: GlobalIconRefreshManager,
    
    /** Manages wallpaper settings (image, video, GIF, ESDE) */
    val wallpaperManager: WallpaperManager
)
