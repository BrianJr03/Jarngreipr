package jr.brian.home.ui.util

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import jr.brian.home.ui.theme.managers.WallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Theme.Home sets windowBackground = @android:color/black so the system
 * wallpaper never flashes through during a screen-swap reparent. That
 * same choice silently covers the system wallpaper when the user picks
 * "System Wallpaper" (WallpaperType.TRANSPARENT) in Appearance, because
 * windowShowWallpaper only takes effect when the window is actually
 * transparent. Flip the window background to transparent live while
 * TRANSPARENT is selected and back to black for every other type.
 *
 * [launcherSyncSource] (frontend-only): when the primary manager's type
 * is [WallpaperType.LAUNCHER], follow this manager's type instead so the
 * frontend window transparency mirrors the main launcher's choice.
 */
fun <A> applyWallpaperWindowBackground(
    activity: A,
    wallpaperManager: WallpaperManager,
    launcherSyncSource: WallpaperManager? = null,
) where A : Activity, A : LifecycleOwner {
    fun effectiveType(): WallpaperType {
        val own = wallpaperManager.currentWallpaper.type
        return if (own == WallpaperType.LAUNCHER && launcherSyncSource != null) {
            launcherSyncSource.currentWallpaper.type
        } else {
            own
        }
    }

    activity.updateWallpaperWindowBackground(effectiveType())
    activity.lifecycleScope.launch {
        activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                snapshotFlow { wallpaperManager.currentWallpaper.type }
                    .distinctUntilChanged()
                    .collect { activity.updateWallpaperWindowBackground(effectiveType()) }
            }
            if (launcherSyncSource != null) {
                launch {
                    snapshotFlow { launcherSyncSource.currentWallpaper.type }
                        .distinctUntilChanged()
                        .collect { activity.updateWallpaperWindowBackground(effectiveType()) }
                }
            }
        }
    }
}

private fun Activity.updateWallpaperWindowBackground(type: WallpaperType) {
    val color = if (type == WallpaperType.TRANSPARENT) Color.TRANSPARENT else Color.BLACK
    window.setBackgroundDrawable(ColorDrawable(color))
}
