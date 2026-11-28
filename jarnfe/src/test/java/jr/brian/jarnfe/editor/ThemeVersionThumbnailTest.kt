package jr.brian.jarnfe.editor

import android.app.Application
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.ui.theme.JarnLooks
import jr.brian.core.ui.theme.resolvedColors
import jr.brian.jarnfe.library.LibraryBuiltInThemes
import jr.brian.jarnfe.library.withSystemsBelowHeader
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val WIDTH = 320
private const val HEIGHT = 180

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThemeVersionThumbnailTest {
    @Test
    fun `a grid copy's thumbnail shows its tiles, not an empty box`() {
        val colors = JarnLooks.Default.resolvedColors
        val spec = LibraryBuiltInThemes.Grid.spec.withSystemsBelowHeader()
        val image = ImageBitmap(WIDTH, HEIGHT)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(WIDTH.toFloat(), HEIGHT.toFloat())) {
            drawThemeThumbnail(spec.views[ThemeView.Systems], colors)
        }
        val pixels = image.toPixelMap()
        val seen = buildSet { for (x in 0 until WIDTH) for (y in 0 until HEIGHT) add(pixels[x, y].toArgb()) }
        assertTrue("the focused tile is drawn", colors.accent.toArgb() in seen)
        assertTrue("the other tiles are drawn", colors.raised.toArgb() in seen)
        assertTrue("the header band stays background", pixels[WIDTH / 2, HEIGHT / 20].toArgb() == colors.bg.toArgb())
    }
}
