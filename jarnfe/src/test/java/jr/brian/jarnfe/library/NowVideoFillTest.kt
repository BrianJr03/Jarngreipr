package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.launch.GameActionsPage
import jr.brian.jarnfe.launch.JarnGameActions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NowVideoFillTest {
    @get:Rule
    val rule = createComposeRule()

    private val videos = FakeJarnGameVideos()

    @Before
    fun setUp() {
        NowVideoFailures.clear()
        NowVideoVisibility.report(false)
        JarnGameActions.page = GameActionsPage.Actions
    }

    @After
    fun tearDown() = NowVideoVisibility.report(false)

    /** A Deck-like frame: a header above the scene and a dock below it. */
    private fun showDeck() {
        rule.setContent {
            JarnTheme {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth().height(HEADER.dp))
                    Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = SIDE.dp)) {
                        NowVideoFill {
                            NowGameVideo("g", CLIP, canPlay = true, videos = videos, muted = true)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(DOCK.dp))
                }
            }
        }
        rule.mainClock.advanceTimeBy(NOW_VIDEO_START_DELAY_MILLIS + SLACK_MILLIS)
        rule.runOnUiThread { videos.created.single().state.value = JarnGameVideoState.Ready }
        rule.mainClock.advanceTimeBy(SLACK_MILLIS)
    }

    private fun videoBounds(): DpRect = rule.onNodeWithTag(NOW_VIDEO_TAG).getUnclippedBoundsInRoot()

    private fun rootBounds(): DpRect = rule.onRoot().getUnclippedBoundsInRoot()

    private fun setHidden(hidden: Boolean) {
        rule.runOnUiThread { if (NowVideoVisibility.uiHidden != hidden) NowVideoVisibility.toggleUi() }
        rule.mainClock.advanceTimeBy(SETTLE_MILLIS)
    }

    private fun assertBounds(expected: DpRect, actual: DpRect) {
        assertEquals(expected.left.value, actual.left.value, TOLERANCE)
        assertEquals(expected.top.value, actual.top.value, TOLERANCE)
        assertEquals(expected.right.value, actual.right.value, TOLERANCE)
        assertEquals(expected.bottom.value, actual.bottom.value, TOLERANCE)
    }

    @Test
    fun `hiding the UI grows the preview to fill the window`() {
        showDeck()
        setHidden(true)
        assertBounds(rootBounds(), videoBounds())
    }

    @Test
    fun `bringing the UI back returns the preview to its slot`() {
        showDeck()
        val slot = videoBounds()
        assertEquals(HEADER.toFloat(), slot.top.value, TOLERANCE)
        assertEquals(SIDE.toFloat(), slot.left.value, TOLERANCE)
        setHidden(true)
        setHidden(false)
        assertBounds(slot, videoBounds())
    }

    @Test
    fun `the same player plays through hiding and showing the UI`() {
        showDeck()
        setHidden(true)
        setHidden(false)
        setHidden(true)
        assertEquals(1, videos.created.size)
        assertEquals(1, videos.live.size)
    }

    private companion object {
        const val CLIP = "/sdcard/ES-DE/downloaded_media/snes/videos/a.mp4"
        const val HEADER = 48
        const val DOCK = 64
        const val SIDE = 12
        const val SLACK_MILLIS = 100L
        const val SETTLE_MILLIS = 600L
        const val TOLERANCE = 1f
    }
}
