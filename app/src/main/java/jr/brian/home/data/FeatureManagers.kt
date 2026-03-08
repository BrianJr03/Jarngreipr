package jr.brian.home.data

import androidx.media3.common.util.UnstableApi
import coil.ImageLoader
import jr.brian.home.di.ESDEImageLoader
import jr.brian.home.esde.data.ESDEEventManager
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.ui.video.VideoPresentationManager
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
@UnstableApi
@Singleton
data class FeatureManagers @Inject constructor(
    val dockManager: DockManager,
    val controlPadManager: ControlPadManager,
    val appDrawerFabManager: AppDrawerFabManager,
    val esdePreferencesManager: ESDEPreferencesManager,
    val esdeEventManager: ESDEEventManager,
    val videoPresentationManager: VideoPresentationManager,
    val gameKonfettiManager: GameKonfettiManager,
    val floatyModeManager: FloatyModeManager,
    @param:ESDEImageLoader val esdeImageLoader: ImageLoader
)
