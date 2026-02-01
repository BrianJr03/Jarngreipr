package jr.brian.home.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Container for feature-specific managers.
 * 
 * These managers handle specific launcher features:
 * - Dock configuration and app slots
 * - Gamepad/controller support
 */
@Singleton
data class FeatureManagers @Inject constructor(
    /** Manages dock configuration and app slots */
    val dockManager: DockManager,
    
    /** Manages gamepad/controller button mappings */
    val controlPadManager: ControlPadManager
)
