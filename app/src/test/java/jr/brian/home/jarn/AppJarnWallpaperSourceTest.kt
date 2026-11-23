package jr.brian.home.jarn

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import jr.brian.home.ui.theme.managers.FRONTEND_WALLPAPER_PREFS
import jr.brian.home.ui.theme.managers.WallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    private lateinit var source: AppJarnWallpaperSource

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        frontend = WallpaperManager(context, FRONTEND_WALLPAPER_PREFS)
        val launcher = WallpaperManager(context)
        source = AppJarnWallpaperSource(context, Provider { frontend }, Provider { launcher })
    }

    @Test
    fun `a restored path whose file is gone falls back to the theme background`() {
        val missing = File(context.filesDir, "jarn_wallpapers/jarn_wallpaper_1.png")
        frontend.setWallpaper("file://${missing.absolutePath}", WallpaperType.IMAGE)

        assertNull(source.currentUri)
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
}
