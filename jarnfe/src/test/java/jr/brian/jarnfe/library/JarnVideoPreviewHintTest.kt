package jr.brian.jarnfe.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnVideoPreviewHintTest {
    private fun hint(
        hasVideo: Boolean = true,
        previewsOn: Boolean = false,
        canPlay: Boolean = true,
        alreadyShown: Boolean = false,
    ) = shouldShowVideoHint(hasVideo, previewsOn, canPlay, alreadyShown)

    @Test
    fun `shows for a game with a video while previews are off`() {
        assertTrue(hint())
    }

    @Test
    fun `stays quiet once it was shown`() {
        assertFalse(hint(alreadyShown = true))
    }

    @Test
    fun `stays quiet when previews are already on`() {
        assertFalse(hint(previewsOn = true))
    }

    @Test
    fun `stays quiet for a game without a video`() {
        assertFalse(hint(hasVideo = false))
    }

    @Test
    fun `stays quiet when nothing could play the video`() {
        assertFalse(hint(canPlay = false))
    }
}
