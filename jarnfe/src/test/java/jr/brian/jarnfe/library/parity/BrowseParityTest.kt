package jr.brian.jarnfe.library.parity

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import jr.brian.frontend.look.JarnBrowseView
import jr.brian.jarnfe.library.LibraryGameList
import jr.brian.jarnfe.library.LibraryHome
import jr.brian.jarnfe.session.LibrarySection
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Today's Grid and Carousel browse for systems and games, held to a checked-in golden of node
 * bounds and D-pad focus order. Moving browse onto the theme engine must keep every case green.
 * Legacy graphics keeps text measurement the same on every host, so the golden holds on CI too.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@Config(sdk = [34], application = BrowseParityApplication::class, qualifiers = "w1280dp-h720dp")
class BrowseParityTest {
    @get:Rule
    val rule = createComposeRule()

    private val app: BrowseParityApplication get() = ApplicationProvider.getApplicationContext()

    private fun systems(view: JarnBrowseView, golden: String) {
        app.useViews(systems = view, games = JarnBrowseView.Grid)
        val library = BrowseParityLibrary.systemsStore()
        rule.assertBrowseParity(golden) {
            LibraryHome(section = LibrarySection.Systems, library = library, onOpenSystem = {}, emptyActions = {})
        }
    }

    private fun games(view: JarnBrowseView, golden: String) {
        app.useViews(systems = JarnBrowseView.Grid, games = view)
        val library = BrowseParityLibrary.gamesStore()
        rule.assertBrowseParity(golden) { LibraryGameList(library) }
    }

    @Test
    fun `systems grid looks and focuses as today`() = systems(JarnBrowseView.Grid, "browse-systems-grid")

    @Test
    fun `systems carousel looks and focuses as today`() = systems(JarnBrowseView.Carousel, "browse-systems-carousel")

    @Test
    fun `games grid looks and focuses as today`() = games(JarnBrowseView.Grid, "browse-games-grid")

    @Test
    fun `games carousel looks and focuses as today`() = games(JarnBrowseView.Carousel, "browse-games-carousel")
}
