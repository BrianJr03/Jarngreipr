package jr.brian.jarnfe.library

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnGameVideoTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `no host player means no video`() {
        var found: JarnGameVideos? = FakeJarnGameVideos()
        rule.setContent { found = rememberJarnGameVideos() }
        rule.waitForIdle()
        assertNull(found)
        assertNull(ApplicationProvider.getApplicationContext<Application>().jarnGameVideosOrNull())
    }

    @Test
    fun `a provided player wins over the host`() {
        val fake = FakeJarnGameVideos()
        var found: JarnGameVideos? = null
        rule.setContent {
            CompositionLocalProvider(LocalJarnGameVideos provides fake) {
                found = rememberJarnGameVideos()
            }
        }
        rule.waitForIdle()
        assertSame(fake, found)
    }

    @Test
    fun `a screen's video is made once and released when the screen leaves`() {
        val fake = FakeJarnGameVideos()
        var path by mutableStateOf<String?>("/media/snes/videos/Mario.MP4")
        rule.setContent {
            CompositionLocalProvider(LocalJarnGameVideos provides fake) {
                path?.let { PreviewOwner(it) }
            }
        }
        rule.waitForIdle()
        val video = fake.created.single()
        assertEquals("/media/snes/videos/Mario.MP4", video.path)
        assertTrue(video.isMuted)
        assertEquals(JarnGameVideoState.Loading, video.state.value)

        path = null
        rule.waitForIdle()

        assertTrue(video.released)
        assertTrue(fake.live.isEmpty())
    }
}

@Composable
private fun PreviewOwner(path: String) {
    val videos = rememberJarnGameVideos() ?: return
    val context = LocalContext.current
    val video = remember(videos, path) { videos.create(context, path, crop = true) }
    DisposableEffect(video) { onDispose(video::release) }
}
