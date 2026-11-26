package jr.brian.jarnfe.settings

import android.app.Application
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.core.app.ApplicationProvider
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.frontend.look.JarnVideoFit
import jr.brian.jarnfe.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** D-pad walk through the group, with A on each row. Sound only changes once previews are on. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class VideoPreviewGroupTest {
    @get:Rule
    val rule = createComposeRule()

    private var display by mutableStateOf(JarnDisplayPrefs())

    private fun string(id: Int) = ApplicationProvider.getApplicationContext<Application>().getString(id)

    private fun show() {
        rule.setContent {
            JarnTheme {
                val first = remember { FocusRequester() }
                LaunchedEffect(Unit) { first.requestFocus() }
                Column(Modifier.focusRequester(first).focusGroup()) {
                    VideoPreviewGroup(display) { transform -> display = transform(display) }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun press(key: Key) {
        rule.onRoot().performKeyInput { pressKey(key) }
        rule.waitForIdle()
    }

    private val previewsRow get() = rule.onNodeWithText(string(R.string.jarnfe_video_previews_play))
    private val fitRow get() = rule.onNodeWithText(string(R.string.jarnfe_video_previews_fit))
    private val soundRow get() = rule.onNodeWithText(string(R.string.jarnfe_video_previews_sound))

    @Test
    fun `sound is dimmed but still reachable while previews are off`() {
        show()
        soundRow.assertIsNotEnabled()

        press(Key.DirectionDown)
        press(Key.DirectionDown)
        press(Key.Enter)

        assertFalse(display.videoPreviewSound)
    }

    @Test
    fun `turning previews on lets sound change`() {
        show()
        press(Key.DirectionDown)
        press(Key.Enter)
        assertTrue(display.videoPreviews)
        soundRow.assertIsEnabled()

        press(Key.DirectionDown)
        press(Key.Enter)

        assertTrue(display.videoPreviewSound)
    }

    @Test
    fun `with previews off the sound subtitle says to turn them on first`() {
        show()

        rule.onNodeWithText(string(R.string.jarnfe_video_previews_sound_hint_off)).assertExists()
        rule.onNodeWithText(string(R.string.jarnfe_video_previews_sound_hint)).assertDoesNotExist()
    }

    @Test
    fun `with previews on the sound subtitle only describes the sound`() {
        display = JarnDisplayPrefs(videoPreviews = true)
        show()

        rule.onNodeWithText(string(R.string.jarnfe_video_previews_sound_hint)).assertExists()
        rule.onNodeWithText(string(R.string.jarnfe_video_previews_sound_hint_off)).assertDoesNotExist()
    }

    @Test
    fun `video fit is dimmed and A leaves it alone while previews are off`() {
        show()
        fitRow.assertIsNotEnabled()

        repeat(3) { press(Key.DirectionDown) }
        press(Key.Enter)

        assertEquals(JarnVideoFit.Fit, display.videoFit)
    }

    @Test
    fun `with previews on A cycles the fit`() {
        display = JarnDisplayPrefs(videoPreviews = true)
        show()
        fitRow.assertIsEnabled()
        rule.onNodeWithText(string(R.string.jarnfe_video_previews_fit_hint)).assertExists()

        repeat(3) { press(Key.DirectionDown) }
        press(Key.Enter)
        assertEquals(JarnVideoFit.Crop, display.videoFit)
        rule.onNodeWithText(string(R.string.jarnfe_video_previews_crop_hint)).assertExists()

        press(Key.Enter)
        assertEquals(JarnVideoFit.Fit, display.videoFit)
    }

    @Test
    fun `up returns to the previews row`() {
        show()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        press(Key.DirectionUp)

        press(Key.Enter)

        assertTrue(display.videoPreviews)
    }
}
