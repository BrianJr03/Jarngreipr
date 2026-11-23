package jr.brian.home.jarn

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.core.model.ScreenAssignment
import jr.brian.home.canvas.jarn.AppJarnCanvasData
import jr.brian.home.data.DockManager
import jr.brian.home.data.ScreenAssignmentStore
import jr.brian.home.data.ScreenFocusLockWriter
import jr.brian.home.di.FrontendWallpaper
import jr.brian.home.ui.theme.managers.WallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType
import jr.brian.home.ui.theme.managers.effectiveFrontendWallpaper
import jr.brian.home.util.ThorDetection
import jr.brian.jarnfe.dock.JarnDockApp
import jr.brian.jarnfe.dock.JarnDockData
import jr.brian.jarnfe.shell.JarnScreenSwap
import jr.brian.jarnfe.wallpaper.JarnWallpaperSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Stage maps to Classic's frontend slot, so both experiences share one saved choice. Focus Lock
 * is left to [syncInputTo], which targets whichever screen is active rather than always Stage.
 */
@Singleton
class AppJarnScreenSwap @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val store: ScreenAssignmentStore,
) : JarnScreenSwap {
    override val isSwapped: Boolean
        get() = store.assignment == ScreenAssignment.FRONTEND_ON_BOTTOM

    override val swapped: Flow<Boolean> =
        store.state.map { it == ScreenAssignment.FRONTEND_ON_BOTTOM }

    override fun requestSwap() {
        if (ThorDetection.isThor()) store.flip()
    }

    override fun syncInputTo(displayId: Int) {
        ScreenFocusLockWriter.syncToDisplayId(context, displayId)
    }
}

/** Read-only. Video, system, and ES-DE wallpapers fall back to the theme background for now. */
@Singleton
class AppJarnWallpaperSource @Inject constructor(
    @param:FrontendWallpaper private val frontend: Provider<WallpaperManager>,
    private val launcher: Provider<WallpaperManager>,
) : JarnWallpaperSource {
    override val currentUri: String?
        get() {
            val info = effectiveFrontendWallpaper(frontend.get(), launcher.get())
            return info.uri.takeIf { info.type == WallpaperType.IMAGE || info.type == WallpaperType.GIF }
        }
}

@Singleton
class AppJarnDockData @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dock: DockManager,
    private val canvas: Provider<AppJarnCanvasData>,
) : JarnDockData {
    override val apps: Flow<List<JarnDockApp>> =
        combine(dock.dockApps, dock.isDockVisible) { packages, visible ->
            if (visible) packages.filter { it.isNotEmpty() }.mapNotNull(::resolve) else emptyList()
        }.flowOn(Dispatchers.IO)

    override fun launch(host: Activity, packageName: String) {
        canvas.get().launchApp(host, packageName)
    }

    private fun resolve(packageName: String): JarnDockApp? {
        val packages = context.packageManager
        return try {
            val info = packages.getApplicationInfo(packageName, 0)
            JarnDockApp(packageName, packages.getApplicationLabel(info).toString(), packages.getApplicationIcon(info))
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}
