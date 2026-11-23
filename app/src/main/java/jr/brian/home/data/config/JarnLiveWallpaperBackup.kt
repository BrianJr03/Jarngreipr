package jr.brian.home.data.config

import jr.brian.frontend.look.JARN_LIVE_WALLPAPER_OFF
import jr.brian.frontend.look.JarnLookStore

/** Always a value, so restoring this backup can also turn a live wallpaper off. */
internal fun JarnLookStore.liveWallpaperBackup(): String = liveWallpaperId ?: JARN_LIVE_WALLPAPER_OFF

/** Null means the backup predates v45, so the current choice stays. */
internal fun JarnLookStore.restoreLiveWallpaper(backup: String?) {
    backup?.let(::setLiveWallpaperId)
}
