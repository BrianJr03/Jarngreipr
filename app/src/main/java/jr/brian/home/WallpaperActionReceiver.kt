package jr.brian.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.data.ManagerContainer
import jr.brian.home.util.WallpaperUtils
import javax.inject.Inject

@AndroidEntryPoint
class WallpaperActionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var managers: ManagerContainer

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SET_ESDE_WALLPAPER -> {
                managers.ui.wallpaperManager.setESDE()
            }

            ACTION_SET_STANDARD_WALLPAPER -> {
                val target =
                    managers.feature.esdePreferencesManager.state.value.wallpaperToggleTarget
                WallpaperUtils.useStandardWallpaper(
                    target = target,
                    wallpaperManager = managers.ui.wallpaperManager
                )
            }
        }
    }

    companion object {
        const val ACTION_SET_ESDE_WALLPAPER = "jr.brian.SET_ESDE_WALLPAPER"
        const val ACTION_SET_STANDARD_WALLPAPER = "jr.brian.SET_STANDARD_WALLPAPER"
    }
}
