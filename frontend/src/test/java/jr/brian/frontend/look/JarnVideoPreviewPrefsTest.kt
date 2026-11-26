package jr.brian.frontend.look

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnVideoPreviewPrefsTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `previews, their sound and the hint all start off`() {
        val store = JarnLookStore(context)

        assertFalse(JarnDisplayPrefs().videoPreviews)
        assertFalse(JarnDisplayPrefs().videoPreviewSound)
        assertFalse(store.display.videoPreviews)
        assertFalse(store.display.videoPreviewSound)
        assertFalse(store.videoHintShown)
    }

    @Test
    fun `both choices survive a restart`() {
        JarnLookStore(context).updateDisplay { it.copy(videoPreviews = true, videoPreviewSound = true) }

        val reloaded = JarnLookStore(context).display

        assertTrue(reloaded.videoPreviews)
        assertTrue(reloaded.videoPreviewSound)
    }

    @Test
    fun `fit is the default and crop survives a restart`() {
        assertEquals(JarnVideoFit.Fit, JarnLookStore(context).display.videoFit)

        JarnLookStore(context).updateDisplay { it.copy(videoFit = JarnVideoFit.Crop) }

        assertEquals(JarnVideoFit.Crop, JarnLookStore(context).display.videoFit)
    }

    @Test
    fun `the hint stays shown after a restart`() {
        JarnLookStore(context).markVideoHintShown()

        assertTrue(JarnLookStore(context).videoHintShown)
    }
}
