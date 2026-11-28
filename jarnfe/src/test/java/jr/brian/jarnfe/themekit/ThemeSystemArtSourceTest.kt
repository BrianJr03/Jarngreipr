package jr.brian.jarnfe.themekit

import jr.brian.core.model.theme.ThemeSystemArt
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.library.JarnLibrarySystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** #581: a theme's own per-system art replaces Jarn's whole, for the systems it covers. */
class ThemeSystemArtSourceTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val snes = JarnLibrarySystem(
        id = "snes",
        title = "Super Nintendo",
        gameCount = 3,
        artPath = "/media/snes.png",
        logoPath = "/media/snes-logo.png",
        nowBackgroundPath = "/media/snes-now.png",
    )

    @Test
    fun `a covered system shows only the theme's art`() {
        val dir = temp.newFolder("theme")
        val source = ThemeSystemArtSource(mapOf("snes" to ThemeSystemArt(logo = "esde/logos/snes.svg")), dir)
        val themed = source.apply(snes)
        assertEquals(dir.canonicalFile.resolve("esde/logos/snes.svg").path, themed.logoPath)
        assertEquals(null, themed.artPath)
        assertEquals(null, themed.nowBackgroundPath)
    }

    @Test
    fun `a background becomes the card art and the Now backdrop`() {
        val dir = temp.newFolder("theme")
        val source = ThemeSystemArtSource(mapOf("snes" to ThemeSystemArt(background = "esde/backgrounds/snes.jpg")), dir)
        val path = dir.canonicalFile.resolve("esde/backgrounds/snes.jpg").path
        val themed = source.apply(snes)
        assertEquals(path, themed.artPath)
        assertEquals(path, themed.nowBackgroundPath)
        assertEquals(null, themed.logoPath)
    }

    @Test
    fun `other systems, escaping paths and code themes keep Jarn's art`() {
        val dir = temp.newFolder("theme")
        val nes = snes.copy(id = "nes")
        assertSame(nes, ThemeSystemArtSource(mapOf("snes" to ThemeSystemArt(logo = "a.svg")), dir).apply(nes))
        assertSame(snes, ThemeSystemArtSource(mapOf("snes" to ThemeSystemArt(logo = "../x.svg")), dir).apply(snes))
        assertSame(snes, ThemeSystemArtSource(mapOf("snes" to ThemeSystemArt(logo = "a.svg")), null).apply(snes))
        assertSame(snes, ThemeSystemArtSource.None.apply(snes))
    }

    @Test
    fun `a focused game reads its system through the theme`() {
        val dir = temp.newFolder("theme")
        val source = ThemeSystemArtSource(mapOf("snes" to ThemeSystemArt(logo = "l.svg")), dir)
        val game = JarnLibraryGame(id = "g", systemId = "snes", title = "Game")
        val focus = source.apply(FocusedItem.Game(game, snes)) as FocusedItem.Game
        assertEquals(dir.canonicalFile.resolve("l.svg").path, focus.system?.logoPath)
        assertEquals(FocusedItem.None, source.apply(FocusedItem.None))
    }
}
