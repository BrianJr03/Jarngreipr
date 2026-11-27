package jr.brian.jarnfe.themekit

import jr.brian.core.model.theme.ThemeRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThemePlacementTest {

    @Test
    fun `full rect fills a 16 by 9 screen`() {
        assertEquals(ThemePxRect(0, 0, 1920, 1080), placeThemeRect(ThemeRect(0f, 0f, 1f, 1f), 1920, 1080))
    }

    @Test
    fun `same rect scales to the 4 by 3 bottom screen`() {
        val rect = ThemeRect(x = 0.25f, y = 0.5f, width = 0.5f, height = 0.25f)
        assertEquals(ThemePxRect(480, 540, 960, 270), placeThemeRect(rect, 1920, 1080))
        assertEquals(ThemePxRect(256, 384, 512, 192), placeThemeRect(rect, 1024, 768))
    }

    @Test
    fun `centered origin puts the rect middle at the point`() {
        val rect = ThemeRect(x = 0.5f, y = 0.5f, width = 0.4f, height = 0.2f, originX = 0.5f, originY = 0.5f)
        assertEquals(ThemePxRect(576, 432, 768, 216), placeThemeRect(rect, 1920, 1080))
    }

    @Test
    fun `bottom right origin anchors the rect corner`() {
        val rect = ThemeRect(x = 1f, y = 1f, width = 0.1f, height = 0.1f, originX = 1f, originY = 1f)
        assertEquals(ThemePxRect(1728, 972, 192, 108), placeThemeRect(rect, 1920, 1080))
    }

    @Test
    fun `touching rects stay touching at odd sizes`() {
        val left = placeThemeRect(ThemeRect(0f, 0f, 1f / 3, 1f), 1001, 333)!!
        val middle = placeThemeRect(ThemeRect(1f / 3, 0f, 1f / 3, 1f), 1001, 333)!!
        val right = placeThemeRect(ThemeRect(2f / 3, 0f, 1f / 3, 1f), 1001, 333)!!
        assertEquals(left.right, middle.left)
        assertEquals(middle.right, right.left)
        assertEquals(1001, right.right)
    }

    @Test
    fun `invalid rects place nothing`() {
        assertNull(placeThemeRect(ThemeRect(0f, 0f, 0f, 0.5f), 1920, 1080))
        assertNull(placeThemeRect(ThemeRect(0f, 0f, 0.5f, -1f), 1920, 1080))
        assertNull(placeThemeRect(ThemeRect(Float.NaN, 0f, 0.5f, 0.5f), 1920, 1080))
        assertNull(placeThemeRect(ThemeRect(0f, 0f, 0.0001f, 0.5f), 100, 100))
    }

    @Test
    fun `off screen rects keep their position`() {
        assertEquals(ThemePxRect(-192, 0, 384, 108), placeThemeRect(ThemeRect(-0.1f, 0f, 0.2f, 0.1f), 1920, 1080))
    }

    @Test
    fun `clampInto shifts then shrinks`() {
        val area = ThemePxRect(0, 0, 100, 100)
        assertEquals(ThemePxRect(80, 0, 20, 20), ThemePxRect(90, -5, 20, 20).clampInto(area))
        assertEquals(ThemePxRect(0, 10, 100, 20), ThemePxRect(-20, 10, 300, 20).clampInto(area))
        assertNull(ThemePxRect(-20, 10, 300, 20).moveInto(area))
    }

    @Test
    fun `safe area subtracts insets`() {
        val screen = ThemeScreen(1920, 1080, ThemeInsets(top = 24, bottom = 40))
        assertEquals(ThemePxRect(0, 24, 1920, 1016), screen.safeArea)
    }

    @Test
    fun `oversized insets leave an empty safe area`() {
        val screen = ThemeScreen(100, 100, ThemeInsets(top = 80, bottom = 80))
        assertEquals(0, screen.safeArea.height)
    }
}
