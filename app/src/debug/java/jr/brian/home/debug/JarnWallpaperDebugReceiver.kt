package jr.brian.home.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.frontend.look.JARN_LIVE_WALLPAPER_OFF
import jr.brian.frontend.look.JarnLookStore
import jr.brian.home.di.FrontendWallpaper
import jr.brian.home.ui.theme.managers.WallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType
import javax.inject.Inject

/**
 * Debug builds only, for scripts/jarnfe-verify. Sets the Jarn FE wallpaper, live wallpaper and
 * Reduce motion straight from adb, then reports all three, so a script can restore what it found.
 * Writes the frontend wallpaper prefs as they are, so no picked file is copied or deleted.
 *
 *   am broadcast -a jr.brian.home.debug.JARN_WALLPAPER -n <pkg>/jr.brian.home.debug.JarnWallpaperDebugReceiver
 *     [--es type GIF|IMAGE|NONE|LAUNCHER|... --es uri <file uri, or empty for none>]
 *     [--es live <id, or off>] [--ez reduced true|false]
 */
@AndroidEntryPoint
class JarnWallpaperDebugReceiver : BroadcastReceiver() {
    @Inject
    @FrontendWallpaper
    lateinit var wallpaper: WallpaperManager

    @Inject
    lateinit var look: JarnLookStore

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        intent.getStringExtra(EXTRA_TYPE)?.let { type ->
            val parsed = WallpaperType.entries.firstOrNull { it.name == type } ?: return@let
            wallpaper.setWallpaper(intent.getStringExtra(EXTRA_URI)?.takeIf { it.isNotEmpty() }, parsed)
        }
        intent.getStringExtra(EXTRA_LIVE)?.let { look.setLiveWallpaperId(it) }
        if (intent.hasExtra(EXTRA_REDUCED)) look.setReducedMotion(intent.getBooleanExtra(EXTRA_REDUCED, false))
        val current = wallpaper.currentWallpaper
        resultData = listOf(
            "type=${current.type.name}",
            "uri=${current.uri.orEmpty()}",
            "live=${look.liveWallpaperId ?: JARN_LIVE_WALLPAPER_OFF}",
            "reduced=${look.reducedMotionState.value}",
        ).joinToString(" ")
    }

    private companion object {
        const val ACTION = "jr.brian.home.debug.JARN_WALLPAPER"
        const val EXTRA_TYPE = "type"
        const val EXTRA_URI = "uri"
        const val EXTRA_LIVE = "live"
        const val EXTRA_REDUCED = "reduced"
    }
}
