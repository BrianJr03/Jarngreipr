package jr.brian.home.jarn

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.frontend.look.JarnLookStore
import jr.brian.home.di.FrontendWallpaper
import jr.brian.home.ui.theme.managers.WallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType
import jr.brian.home.ui.theme.managers.effectiveFrontendWallpaper
import jr.brian.home.util.WallpaperUtils
import jr.brian.jarnfe.wallpaper.JarnWallpaperScreen
import jr.brian.jarnfe.wallpaper.JarnWallpaperSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Video, system, and ES-DE wallpapers fall back to the theme background for now. Picked files get
 * their own folder because Classic's wallpaper copy keeps only the newest file in its folder.
 *
 * The top screen (and both, while they share) uses the frontend [WallpaperManager], so Classic sees
 * no change. The bottom screen's own file is kept in [JarnLookStore]. Each screen gets its own copy,
 * and a file is only deleted once neither screen points at it.
 */
@Singleton
class AppJarnWallpaperSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:FrontendWallpaper private val frontend: Provider<WallpaperManager>,
    private val launcher: Provider<WallpaperManager>,
    private val look: Provider<JarnLookStore>,
) : JarnWallpaperSource {
    override val currentUri: String?
        get() {
            val info = effectiveFrontendWallpaper(frontend.get(), launcher.get())
            return info.uri
                .takeIf { info.type == WallpaperType.IMAGE || info.type == WallpaperType.GIF }
                ?.takeUnless(::isMissingFile)
        }

    init {
        sweepOrphanedBottomFiles()
    }

    override fun usableOrNull(uri: String?): String? = uri?.takeUnless(::isMissingFile)

    /** Backups carry the wallpaper's path but not its file, so a restore can point at nothing. */
    private fun isMissingFile(uri: String): Boolean =
        uri.startsWith(FILE_SCHEME) && !File(uri.removePrefix(FILE_SCHEME)).exists()

    override suspend fun applyPicked(uri: Uri, screen: JarnWallpaperScreen): Boolean {
        val copied = copyPicked(uri, prefixFor(screen)) ?: return false
        when (screen) {
            JarnWallpaperScreen.Top -> {
                val manager = frontend.get()
                val previous = manager.currentWallpaper.uri
                manager.setWallpaper(copied.first, copied.second)
                withContext(Dispatchers.IO) { deleteUnused(previous) }
            }
            JarnWallpaperScreen.Bottom -> replaceBottom(copied.first)
        }
        return true
    }

    override fun useThemeBackground(screen: JarnWallpaperScreen) {
        when (screen) {
            JarnWallpaperScreen.Top -> {
                val manager = frontend.get()
                val previous = manager.currentWallpaper.uri
                manager.setDefault()
                deleteUnused(previous)
            }
            JarnWallpaperScreen.Bottom -> {
                val store = look.get()
                val previous = store.bottomWallpaperUri
                store.setBottomWallpaperUri(null)
                deleteUnused(previous)
            }
        }
    }

    override suspend fun setSameOnBothScreens(same: Boolean) {
        val store = look.get()
        if (same == store.sameWallpaperOnBoth) return
        if (same) {
            val previous = store.bottomWallpaperUri
            store.setSameWallpaperOnBoth(true)
            store.setBottomWallpaperUri(null)
            withContext(Dispatchers.IO) { deleteUnused(previous) }
        } else {
            val top = currentUri
            val copy = top?.let { copyPicked(Uri.parse(it), BOTTOM_PREFIX)?.first }
            store.setBottomWallpaperUri(copy)
            store.setSameWallpaperOnBoth(false)
        }
    }

    private suspend fun replaceBottom(uri: String) {
        val store = look.get()
        val previous = store.bottomWallpaperUri
        store.setBottomWallpaperUri(uri)
        withContext(Dispatchers.IO) { deleteUnused(previous) }
    }

    private suspend fun copyPicked(uri: Uri, prefix: String): Pair<String, WallpaperType>? =
        withContext(Dispatchers.IO) {
            val type = WallpaperUtils.detectWallpaperType(context, uri)
            if (type != WallpaperType.IMAGE && type != WallpaperType.GIF) return@withContext null
            WallpaperUtils.copyMediaToInternalStorage(context, uri, JARN_WALLPAPER_DIR, prefix)
                ?.let { it to type }
        }

    private fun prefixFor(screen: JarnWallpaperScreen) =
        if (screen == JarnWallpaperScreen.Bottom) BOTTOM_PREFIX else TOP_PREFIX

    /**
     * Only touches this bridge's folder, so a Classic wallpaper file is never removed, and never a
     * file the other screen still shows.
     */
    private fun deleteUnused(uri: String?) {
        if (uri == null || uri == frontend.get().currentWallpaper.uri || uri == look.get().bottomWallpaperUri) return
        val file = File(uri.removePrefix(FILE_SCHEME))
        if (file.parentFile == File(context.filesDir, JARN_WALLPAPER_DIR)) file.delete()
    }

    /**
     * A restore can turn sharing on or point the bottom screen elsewhere without seeing the files,
     * so bottom files nothing points at are removed once per start.
     */
    internal fun sweepOrphanedBottomFiles() {
        val dir = File(context.filesDir, JARN_WALLPAPER_DIR)
        dir.listFiles { file -> file.name.startsWith(BOTTOM_PREFIX) }?.forEach { file ->
            deleteUnused(FILE_SCHEME + file.absolutePath)
        }
    }

    private companion object {
        const val JARN_WALLPAPER_DIR = "jarn_wallpapers"
        const val TOP_PREFIX = "jarn_wallpaper_"
        const val BOTTOM_PREFIX = "jarn_wallpaper_bottom_"
        const val FILE_SCHEME = "file://"
    }
}
