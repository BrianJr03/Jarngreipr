package jr.brian.jarnfe.wallpaper

import android.app.Application
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnKeyInput
import jr.brian.jarnfe.input.JarnKeyPhase
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.ProvideJarnInput
import jr.brian.jarnfe.settings.JarnLiveWallpaperPicker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnLiveWallpaperPickerTest {
    @get:Rule
    val rule = createComposeRule()

    private val router = JarnInputRouter()
    private val wallpaper = JarnLiveWallpaper("test", R.string.jarnfe_wallpaper_title) {}
    private var saved by mutableStateOf<String?>(null)

    private fun show(catalog: List<JarnLiveWallpaper>) {
        rule.setContent {
            JarnTheme {
                ProvideJarnInput(router, JarnScreen.Stage) {
                    JarnLiveWallpaperPicker(saved, { saved = it }, catalog)
                }
            }
        }
    }

    private fun row() = rule.onNodeWithText(LIVE_LABEL)

    private fun option(label: String) =
        rule.onNode(hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))

    @Test
    fun `an empty catalog shows no row`() {
        show(emptyList())
        row().assertDoesNotExist()
    }

    @Test
    fun `choosing a wallpaper saves it and returns focus to the row`() {
        show(listOf(wallpaper))
        row().performClick()
        rule.waitForIdle()
        option(WALLPAPER_NAME).performClick()
        rule.waitForIdle()
        assertEquals("test", saved)
        option(OFF_LABEL).assertDoesNotExist()
        row().assertIsFocused()
    }

    @Test
    fun `choosing off clears the saved wallpaper`() {
        saved = "test"
        show(listOf(wallpaper))
        row().performClick()
        rule.waitForIdle()
        option(OFF_LABEL).performClick()
        rule.waitForIdle()
        assertNull(saved)
    }

    @Test
    fun `back closes the list without changing the choice`() {
        saved = "test"
        show(listOf(wallpaper))
        row().performClick()
        rule.waitForIdle()
        option(OFF_LABEL).assertExists()
        rule.runOnIdle {
            router.dispatch(JarnKeyInput(KeyEvent.KEYCODE_BUTTON_B, JarnKeyPhase.Down, 0))
            router.dispatch(JarnKeyInput(KeyEvent.KEYCODE_BUTTON_B, JarnKeyPhase.Up, 10))
        }
        rule.waitForIdle()
        option(OFF_LABEL).assertDoesNotExist()
        assertEquals("test", saved)
    }

    @Test
    fun `opening focuses the saved choice`() {
        saved = "test"
        show(listOf(wallpaper))
        row().performClick()
        rule.waitForIdle()
        option(WALLPAPER_NAME).assertIsFocused()
    }

    private companion object {
        const val LIVE_LABEL = "Live wallpaper"
        const val OFF_LABEL = "Off"
        const val WALLPAPER_NAME = "Wallpaper"
    }
}
