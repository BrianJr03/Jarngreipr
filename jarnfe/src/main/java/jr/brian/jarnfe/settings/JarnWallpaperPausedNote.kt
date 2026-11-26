package jr.brian.jarnfe.settings

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.wallpaper.JarnWallpaperPause
import jr.brian.jarnfe.wallpaper.rememberJarnWallpaperPause

/**
 * Says why a moving wallpaper holds still. Shows nothing while wallpapers may move, so Battery
 * Saver only gets a note while "Pause animations on Battery Saver" is on.
 */
@Composable
internal fun JarnWallpaperPausedNote(hasMovingWallpaper: Boolean) {
    JarnWallpaperPausedNoteContent(rememberJarnWallpaperPause().takeIf { hasMovingWallpaper })
}

@Composable
internal fun JarnWallpaperPausedNoteContent(pause: JarnWallpaperPause?) {
    if (pause == null) return
    JarnText(
        text = stringResource(pause.noteRes),
        style = JarnTheme.type.caption,
        color = JarnTheme.colors.textMid,
    )
}

@get:StringRes
private val JarnWallpaperPause.noteRes: Int
    get() = when (this) {
        JarnWallpaperPause.BatterySaver -> R.string.jarnfe_wallpaper_paused_battery_saver
        JarnWallpaperPause.ReduceMotion -> R.string.jarnfe_wallpaper_paused_reduce_motion
    }
