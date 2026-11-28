package jr.brian.jarnfe.themes

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import jr.brian.core.model.theme.ThemeView
import jr.brian.frontend.look.JarnBrowseView
import jr.brian.frontend.look.JarnLookStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnThemeChoiceStoreTest {
    private val store get() = JarnLookStore(ApplicationProvider.getApplicationContext<Application>())

    private fun JarnLookStore.choice() = resolveJarnThemeChoice(activeThemeId, display, TestThemes)

    @Test
    fun `an old mix survives the first change`() {
        val first = store
        first.updateDisplay { it.copy(systemView = JarnBrowseView.Grid, gameView = JarnBrowseView.Carousel) }
        first.applyThemeChoice(first.choice().withOverride(ThemeView.Systems, JarnBrowseView.Carousel))
        val reloaded = store
        assertEquals(JarnThemeRegistry.Grid.id, reloaded.activeThemeId)
        assertEquals(JarnBrowseView.Carousel, reloaded.display.systemView)
        assertEquals(JarnBrowseView.Carousel, reloaded.display.gameView)
    }

    @Test
    fun `picking a layout keeps the overrides`() {
        val first = store
        first.updateDisplay { it.copy(systemView = JarnBrowseView.Grid, gameView = JarnBrowseView.Carousel) }
        first.applyThemeChoice(first.choice().copy(layout = TemplateCarousel))
        val reloaded = store
        assertEquals(TemplateCarousel.id, reloaded.activeThemeId)
        assertEquals(JarnBrowseView.SameAsLayout, reloaded.display.systemView)
        assertEquals(JarnBrowseView.Carousel, reloaded.display.gameView)
        assertEquals(JarnThemeChoice(TemplateCarousel, games = JarnBrowseView.Carousel), reloaded.choice())
    }

    @Test
    fun `setting an override back to same as layout clears it`() {
        val first = store
        first.applyThemeChoice(JarnThemeChoice(TemplateCarousel, games = JarnBrowseView.Grid))
        first.applyThemeChoice(first.choice().withOverride(ThemeView.Games, null))
        assertEquals(JarnThemeChoice(TemplateCarousel), store.choice())
    }

    @Test
    fun `a template leaves Command Panel matching alone`() {
        val first = store
        first.setMatchCommandPanel(true)
        first.setMatchCommandPanelFonts(true)
        first.applyThemeChoice(JarnThemeChoice(TemplateCarousel))
        assertTrue(store.matchCommandPanel)
        assertTrue(store.matchCommandPanelFonts)
    }

    @Test
    fun `a theme with its own colors turns off both Command Panel matches`() {
        val first = store
        first.setMatchCommandPanel(true)
        first.setMatchCommandPanelFonts(true)
        first.applyThemeChoice(JarnThemeChoice(OwnColorsTheme))
        val reloaded = store
        assertEquals(OwnColorsTheme.id, reloaded.activeThemeId)
        assertFalse(reloaded.matchCommandPanel)
        assertFalse(reloaded.matchCommandPanelFonts)
    }

    @Test
    fun `the picker marks only the saved layout`() {
        val saved = JarnThemeChoice(JarnThemeRegistry.Carousel, games = JarnBrowseView.Grid)
        val options = jarnThemeOptions(saved, TestThemes)
        assertEquals(TestThemes.map { it.id }, options.map { it.key })
        assertEquals(listOf(JarnThemeRegistry.Carousel.id), options.filter { it.chosen }.map { it.key })
        assertTrue(options.all { it.choice.games == JarnBrowseView.Grid })
    }
}
