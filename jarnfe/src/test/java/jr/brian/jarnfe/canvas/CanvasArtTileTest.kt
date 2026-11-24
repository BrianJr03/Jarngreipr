package jr.brian.jarnfe.canvas

import androidx.compose.ui.unit.dp
import jr.brian.home.canvas.model.CanvasItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasArtTileTest {
    private fun tile(argb: Long?) = CanvasItem.EsdeArtItem("e", backgroundColorArgb = argb)

    @Test
    fun `the default card turns transparent as exactly zero and comes back`() {
        val hidden = tile(null).withCardTransparencyToggled()
        assertEquals(0L, hidden.backgroundColorArgb)
        assertTrue(hidden.isCardTransparent)
        assertNull(hidden.withCardTransparencyToggled().backgroundColorArgb)
    }

    @Test
    fun `every Classic preset color survives a round trip and hides fully`() {
        listOf(
            0xFF000000L, 0xFF111111L, 0xFF1E1E2EL, 0xFF0F3460L, 0xFF1F4D2BL, 0xFF3A1F4DL,
            0xFF4D1F1FL, 0xFF4D3A1FL, 0xFFE94560L, 0xFFC97B12L, 0xFF2E7D32L, 0xFF6A1B9AL,
        ).forEach { color ->
            val hidden = tile(color).withCardTransparencyToggled()
            assertTrue(hidden.isCardTransparent)
            assertEquals(0L, hidden.backgroundColorArgb!! ushr 24)
            assertEquals(color, hidden.withCardTransparencyToggled().backgroundColorArgb)
        }
    }

    @Test
    fun `black stays distinct from the default card while hidden`() {
        val black = tile(0xFF000000L).withCardTransparencyToggled()
        val default = tile(null).withCardTransparencyToggled()
        assertFalse(black.backgroundColorArgb == default.backgroundColorArgb)
    }

    @Test
    fun `opaque and unset tiles are not transparent`() {
        assertFalse(tile(null).isCardTransparent)
        assertFalse(tile(0xFF112233L).isCardTransparent)
    }

    @Test
    fun `the inset clears the focus ring on a small tile and is capped on a large one`() {
        assertEquals(6.dp, canvasArtInset(47.dp, 6.dp, 12.dp))
        assertEquals(12.dp, canvasArtInset(300.dp, 6.dp, 12.dp))
        assertEquals(9.6f, canvasArtInset(80.dp, 6.dp, 12.dp).value, 0.01f)
    }

    @Test
    fun `art corners follow the card and a square card keeps square art`() {
        assertEquals(6.dp, canvasArtCorner(12.dp, 6.dp, 4.dp))
        assertEquals(4.dp, canvasArtCorner(12.dp, 12.dp, 4.dp))
        assertEquals(0.dp, canvasArtCorner(0.dp, 6.dp, 4.dp))
    }
}
