package jr.brian.home.data

import jr.brian.home.esde.preferences.ESDEPreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Container for feature-specific managers.
 * 
 * These managers handle specific launcher features:
 * - Dock configuration and app slots
 * - Gamepad/controller support
 * - ES-DE integration settings
 */
@Singleton
data class FeatureManagers @Inject constructor(
    /** Manages dock configuration and app slots */
    val dockManager: DockManager,
    
    /** Manages gamepad/controller button mappings */
    val controlPadManager: ControlPadManager,
    
    /** Manages ES-DE wallpaper preferences and settings */
    val esdePreferencesManager: ESDEPreferencesManager
)
