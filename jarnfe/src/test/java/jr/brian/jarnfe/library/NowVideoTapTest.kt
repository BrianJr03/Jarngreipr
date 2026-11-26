package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.ProvideJarnInput
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val SCENE_TAG = "scene"

/**
 * The Deck's layering: the scene's parent holds the tap, the scrolling Now page with its controls
 * sits over the video, and the hidden-UI touch catcher sits over everything.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NowVideoTapTest {
    @get:Rule
    val rule = createComposeRule()

    private var plays = 0
    private var favorite = false

    @Before
    fun setUp() = NowVideoVisibility.report(false)

    @After
    fun tearDown() = NowVideoVisibility.report(false)

    private fun showDeck(previewPlaying: Boolean = true) {
        rule.setContent {
            JarnTheme {
                Box(Modifier.size(538.dp, 420.dp)) {
                    Box(Modifier.matchParentSize().testTag(SCENE_TAG).nowVideoTapToFill()) {
                        Column(Modifier.matchParentSize().verticalScroll(rememberScrollState())) {
                            Spacer(Modifier.height(300.dp))
                            JarnButton(label = "Play", onClick = { plays++ }, modifier = Modifier.fillMaxWidth())
                            JarnToggleRow(label = "Favorite", checked = favorite, onCheckedChange = { favorite = it })
                            Spacer(Modifier.height(600.dp))
                        }
                    }
                    NowHiddenUiTouch()
                }
            }
        }
        rule.runOnIdle { NowVideoVisibility.report(previewPlaying) }
        rule.waitForIdle()
    }

    private fun tapVideo() {
        rule.onNodeWithTag(SCENE_TAG).performTouchInput { click(Offset(centerX, 80.dp.toPx())) }
        rule.waitForIdle()
    }

    @Test
    fun `a tap on the playing video hides the UI`() {
        showDeck()
        tapVideo()
        assertTrue(NowVideoVisibility.uiHidden)
    }

    @Test
    fun `the tap that hides the UI does not bring it straight back`() {
        showDeck()
        tapVideo()
        rule.waitForIdle()
        assertTrue(NowVideoVisibility.uiHidden)
        rule.onNodeWithTag(NOW_HIDDEN_UI_TOUCH_TAG).assertExists()
    }

    @Test
    fun `a tap while the UI is hidden brings it back`() {
        showDeck()
        tapVideo()
        rule.onNodeWithTag(NOW_HIDDEN_UI_TOUCH_TAG).performTouchInput { click(center) }
        rule.waitForIdle()
        assertFalse(NowVideoVisibility.uiHidden)
    }

    @Test
    fun `Play and Favorite still take their own taps`() {
        showDeck()
        rule.onNodeWithText("Play").performTouchInput { click() }
        rule.waitForIdle()
        rule.onNodeWithText("Favorite").performTouchInput { click() }
        rule.waitForIdle()
        assertEquals(1, plays)
        assertTrue(favorite)
        assertFalse(NowVideoVisibility.uiHidden)
    }

    @Test
    fun `scrolling the page never hides the UI`() {
        showDeck()
        rule.onNodeWithTag(SCENE_TAG).performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertFalse(NowVideoVisibility.uiHidden)
    }

    @Test
    fun `the real Now scene takes the tap on its backdrop and leaves its content's own taps alone`() {
        rule.setContent {
            JarnTheme {
                ProvideJarnInput(JarnInputRouter(), JarnScreen.Deck) {
                    Box(Modifier.size(538.dp, 420.dp).testTag(SCENE_TAG)) {
                        NowScene(LibrarySelection.None, systemNow = null) {
                            Column(Modifier.matchParentSize().verticalScroll(rememberScrollState())) {
                                Spacer(Modifier.height(300.dp))
                                JarnButton(label = "Play", onClick = { plays++ }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
        rule.runOnIdle { NowVideoVisibility.report(true) }
        rule.onNodeWithText("Play").performTouchInput { click() }
        rule.waitForIdle()
        assertEquals(1, plays)
        assertFalse(NowVideoVisibility.uiHidden)
        tapVideo()
        assertTrue(NowVideoVisibility.uiHidden)
    }

    @Test
    fun `with no preview playing a tap does nothing`() {
        showDeck(previewPlaying = false)
        tapVideo()
        assertFalse(NowVideoVisibility.uiHidden)
    }
}
