package jr.brian.home.jarn

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import jr.brian.frontend.look.JarnLookStore
import jr.brian.home.ui.theme.managers.FRONTEND_WALLPAPER_PREFS
import jr.brian.home.ui.theme.managers.WallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType
import jr.brian.jarnfe.wallpaper.JarnWallpaperScreen
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class AppJarnWallpaperSourceTest {
    private lateinit var context: Context
    private lateinit var frontend: WallpaperManager
    private lateinit var launcher: WallpaperManager
    private lateinit var look: JarnLookStore
    private lateinit var source: AppJarnWallpaperSource

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        frontend = WallpaperManager(context, FRONTEND_WALLPAPER_PREFS)
        launcher = WallpaperManager(context)
        look = JarnLookStore(context)
        source = AppJarnWallpaperSource(context, Provider { frontend }, Provider { launcher }, Provider { look })
    }

    private fun picked(name: String): Uri {
        val file = File(context.cacheDir, name)
        file.writeBytes(GIF_HEADER)
        return Uri.fromFile(file)
    }

    private fun fileOf(uri: String?) = File(requireNotNull(uri).removePrefix("file://"))

    @Test
    fun `a restored path whose file is gone falls back to the theme background`() {
        val missing = File(context.filesDir, "jarn_wallpapers/jarn_wallpaper_1.png")
        frontend.setWallpaper("file://${missing.absolutePath}", WallpaperType.IMAGE)

        assertNull(source.currentUri)
        assertNull(source.usableOrNull("file://${missing.absolutePath}"))
    }

    @Test
    fun `a wallpaper whose file exists is shown`() {
        val file = File(context.filesDir, "jarn_wallpapers/jarn_wallpaper_2.png")
        file.parentFile?.mkdirs()
        file.writeBytes(byteArrayOf(1))
        val uri = "file://${file.absolutePath}"
        frontend.setWallpaper(uri, WallpaperType.IMAGE)

        assertEquals(uri, source.currentUri)
    }

    @Test
    fun `turning sharing off gives the bottom screen its own copy`() = runTest {
        assertTrue(source.applyPicked(picked("top.gif")))
        val top = source.currentUri

        source.setSameOnBothScreens(false)

        val bottom = look.bottomWallpaperUri
        assertFalse(look.sameWallpaperOnBoth)
        assertNotNull(bottom)
        assertNotEquals(top, bottom)
        assertTrue(fileOf(bottom).exists())
        assertTrue(fileOf(top).exists())
    }

    @Test
    fun `replacing one screen never deletes the other screen's file`() = runTest {
        source.applyPicked(picked("top.gif"))
        source.setSameOnBothScreens(false)
        val oldTop = source.currentUri
        val oldBottom = look.bottomWallpaperUri

        source.applyPicked(picked("bottom.gif"), JarnWallpaperScreen.Bottom)

        assertFalse(fileOf(oldBottom).exists())
        assertTrue(fileOf(oldTop).exists())
        assertTrue(fileOf(look.bottomWallpaperUri).exists())
    }

    @Test
    fun `the theme background on one screen leaves the other alone`() = runTest {
        source.applyPicked(picked("top.gif"))
        source.setSameOnBothScreens(false)
        val top = source.currentUri
        val bottom = look.bottomWallpaperUri

        source.useThemeBackground(JarnWallpaperScreen.Bottom)

        assertNull(look.bottomWallpaperUri)
        assertFalse(fileOf(bottom).exists())
        assertEquals(top, source.currentUri)
        assertTrue(fileOf(top).exists())
    }

    @Test
    fun `sharing again shows the top on both and removes the bottom file`() = runTest {
        source.applyPicked(picked("top.gif"))
        source.setSameOnBothScreens(false)
        source.applyPicked(picked("bottom.gif"), JarnWallpaperScreen.Bottom)
        val bottom = look.bottomWallpaperUri

        source.setSameOnBothScreens(true)

        assertTrue(look.sameWallpaperOnBoth)
        assertNull(look.bottomWallpaperUri)
        assertFalse(fileOf(bottom).exists())
        assertTrue(fileOf(source.currentUri).exists())
    }

    @Test
    fun `a file both screens point at is kept until neither does`() = runTest {
        source.applyPicked(picked("top.gif"))
        val shared = requireNotNull(source.currentUri)
        look.setBottomWallpaperUri(shared)

        source.useThemeBackground(JarnWallpaperScreen.Top)

        assertTrue(fileOf(shared).exists())
    }

    @Test
    fun `turning sharing off with no picked wallpaper leaves the bottom on the theme`() = runTest {
        source.setSameOnBothScreens(false)

        assertFalse(look.sameWallpaperOnBoth)
        assertNull(look.bottomWallpaperUri)
    }

    @Test
    fun `a bottom file left behind by a restore is removed on the next start`() {
        val dir = File(context.filesDir, "jarn_wallpapers").apply { mkdirs() }
        val orphan = File(dir, "jarn_wallpaper_bottom_1.gif").apply { writeBytes(GIF_HEADER) }
        val kept = File(dir, "jarn_wallpaper_bottom_2.gif").apply { writeBytes(GIF_HEADER) }
        val top = File(dir, "jarn_wallpaper_3.gif").apply { writeBytes(GIF_HEADER) }
        frontend.setWallpaper("file://${top.absolutePath}", WallpaperType.GIF)
        look.setSameWallpaperOnBoth(false)
        look.setBottomWallpaperUri("file://${kept.absolutePath}")

        AppJarnWallpaperSource(context, Provider { frontend }, Provider { launcher }, Provider { look })

        assertFalse(orphan.exists())
        assertTrue(kept.exists())
        assertTrue(top.exists())
    }

    @Test
    fun `a bottom file the top screen points at survives the sweep`() {
        val dir = File(context.filesDir, "jarn_wallpapers").apply { mkdirs() }
        val shared = File(dir, "jarn_wallpaper_bottom_4.gif").apply { writeBytes(GIF_HEADER) }
        frontend.setWallpaper("file://${shared.absolutePath}", WallpaperType.GIF)

        source.sweepOrphanedBottomFiles()

        assertTrue(shared.exists())
    }

    private companion object {
        val GIF_HEADER = "GIF89a".toByteArray()
    }
}
