package jr.brian.jarnfe.themes

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import jr.brian.frontend.look.JarnBrowseView
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.frontend.look.JarnLookStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnThemeChoiceStoreTest {
    private val store get() = JarnLookStore(ApplicationProvider.getApplicationContext<Application>())
    private val installed = listOf(TemplateCarousel, OwnColorsTheme)

    @Test
    fun `an existing player's stored views land on the same theme`() {
        store.updateDisplay { it.copy(systemView = JarnBrowseView.Grid, gameView = JarnBrowseView.Carousel) }
        val reloaded = store
        assertNull(reloaded.activeThemeId)
        assertEquals(
            JarnThemeChoice.BuiltIn(JarnBrowseView.Grid, JarnBrowseView.Carousel),
            resolveJarnThemeChoice(reloaded.activeThemeId, reloaded.display, installed),
        )
    }

    @Test
    fun `picking a built-in clears the theme and sets both views`() {
        val first = store
        first.setActiveThemeId(TemplateCarousel.id)
        first.applyThemeChoice(JarnThemeChoice.BuiltIn(JarnBrowseView.Carousel, JarnBrowseView.Carousel))
        val reloaded = store
        assertNull(reloaded.activeThemeId)
        assertEquals(JarnBrowseView.Carousel, reloaded.display.systemView)
        assertEquals(JarnBrowseView.Carousel, reloaded.display.gameView)
    }

    @Test
    fun `picking a template keeps the stored views and Command Panel matching`() {
        val first = store
        first.updateDisplay { it.copy(systemView = JarnBrowseView.Grid, gameView = JarnBrowseView.Carousel) }
        first.setMatchCommandPanel(true)
        first.setMatchCommandPanelFonts(true)
        first.applyThemeChoice(JarnThemeChoice.Installed(TemplateCarousel))
        val reloaded = store
        assertEquals(TemplateCarousel.id, reloaded.activeThemeId)
        assertEquals(JarnBrowseView.Carousel, reloaded.display.gameView)
        assertTrue(reloaded.matchCommandPanel)
        assertTrue(reloaded.matchCommandPanelFonts)
    }

    @Test
    fun `a theme with its own colors turns off both Command Panel matches`() {
        val first = store
        first.setMatchCommandPanel(true)
        first.setMatchCommandPanelFonts(true)
        first.applyThemeChoice(JarnThemeChoice.Installed(OwnColorsTheme))
        val reloaded = store
        assertEquals(OwnColorsTheme.id, reloaded.activeThemeId)
        assertFalse(reloaded.matchCommandPanel)
        assertFalse(reloaded.matchCommandPanelFonts)
    }

    @Test
    fun `the picker shows a mix card only while the views differ`() {
        val same = JarnDisplayPrefs()
        val sameOptions = jarnThemeOptions(resolveJarnThemeChoice(null, same, installed), same, installed)
        assertEquals(listOf("builtin-grid", "builtin-carousel", TemplateCarousel.id, OwnColorsTheme.id), sameOptions.map { it.key })
        assertEquals(listOf("builtin-grid"), sameOptions.filter { it.chosen }.map { it.key })

        val mixed = JarnDisplayPrefs(systemView = JarnBrowseView.Carousel, gameView = JarnBrowseView.Grid)
        val mixedOptions = jarnThemeOptions(resolveJarnThemeChoice(null, mixed, installed), mixed, installed)
        assertEquals(listOf("builtin-mixed"), mixedOptions.filter { it.chosen }.map { it.key })
    }

    @Test
    fun `an installed theme is the only chosen card`() {
        val mixed = JarnDisplayPrefs(systemView = JarnBrowseView.Carousel, gameView = JarnBrowseView.Grid)
        val options = jarnThemeOptions(JarnThemeChoice.Installed(TemplateCarousel), mixed, installed)
        assertEquals(listOf(TemplateCarousel.id), options.filter { it.chosen }.map { it.key })
        assertTrue(options.any { it.key == "builtin-mixed" })
    }
}
