package jr.brian.jarnfe.shell

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.isOnTopScreen
import jr.brian.jarnfe.settings.JarnWallpaperPausedNoteContent
import jr.brian.jarnfe.wallpaper.JarnWallpaperPause
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnBatterySaverIndicatorTest {
    @get:Rule
    val rule = createComposeRule()

    private fun string(id: Int) = ApplicationProvider.getApplicationContext<Application>().getString(id)

    private val status = JarnDeviceStatus(time = "9:41", batteryPercent = 80)

    private fun show(content: @Composable () -> Unit) {
        rule.setContent { JarnTheme { content() } }
        rule.waitForIdle()
    }

    @Test
    fun `the icon needs Battery Saver, the setting and the top screen`() {
        assertTrue(showsBatterySaverIcon(batterySaverOn = true, pausesOnBatterySaver = true, onTopScreen = true))
        assertFalse(showsBatterySaverIcon(batterySaverOn = false, pausesOnBatterySaver = true, onTopScreen = true))
        assertFalse(showsBatterySaverIcon(batterySaverOn = true, pausesOnBatterySaver = false, onTopScreen = true))
        assertFalse(showsBatterySaverIcon(batterySaverOn = true, pausesOnBatterySaver = true, onTopScreen = false))
    }

    @Test
    fun `the icon reads out for TalkBack next to the clock`() {
        show { JarnStatusReadoutContent(status, showsBatterySaver = true) }
        rule.onNodeWithContentDescription(string(R.string.jarnfe_status_battery_saver)).assertExists()
        rule.onNodeWithText("9:41").assertExists()
    }

    @Test
    fun `no icon when it should not show`() {
        show { JarnStatusReadoutContent(status, showsBatterySaver = false) }
        rule.onNodeWithContentDescription(string(R.string.jarnfe_status_battery_saver)).assertDoesNotExist()
    }

    /** Stage or Deck, whichever role is drawn, the window on the default display is the top one. */
    @Test
    fun `a window on the default display counts as the top screen`() {
        val top = mutableStateOf<Boolean?>(null)
        show { top.value = isOnTopScreen() }
        assertTrue(top.value == true)
    }

    @Test
    fun `a Stage or Deck on the top screen shows the clock and battery`() {
        show { JarnTopScreenStatus(onTopScreen = true) }
        rule.onNodeWithTag(JARN_STATUS_READOUT_TAG).assertExists()
    }

    @Test
    fun `a swapped Stage on the bottom screen shows no clock or battery`() {
        show { JarnTopScreenStatus(onTopScreen = false) }
        rule.onNodeWithTag(JARN_STATUS_READOUT_TAG).assertDoesNotExist()
    }

    @Test
    fun `the paused note shows only when a pause applies`() {
        show { JarnWallpaperPausedNoteContent(JarnWallpaperPause.BatterySaver) }
        rule.onNodeWithText(string(R.string.jarnfe_wallpaper_paused_battery_saver)).assertExists()
    }

    @Test
    fun `no note when Battery Saver is not pausing`() {
        show { JarnWallpaperPausedNoteContent(null) }
        rule.onNodeWithText(string(R.string.jarnfe_wallpaper_paused_battery_saver)).assertDoesNotExist()
    }
}
