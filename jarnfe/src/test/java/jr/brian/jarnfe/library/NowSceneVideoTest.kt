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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.frontend.look.JarnVideoFit
import jr.brian.jarnfe.launch.GameActionsPage
import jr.brian.jarnfe.launch.JarnGameActions
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NowSceneVideoTest {
    @get:Rule
    val rule = createComposeRule()

    private val videos = FakeJarnGameVideos()
    private val game = JarnLibraryGame(id = "g", systemId = "s", title = "Lantern Reach")

    @Before
    fun setUp() {
        NowVideoFailures.clear()
        JarnGameActions.page = GameActionsPage.Actions
        rule.mainClock.autoAdvance = false
    }

    @After
    fun tearDown() {
        NowVideoFailures.clear()
        JarnGameActions.page = GameActionsPage.Actions
    }

    private fun showVideo(
        path: () -> String?,
        gameId: () -> String = { "g" },
        canPlay: () -> Boolean = { true },
        hold: NowVideoHold = NowVideoHold(),
        crop: () -> Boolean = { false },
        backdrop: NowVideoBackdrop? = null,
    ) {
        rule.setContent {
            JarnTheme {
                CompositionLocalProvider(LocalNowVideoHold provides hold) {
                    Box(Modifier.size(538.dp, 380.dp)) {
                        NowGameVideo(gameId(), path(), canPlay(), videos, muted = true, crop(), backdrop)
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
    }

    private fun waitForStart() {
        rule.mainClock.advanceTimeBy(NOW_VIDEO_START_DELAY_MILLIS + FRAME_SLACK_MILLIS)
    }

    @Test
    fun `a game with no video makes no player and leaves the art alone`() {
        showVideo(path = { null })
        waitForStart()
        assertTrue(videos.created.isEmpty())
        rule.onNodeWithTag(NOW_VIDEO_TAG).assertDoesNotExist()
    }

    @Test
    fun `one muted, fitted player starts only after the selection rests`() {
        showVideo(path = { CLIP_A })
        rule.mainClock.advanceTimeBy(NOW_VIDEO_START_DELAY_MILLIS - FRAME_SLACK_MILLIS)
        assertTrue(videos.created.isEmpty())
        waitForStart()
        assertEquals(1, videos.created.size)
        val video = videos.created.single()
        assertEquals(CLIP_A, video.path)
        assertFalse(video.crop)
        assertTrue(video.isMuted)
        rule.onNodeWithTag(NOW_VIDEO_TAG).assertExists()
    }

    @Test
    fun `changing game releases the old player before the next one starts`() {
        var path by mutableStateOf(CLIP_A)
        var id by mutableStateOf("a")
        showVideo(path = { path }, gameId = { id })
        waitForStart()
        change {
            id = "b"
            path = CLIP_B
        }
        waitForStart()
        assertEquals(listOf(CLIP_A, CLIP_B), videos.created.map { it.path })
        assertTrue(videos.created.first().released)
        assertEquals(1, videos.live.size)
    }

    @Test
    fun `switching to crop recreates the player once`() {
        var crop by mutableStateOf(false)
        showVideo(path = { CLIP_A }, crop = { crop })
        waitForStart()
        change { crop = true }
        rule.mainClock.advanceTimeBy(FADE_SLACK_MILLIS)
        assertEquals(listOf(false, true), videos.created.map { it.crop })
        assertTrue(videos.created.first().released)
        assertEquals(1, videos.live.size)
    }

    @Test
    fun `a fitted preview blurs the art only once its first frame shows`() {
        val backdrop = NowVideoBackdrop()
        showVideo(path = { CLIP_A }, backdrop = backdrop)
        waitForStart()
        assertFalse(backdrop.blurred)
        change { videos.created.single().state.value = JarnGameVideoState.Ready }
        assertTrue(backdrop.blurred)
        change { videos.created.single().state.value = JarnGameVideoState.Failed }
        assertFalse(backdrop.blurred)
    }

    @Test
    fun `a cropped preview never blurs the art`() {
        val backdrop = NowVideoBackdrop()
        showVideo(path = { CLIP_A }, crop = { true }, backdrop = backdrop)
        waitForStart()
        change { videos.created.single().state.value = JarnGameVideoState.Ready }
        assertFalse(backdrop.blurred)
    }

    @Test
    fun `leaving the game clears the blur`() {
        var path by mutableStateOf<String?>(CLIP_A)
        val backdrop = NowVideoBackdrop()
        showVideo(path = { path }, backdrop = backdrop)
        waitForStart()
        change { videos.created.single().state.value = JarnGameVideoState.Ready }
        change { path = null }
        rule.mainClock.advanceTimeBy(FADE_SLACK_MILLIS)
        assertFalse(backdrop.blurred)
    }

    @Test
    fun `display prefs map to the preview`() {
        val fitted = JarnDisplayPrefs(videoPreviews = true).nowVideoPreview()
        assertEquals(NowVideoPreview(enabled = true, muted = true, crop = false), fitted)
        val cropped = JarnDisplayPrefs(videoPreviewSound = true, videoFit = JarnVideoFit.Crop).nowVideoPreview()
        assertEquals(NowVideoPreview(enabled = false, muted = false, crop = true), cropped)
    }

    @Test
    fun `scrolling past games quickly never makes a player`() {
        var path by mutableStateOf<String?>(CLIP_A)
        showVideo(path = { path })
        repeat(10) { step ->
            change { path = "/videos/$step.mp4" }
            rule.mainClock.advanceTimeBy(NOW_VIDEO_START_DELAY_MILLIS / 3)
        }
        assertTrue(videos.created.isEmpty())
    }

    @Test
    fun `a failed preview keeps the art and is not retried for the session`() {
        var path by mutableStateOf<String?>(CLIP_A)
        showVideo(path = { path })
        waitForStart()
        change { videos.created.single().state.value = JarnGameVideoState.Failed }
        rule.mainClock.advanceTimeBy(FADE_SLACK_MILLIS)
        assertTrue(videos.created.single().released)
        change { path = null }
        change { path = CLIP_A }
        waitForStart()
        assertEquals(1, videos.created.size)
    }

    @Test
    fun `a closed gate makes no player and releases a live one`() {
        var canPlay by mutableStateOf(false)
        showVideo(path = { CLIP_A }, canPlay = { canPlay })
        waitForStart()
        assertTrue(videos.created.isEmpty())
        change { canPlay = true }
        waitForStart()
        assertEquals(1, videos.live.size)
        change { canPlay = false }
        assertTrue(videos.live.isEmpty())
    }

    @Test
    fun `pressing Play releases the player at once`() {
        showVideo(path = { CLIP_A })
        waitForStart()
        change { JarnLibrary.play(game) }
        assertTrue(videos.created.single().released)
        waitForStart()
        assertEquals(1, videos.created.size)
    }

    @Test
    fun `the More sheet or game actions hold the preview`() {
        val hold = NowVideoHold().apply { sheetOpen = true }
        showVideo(path = { CLIP_A }, hold = hold)
        waitForStart()
        assertTrue(videos.created.isEmpty())
        change { hold.sheetOpen = false }
        waitForStart()
        assertEquals(1, videos.live.size)
        change { JarnGameActions.page = GameActionsPage.Emulators }
        rule.mainClock.advanceTimeBy(FADE_SLACK_MILLIS)
        assertTrue(videos.live.isEmpty())
    }

    @Test
    fun `previews off makes no player even with a host and a video`() {
        rule.setContent {
            JarnTheme {
                CompositionLocalProvider(LocalJarnGameVideos provides videos) {
                    Box(Modifier.size(538.dp, 380.dp)) {
                        NowSceneVideo(LibrarySelection.Game(game), NowVideoPreview.Off)
                    }
                }
            }
        }
        waitForStart()
        assertTrue(videos.created.isEmpty())
    }

    @Test
    fun `the Resume poster never plays a preview`() {
        rule.setContent {
            JarnTheme {
                CompositionLocalProvider(LocalJarnGameVideos provides videos) {
                    Box(Modifier.size(538.dp, 380.dp)) {
                        NowSceneVideo(LibrarySelection.Game(game, resume = true), NowVideoPreview(enabled = true))
                    }
                }
            }
        }
        waitForStart()
        assertTrue(videos.created.isEmpty())
    }

    private companion object {
        const val CLIP_A = "/sdcard/ES-DE/downloaded_media/snes/videos/a.mp4"
        const val CLIP_B = "/sdcard/ES-DE/downloaded_media/snes/videos/b.mp4"
        const val FRAME_SLACK_MILLIS = 50L
        const val FADE_SLACK_MILLIS = 200L
    }
}
