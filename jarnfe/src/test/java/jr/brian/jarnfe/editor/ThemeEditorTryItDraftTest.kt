package jr.brian.jarnfe.editor

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import jr.brian.core.model.theme.ThemeView
import jr.brian.frontend.look.JarnBrowseView
import jr.brian.frontend.look.JarnLookStore
import jr.brian.jarnfe.library.LibraryBuiltInTheme
import jr.brian.jarnfe.library.LibraryBuiltInThemes
import jr.brian.jarnfe.library.withSystemsBelowHeader
import jr.brian.jarnfe.themes.JarnThemeChoice
import jr.brian.jarnfe.themes.JarnThemeEntry
import jr.brian.jarnfe.themes.JarnThemeKind
import jr.brian.jarnfe.themes.JarnThemePreview
import jr.brian.jarnfe.themes.JarnThemeRegistry
import jr.brian.jarnfe.themes.activeTheme
import jr.brian.jarnfe.themes.applyThemeChoice
import jr.brian.jarnfe.themes.resolveJarnThemeChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A Grid copy being edited, while Carousel is the player's layout. */
private val Draft = JarnThemeEntry(
    id = "grid-copy",
    kind = JarnThemeKind.User,
    theme = LibraryBuiltInTheme(LibraryBuiltInThemes.Grid.spec.withSystemsBelowHeader()),
)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ThemeEditorTryItDraftTest {
    private val store get() = JarnLookStore(ApplicationProvider.getApplicationContext<Application>())

    @Test
    fun `try it draws the draft, not the active layout`() {
        val active = JarnThemeChoice(JarnThemeRegistry.Carousel)
        val theme = tryItChoice(Draft, active).activeTheme()
        assertSame(Draft, theme.forView(ThemeView.Systems))
        assertSame(Draft, theme.forView(ThemeView.Games))
    }

    @Test
    fun `the player's own Systems and Games choices still apply`() {
        val active = JarnThemeChoice(JarnThemeRegistry.Carousel, games = JarnBrowseView.Grid)
        val theme = tryItChoice(Draft, active).activeTheme()
        assertSame(Draft, theme.forView(ThemeView.Systems))
        assertSame(JarnThemeRegistry.Grid, theme.forView(ThemeView.Games))
    }

    @Test
    fun `the canvas leaving never clears the preview Try it just set`() {
        val canvas = Any()
        val tryIt = Any()
        JarnThemePreview.show(canvas, JarnThemeChoice(Draft))
        JarnThemePreview.show(tryIt, tryItChoice(Draft, JarnThemeChoice(JarnThemeRegistry.Carousel)))
        JarnThemePreview.clear(canvas)
        assertSame(Draft, JarnThemePreview.choice?.layout)
        JarnThemePreview.clear(tryIt)
        assertNull(JarnThemePreview.choice)
    }

    @Test
    fun `use this layout keeps the overrides it was tried with`() {
        val first = store
        first.applyThemeChoice(JarnThemeChoice(JarnThemeRegistry.Carousel, games = JarnBrowseView.Grid))
        val saved = resolveJarnThemeChoice(first.activeThemeId, first.display)
        first.applyThemeChoice(tryItChoice(Draft, saved))
        val reloaded = store
        assertEquals(Draft.id, reloaded.activeThemeId)
        assertEquals(JarnBrowseView.SameAsLayout, reloaded.display.systemView)
        assertEquals(JarnBrowseView.Grid, reloaded.display.gameView)
    }
}
