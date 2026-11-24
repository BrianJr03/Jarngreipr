package jr.brian.jarnfe.search

import android.app.Activity
import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.apps.FakeJarnAppOptions
import jr.brian.jarnfe.apps.JarnAppOptionsData
import jr.brian.jarnfe.apps.JarnAppsData
import jr.brian.jarnfe.apps.JarnAppsDataProvider
import jr.brian.jarnfe.apps.JarnAppsSnapshot
import jr.brian.jarnfe.dock.JarnDockApp
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.ProvideJarnInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A on an app result launches it through the same apps data the drawer uses, and a long press opens its options. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = JarnSearchAppGridTest.HostApp::class)
class JarnSearchAppGridTest {
    @get:Rule
    val rule = createComposeRule()

    class HostApp : Application(), JarnAppsDataProvider {
        override val jarnAppsData: JarnAppsData = LaunchingApps
    }

    private object LaunchingApps : JarnAppsData, JarnAppOptionsData by FakeJarnAppOptions() {
        val launched = mutableListOf<String>()

        override val snapshot: Flow<JarnAppsSnapshot> = flowOf(JarnAppsSnapshot(emptyList(), emptyList(), emptySet()))

        override fun launchApp(host: Activity, packageName: String) {
            launched += packageName
        }
    }

    @Test
    fun clickingAnAppResultLaunchesIt() {
        LaunchingApps.launched.clear()
        val apps = listOf(JarnDockApp("a.music", "Music"), JarnDockApp("a.maps", "Maps"))
        rule.setContent {
            JarnTheme {
                ProvideJarnInput(JarnInputRouter(), JarnScreen.Stage) {
                    Box(Modifier.size(320.dp, 320.dp)) { SearchAppGrid(apps, JarnSearchStore()) }
                }
            }
        }
        rule.onNodeWithTag("a.maps").performClick()
        rule.waitForIdle()
        assertEquals(listOf("a.maps"), LaunchingApps.launched)
    }

    @Test
    fun longPressingAnAppResultOpensItsOptionsWithoutLaunching() {
        LaunchingApps.launched.clear()
        val apps = listOf(JarnDockApp("a.music", "Music"), JarnDockApp("a.maps", "Maps"))
        rule.setContent {
            JarnTheme {
                ProvideJarnInput(JarnInputRouter(), JarnScreen.Stage) {
                    Box(Modifier.size(320.dp, 320.dp)) { SearchAppGrid(apps, JarnSearchStore()) }
                }
            }
        }
        rule.onNodeWithTag("a.music").performTouchInput { longClick() }
        rule.waitForIdle()
        val context = ApplicationProvider.getApplicationContext<Application>()
        rule.onNodeWithText(context.getString(R.string.jarnfe_canvas_action_app_info)).assertIsDisplayed()
        rule.onNodeWithText(context.getString(R.string.jarnfe_app_icon_title)).assertIsDisplayed()
        rule.onNodeWithText(context.getString(R.string.jarnfe_folder_rename_app)).assertIsDisplayed()
        assertTrue(LaunchingApps.launched.isEmpty())
    }
}
