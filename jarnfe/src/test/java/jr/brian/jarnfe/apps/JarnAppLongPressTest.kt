package jr.brian.jarnfe.apps

import android.app.Activity
import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.dock.JarnDockApp
import jr.brian.jarnfe.dock.JarnDockData
import jr.brian.jarnfe.dock.JarnDockDataProvider
import jr.brian.jarnfe.dock.JarnDockRow
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.ProvideJarnInput
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Hosts the dock's data the way the real Application does. */
class JarnLongPressTestApp : Application(), JarnDockDataProvider, JarnAppsDataProvider {
    val launched = mutableListOf<String>()
    private val apps = listOf(JarnDockApp("pkg.maps", "Maps"), JarnDockApp("pkg.mail", "Mail"))

    override val jarnDockData = object : JarnDockData {
        override val apps = flowOf(this@JarnLongPressTestApp.apps)
        override fun launch(host: Activity, packageName: String) {
            launched += packageName
        }
    }

    override val jarnAppsData: JarnAppsData = object : JarnAppsData, JarnAppOptionsData by FakeJarnAppOptions() {
        override val snapshot = flowOf(JarnAppsSnapshot(apps, emptyList(), emptySet()))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = JarnLongPressTestApp::class)
class JarnAppLongPressTest {
    @get:Rule
    val rule = createComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<JarnLongPressTestApp>()

    private fun string(id: Int) = app.getString(id)

    @Test
    fun `a long press on a drawer app opens its options, and a tap still opens the app`() {
        val opened = mutableListOf<String>()
        val apps = listOf(JarnDockApp("pkg.maps", "Maps"), JarnDockApp("pkg.mail", "Mail"))
        rule.setContent {
            JarnTheme {
                ProvideJarnInput(JarnInputRouter(), JarnScreen.Deck) {
                    Box(Modifier.size(540.dp, 460.dp)) {
                        JarnAppsDrawer(
                            JarnAppsSnapshot(apps, emptyList(), emptySet()),
                            JarnAppsCommands({ opened += it }, twoScreenOptions()),
                        )
                    }
                }
            }
        }
        rule.onNodeWithTag("pkg.mail").performClick()
        assertEquals(listOf("pkg.mail"), opened)
        rule.onNodeWithTag("pkg.maps").performTouchInput { longClick() }
        rule.waitForIdle()
        assertMenuShows()
        rule.onNodeWithText(string(R.string.jarnfe_folder_launch_on)).assertIsDisplayed()
        assertEquals(listOf("pkg.mail"), opened)
    }

    @Test
    fun `a long press on a dock app opens its options, and a tap still launches it`() {
        rule.setContent {
            JarnTheme {
                ProvideJarnInput(JarnInputRouter(), JarnScreen.Deck) {
                    Box(Modifier.size(540.dp, 460.dp)) { JarnDockRow() }
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Mail").performTouchInput { longClick() }
        rule.waitForIdle()
        assertMenuShows()
        assertTrue(app.launched.isEmpty())
    }

    private fun twoScreenOptions() = FakeJarnAppOptions().apply { current = current.copy(hasSecondDisplay = true) }

    private fun assertMenuShows() {
        rule.onNodeWithText(string(R.string.jarnfe_canvas_action_app_info)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.jarnfe_app_icon_title)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.jarnfe_folder_rename_app)).assertIsDisplayed()
    }
}
