package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.audio.JarnAudio
import jr.brian.jarnfe.audio.JarnAudioGate
import jr.brian.jarnfe.launch.GameActionsPage
import jr.brian.jarnfe.launch.JarnGameActions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NowVideoAudioTest {
    @get:Rule
    val rule = createComposeRule()

    private val videos = FakeJarnGameVideos()
    private val claimed: String? get() = JarnAudio.gateState.value.previewGameId

    @Before
    fun setUp() {
        NowVideoFailures.clear()
        JarnGameActions.page = GameActionsPage.Actions
        JarnAudio.resetForTest(JarnAudioGate(topWindows = setOf(WINDOW)))
        rule.mainClock.autoAdvance = false
    }

    @After
    fun tearDown() {
        NowVideoFailures.clear()
        JarnAudio.resetForTest()
    }

    private fun showVideo(
        path: () -> String?,
        gameId: () -> String = { "g" },
        muted: Boolean = false,
        shown: () -> Boolean = { true },
        canPlay: () -> Boolean = { true },
    ) {
        rule.setContent {
            JarnTheme {
                CompositionLocalProvider(LocalNowVideoHold provides NowVideoHold()) {
                    Box(Modifier.size(538.dp, 380.dp)) {
                        if (shown()) NowGameVideo(gameId(), path(), canPlay(), videos, muted)
                    }
                }
            }
        }
        rule.mainClock.advanceTimeByFrame()
    }

    private fun change(block: () -> Unit) {
        rule.runOnUiThread {
            block()
            Snapshot.sendApplyNotifications()
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
    }

    private fun waitForStart() {
        rule.mainClock.advanceTimeBy(NOW_VIDEO_START_DELAY_MILLIS + FRAME_SLACK_MILLIS)
    }

    private fun startReady(): FakeJarnGameVideo {
        waitForStart()
        val video = videos.live.single()
        change { video.state.value = JarnGameVideoState.Ready }
        return video
    }

    @Test
    fun `a preview with sound claims audio before it starts and is heard once ready`() {
        showVideo(path = { CLIP_A })
        assertEquals("g", claimed)
        assertFalse(JarnAudio.gateState.value.allowed)
        waitForStart()
        assertTrue(videos.live.single().isMuted)
        val video = startReady()
        assertFalse(video.isMuted)
    }

    @Test
    fun `a muted preview never claims audio`() {
        showVideo(path = { CLIP_A }, muted = true)
        val video = startReady()
        assertTrue(video.isMuted)
        assertNull(claimed)
        assertTrue(JarnAudio.gateState.value.allowed)
    }

    @Test
    fun `other media keeps the preview muted`() {
        JarnAudio.resetForTest(JarnAudioGate(topWindows = setOf(WINDOW), otherMediaPlaying = true))
        showVideo(path = { CLIP_A })
        val video = startReady()
        assertTrue(video.isMuted)
        change { JarnAudio.resetForTest(JarnAudioGate(topWindows = setOf(WINDOW), previewGameId = "g")) }
        assertFalse(video.isMuted)
        change {
            JarnAudio.resetForTest(
                JarnAudioGate(topWindows = setOf(WINDOW), otherMediaPlaying = true, previewGameId = "g"),
            )
        }
        assertTrue(video.isMuted)
    }

    @Test
    fun `a failed preview lets go of audio`() {
        showVideo(path = { CLIP_A })
        val video = startReady()
        change { video.state.value = JarnGameVideoState.Failed }
        assertNull(claimed)
        assertTrue(JarnAudio.gateState.value.allowed)
    }

    @Test
    fun `changing game mutes the leaving preview and moves the claim`() {
        var id by mutableStateOf("a")
        var path by mutableStateOf<String?>(CLIP_A)
        showVideo(path = { path }, gameId = { id })
        val first = startReady()
        change {
            id = "b"
            path = CLIP_B
        }
        assertTrue(first.isMuted)
        assertEquals("b", claimed)
        change { path = null }
        assertNull(claimed)
    }

    @Test
    fun `leaving the scene or closing the gate leaves no claim behind`() {
        var shown by mutableStateOf(true)
        var canPlay by mutableStateOf(true)
        showVideo(path = { CLIP_A }, shown = { shown }, canPlay = { canPlay })
        startReady()
        change { canPlay = false }
        assertNull(claimed)
        change { canPlay = true }
        assertEquals("g", claimed)
        change { shown = false }
        assertNull(claimed)
        assertTrue(videos.live.isEmpty())
        assertTrue(JarnAudio.gateState.value.allowed)
    }

    @Test
    fun `a covered window keeps the preview muted`() {
        JarnAudio.resetForTest(JarnAudioGate(topWindows = setOf(WINDOW), coveredWindows = setOf(WINDOW)))
        showVideo(path = { CLIP_A })
        assertTrue(startReady().isMuted)
    }

    private companion object {
        const val WINDOW = 1
        const val CLIP_A = "/sdcard/ES-DE/downloaded_media/n64/videos/a.mp4"
        const val CLIP_B = "/sdcard/ES-DE/downloaded_media/n64/videos/b.mp4"
        const val FRAME_SLACK_MILLIS = 50L
    }
}
