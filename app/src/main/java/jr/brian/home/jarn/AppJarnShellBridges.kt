package jr.brian.home.jarn

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
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
import jr.brian.home.util.WallpaperUtils
import jr.brian.jarnfe.dock.JarnDockApp
import jr.brian.jarnfe.dock.JarnDockData
import jr.brian.jarnfe.shell.JarnScreenSwap
import jr.brian.jarnfe.wallpaper.JarnWallpaperSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
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

/**
 * Video, system, and ES-DE wallpapers fall back to the theme background for now. Picked files get
 * their own folder because Classic's wallpaper copy keeps only the newest file in its folder.
 */
@Singleton
class AppJarnWallpaperSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:FrontendWallpaper private val frontend: Provider<WallpaperManager>,
    private val launcher: Provider<WallpaperManager>,
) : JarnWallpaperSource {
    override val currentUri: String?
        get() {
            val info = effectiveFrontendWallpaper(frontend.get(), launcher.get())
            return info.uri
                .takeIf { info.type == WallpaperType.IMAGE || info.type == WallpaperType.GIF }
                ?.takeUnless(::isMissingFile)
        }

    /** Backups carry the wallpaper's path but not its file, so a restore can point at nothing. */
    private fun isMissingFile(uri: String): Boolean =
        uri.startsWith(FILE_SCHEME) && !File(uri.removePrefix(FILE_SCHEME)).exists()

    override suspend fun applyPicked(uri: Uri): Boolean {
        val copied = withContext(Dispatchers.IO) {
            val type = WallpaperUtils.detectWallpaperType(context, uri)
            if (type != WallpaperType.IMAGE && type != WallpaperType.GIF) return@withContext null
            WallpaperUtils.copyMediaToInternalStorage(context, uri, JARN_WALLPAPER_DIR, JARN_WALLPAPER_PREFIX)
                ?.let { it to type }
        } ?: return false
        val manager = frontend.get()
        val previous = manager.currentWallpaper.uri
        manager.setWallpaper(copied.first, copied.second)
        withContext(Dispatchers.IO) { deleteOwnedFile(previous) }
        return true
    }

    override fun useThemeBackground() {
        val manager = frontend.get()
        val previous = manager.currentWallpaper.uri
        manager.setDefault()
        deleteOwnedFile(previous)
    }

    /** Only touches this bridge's folder, so a Classic wallpaper file is never removed. */
    private fun deleteOwnedFile(uri: String?) {
        val path = uri?.removePrefix(FILE_SCHEME) ?: return
        val file = File(path)
        if (file.parentFile == File(context.filesDir, JARN_WALLPAPER_DIR)) file.delete()
    }

    private companion object {
        const val JARN_WALLPAPER_DIR = "jarn_wallpapers"
        const val JARN_WALLPAPER_PREFIX = "jarn_wallpaper_"
        const val FILE_SCHEME = "file://"
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
