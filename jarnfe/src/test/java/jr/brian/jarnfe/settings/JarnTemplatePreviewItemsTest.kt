package jr.brian.jarnfe.settings

import jr.brian.core.model.theme.ThemeView
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.library.JarnLibrarySystem
import jr.brian.jarnfe.themekit.FocusedItem
import org.junit.Assert.assertEquals
import org.junit.Test

class JarnTemplatePreviewItemsTest {
    private val snes = JarnLibrarySystem(id = "snes", title = "Super Nintendo", gameCount = 1, favoriteCount = 0)
    private val game = JarnLibraryGame(id = "g1", systemId = "snes", title = "Chrono Trigger")

    @Test
    fun `the preview focuses the first item with its system`() {
        val items = JarnTemplatePreviewItems(listOf(snes), listOf(game))
        assertEquals(FocusedItem.System(snes), items.focused(ThemeView.Systems))
        assertEquals(FocusedItem.Game(game, snes), items.focused(ThemeView.Games))
    }

    @Test
    fun `an empty library focuses nothing`() {
        val items = JarnTemplatePreviewItems(emptyList(), emptyList())
        assertEquals(FocusedItem.None, items.focused(ThemeView.Systems))
        assertEquals(FocusedItem.None, items.focused(ThemeView.Games))
    }
}
