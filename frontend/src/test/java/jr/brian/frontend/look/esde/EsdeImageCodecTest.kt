package jr.brian.frontend.look.esde

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Base64
import kotlin.math.abs

/** Two 40x20 frames: red (200, 30, 30), then blue. */
private const val TWO_FRAME_GIF =
    "R0lGODlhKAAUAIEAAMgeHgAAAAAAAAAAACH/C05FVFNDQVBFMi4wAwEAAAAh+QQACgAAACwAAAAAKAAUAAAIMAABCBxIsKDBgwgTKlzIsKHDhxA" +
        "jSpxIsaLFixgzatzIsaPHjyBDihxJsqTJkxQDAgAh+QQACgAAACwAAAAAKAAUAIEeHsgAAAAAAAAAAAAIMAABCBxIsKDBgwgTKlzIsKHDhxAjSpx" +
        "IsaLFixgzatzIsaPHjyBDihxJsqTJkxQDAgA7"

/** The real Android decoders behind ES-DE imports: shrinking art and sampling a background tone. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EsdeImageCodecTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun png(width: Int, height: Int, draw: Canvas.() -> Unit): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).draw()
        return temp.newFile("art-${width}x$height.png").also { file ->
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun decode(file: File): Bitmap = requireNotNull(BitmapFactory.decodeFile(file.path))

    @Test
    fun `a large PNG is shrunk to fit and keeps its shape`() {
        val source = png(400, 200) { drawColor(android.graphics.Color.GREEN) }
        val target = temp.root.resolve("logo.png")
        assertTrue(AndroidEsdeImageScaler.scale(source, target, maxWidth = 100, maxHeight = 100, keepAlpha = true))
        val out = decode(target)
        assertEquals(100, out.width)
        assertEquals(50, out.height)
    }

    @Test
    fun `a small picture is never blown up, and a background is written as JPEG`() {
        val source = png(40, 30) { drawColor(android.graphics.Color.BLUE) }
        val target = temp.root.resolve("bg.jpg")
        assertTrue(AndroidEsdeImageScaler.scale(source, target, maxWidth = 1920, maxHeight = 1080, keepAlpha = false))
        val header = target.readBytes().take(2).map { it.toInt() and 0xFF }
        assertEquals(listOf(0xFF, 0xD8), header)
        assertEquals(40, decode(target).width)
    }

    @Test
    fun `an animated GIF keeps its first frame`() {
        val source = temp.newFile("anim.gif").also { it.writeBytes(Base64.getDecoder().decode(TWO_FRAME_GIF)) }
        val target = temp.root.resolve("anim.png")
        assertTrue(AndroidEsdeImageScaler.scale(source, target, maxWidth = 20, maxHeight = 20, keepAlpha = true))
        val out = decode(target)
        assertEquals(20, out.width)
        val pixel = out.getPixel(5, 5)
        assertTrue("first frame is red, got ${Integer.toHexString(pixel)}", android.graphics.Color.red(pixel) > 150 && android.graphics.Color.blue(pixel) < 80)
    }

    @Test
    fun `an unreadable file is refused`() {
        val junk = temp.newFile("junk.png").also { it.writeText("not an image") }
        assertTrue(!AndroidEsdeImageScaler.scale(junk, temp.root.resolve("x.png"), 10, 10, true))
        assertNull(AndroidEsdeColorSampler.pixels(junk))
    }

    @Test
    fun `the sampled background is the picture's main dark tone`() {
        val source = png(640, 360) {
            drawColor(android.graphics.Color.rgb(16, 20, 40))
            drawRect(400f, 0f, 640f, 360f, Paint().apply { color = android.graphics.Color.rgb(250, 240, 200) })
        }
        val pixels = requireNotNull(AndroidEsdeColorSampler.pixels(source))
        assertEquals(64 * 36, pixels.size)
        val tone = requireNotNull(dominantDarkColor(pixels))
        assertClose(Color(16, 20, 40), tone)
    }

    @Test
    fun `a bright picture gives its main tone darkened`() {
        val bright = IntArray(100) { Color(240, 200, 120).toArgb() }
        val tone = requireNotNull(dominantDarkColor(bright))
        assertTrue(tone.red < 0.2f && tone.green < 0.2f)
        assertNull(dominantDarkColor(IntArray(10)))
    }

    private fun assertClose(expected: Color, actual: Color) {
        val channels = listOf(expected.red - actual.red, expected.green - actual.green, expected.blue - actual.blue)
        assertTrue("expected $expected, got $actual", channels.all { abs(it) < 0.03f })
    }
}
