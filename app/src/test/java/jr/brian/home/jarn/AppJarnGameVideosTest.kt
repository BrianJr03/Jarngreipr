package jr.brian.home.jarn

import androidx.test.core.app.ApplicationProvider
import jr.brian.jarnfe.library.JarnGameVideoState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class AppJarnGameVideosTest {
    @Test
    fun `a plain path keeps characters a uri would cut off`() {
        val uri = gameVideoUri("/sdcard/ES-DE/downloaded_media/snes/videos/Sonic #1 (USA)?.MP4")!!
        assertEquals("file", uri.scheme)
        assertEquals("/sdcard/ES-DE/downloaded_media/snes/videos/Sonic #1 (USA)?.MP4", uri.path)
    }

    @Test
    fun `a content uri is used as is`() {
        val path = "content://com.android.externalstorage.documents/tree/primary%3AES-DE/doc/x.mp4"
        assertEquals(path, gameVideoUri(path).toString())
    }

    @Test
    fun `a blank path has no uri`() {
        assertNull(gameVideoUri(""))
        assertNull(gameVideoUri("  "))
    }

    @Test
    fun `a blank path fails at once and release is safe twice`() {
        val video = AppJarnGameVideos().create(ApplicationProvider.getApplicationContext(), "", true)
        assertEquals(JarnGameVideoState.Failed, video.state.value)
        video.setMuted(false)
        video.release()
        video.release()
        video.play()
    }
}
