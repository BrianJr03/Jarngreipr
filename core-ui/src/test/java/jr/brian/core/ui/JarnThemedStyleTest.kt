package jr.brian.core.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import jr.brian.core.ui.theme.jarnStyleFor
import org.junit.Assert.assertEquals
import org.junit.Test

class JarnThemedStyleTest {
    private val themed = TextStyle(fontFamily = FontFamily.Cursive)
    private val base = TextStyle(fontFamily = FontFamily.SansSerif)
    private val latin: (Int) -> Boolean = { it in 'A'.code..'Z'.code || it in 'a'.code..'z'.code || it in '0'.code..'9'.code }

    @Test
    fun `fully covered text keeps the theme`() {
        assertEquals(themed, jarnStyleFor("Continue playing", themed, base, latin))
        assertEquals(themed, jarnStyleFor("Zelda: Breath of the Wild (USA)", themed, base, latin))
    }

    @Test
    fun `one missing glyph puts the whole text on base`() {
        assertEquals(base, jarnStyleFor("Pokémon", themed, base, latin))
    }

    @Test
    fun `emoji and CJK fall back to base`() {
        assertEquals(base, jarnStyleFor("Fun 🎮", themed, base, latin))
        assertEquals(base, jarnStyleFor("ゼルダの伝説", themed, base, latin))
    }
}
