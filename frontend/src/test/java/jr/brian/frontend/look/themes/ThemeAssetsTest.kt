package jr.brian.frontend.look.themes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ThemeAssetsTest {
    private fun bytes(size: Int, order: ByteOrder = ByteOrder.LITTLE_ENDIAN, fill: ByteBuffer.() -> Unit) =
        ByteBuffer.allocate(size).order(order).apply(fill).array()

    @Test
    fun `reads sizes from every image header without decoding`() {
        assertEquals(ImageSize(1920, 1080), readImageSize(pngHeader(1920, 1080)))

        val gif = bytes(13) { put("GIF89a".toByteArray()); putShort(320); putShort(200) }
        assertEquals(ImageSize(320, 200), readImageSize(gif))

        val jpeg = bytes(32, ByteOrder.BIG_ENDIAN) {
            put(0xFF.toByte()); put(0xD8.toByte())
            put(0xFF.toByte()); put(0xE0.toByte()); putShort(4); putShort(0)
            put(0xFF.toByte()); put(0xC0.toByte()); putShort(17); put(8); putShort(480); putShort(640)
        }
        assertEquals(ImageSize(640, 480), readImageSize(jpeg))

        val vp8x = bytes(30) {
            put("RIFF".toByteArray()); putInt(0); put("WEBPVP8X".toByteArray()); putInt(10); putInt(0)
            put(byteArrayOf(0x7F, 0x07, 0)); put(byteArrayOf(0x37, 0x04, 0))
        }
        assertEquals(ImageSize(1920, 1080), readImageSize(vp8x))
    }

    @Test
    fun `unknown or cut off headers are unreadable`() {
        assertNull(readImageSize(ByteArray(0)))
        assertNull(readImageSize("not an image".toByteArray()))
        assertNull(readImageSize(pngHeader(1, 1).copyOf(18)))
    }

    @Test
    fun `pixel caps refuse huge or empty images`() {
        assertTrue(isImageWithinCaps(ImageSize(4096, 4096)))
        assertFalse(isImageWithinCaps(ImageSize(8193, 10)))
        assertFalse(isImageWithinCaps(ImageSize(8192, 8192)))
        assertFalse(isImageWithinCaps(ImageSize(0, 10)))
        assertFalse(isImageWithinCaps(ImageSize(-5, 10)))
        assertFalse(isImageWithinCaps(null))
    }

    @Test
    fun `fonts need a known tag and a cmap with the Latin capitals`() {
        assertTrue(isUsableFont(ttf()))
        assertFalse(isUsableFont(ttf(first = 'B'.code)))
        assertFalse(isUsableFont(ByteArray(64) { 7 }))
        assertFalse(isUsableFont(ttf().copyOf(30)))
        assertFalse(isUsableFont(ByteArray(4)))
    }

    @Test
    fun `only images and fonts are ever kept`() {
        assertEquals(ThemeAssetKind.Image, themeAssetKind("art/Bg.PNG"))
        assertEquals(ThemeAssetKind.Font, themeAssetKind("fonts/a.otf"))
        assertNull(themeAssetKind("run.sh"))
        assertNull(themeAssetKind("noext"))
    }

    @Test
    fun `safe paths stay inside the theme folder`() {
        listOf("a.png", "art/a.png", "deep/er/a.png", "dir/").forEach { assertTrue(it, isSafeThemePath(it)) }
        listOf("", "/a.png", "../a.png", "a/../../b", "./a", "a//b", "a\\b", "C:a", "a\u0000b").forEach {
            assertFalse(it, isSafeThemePath(it))
        }
    }
}
