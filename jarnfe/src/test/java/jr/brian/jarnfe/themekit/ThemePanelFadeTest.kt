package jr.brian.jarnfe.themekit

import androidx.compose.ui.graphics.Color
import jr.brian.core.model.theme.ThemeEdge
import jr.brian.core.model.theme.ThemeFade
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.jarnfe.library.BigShelfGames
import jr.brian.jarnfe.library.BigShelfSystems
import org.junit.Assert.assertEquals
import org.junit.Test

/** #578: a panel edge fades from its color to transparent. */
class ThemePanelFadeTest {
    private val color = Color(0xFF102030)
    private val clear = color.copy(alpha = 0f)

    @Test
    fun `a top or start fade is clear at that edge and solid past its length`() {
        assertEquals(listOf(0f to clear, 0.3f to color, 1f to color), themeFadeStops(color, ThemeEdge.Top, 0.3f).toList())
        assertEquals(listOf(0f to clear, 0.5f to color, 1f to color), themeFadeStops(color, ThemeEdge.Start, 0.5f).toList())
    }

    @Test
    fun `a bottom or end fade mirrors it`() {
        assertEquals(listOf(0f to color, 0.75f to color, 1f to clear), themeFadeStops(color, ThemeEdge.Bottom, 0.25f).toList())
        assertEquals(listOf(0f to color, 0.75f to color, 1f to clear), themeFadeStops(color, ThemeEdge.End, 0.25f).toList())
    }

    @Test
    fun `a length out of range is clamped, and a broken one takes the default`() {
        assertEquals(listOf(0f to clear, 1f to color, 1f to color), themeFadeStops(color, ThemeEdge.Top, 4f).toList())
        val default = ThemeFade.DEFAULT_LENGTH
        assertEquals(listOf(0f to clear, default to color, 1f to color), themeFadeStops(color, ThemeEdge.Top, Float.NaN).toList())
    }

    @Test
    fun `Big shelf plates fade up into the art`() {
        listOf(BigShelfSystems, BigShelfGames).forEach { view ->
            val plate = view.layers.single { it.id == "plate" } as ThemeLayer.Panel
            assertEquals(ThemeEdge.Top, plate.fade?.edge)
        }
    }
}
