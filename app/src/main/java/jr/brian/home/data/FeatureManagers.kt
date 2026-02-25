package jr.brian.home.data

import coil.ImageLoader
import jr.brian.home.di.ESDEImageLoader
import jr.brian.home.esde.data.ESDEPreferencesManager
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
    val dockManager: DockManager,
    val controlPadManager: ControlPadManager,
    val appDrawerFabManager: AppDrawerFabManager,
    val esdePreferencesManager: ESDEPreferencesManager,
    val gameKonfettiManager: GameKonfettiManager,
    @param:ESDEImageLoader val esdeImageLoader: ImageLoader
)
