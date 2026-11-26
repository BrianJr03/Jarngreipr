package jr.brian.frontend.look

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnStickMutePrefsTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `muting with L3 and R3 starts off and on survives a restart`() {
        assertFalse(JarnLookStore(context).display.stickMute)

        JarnLookStore(context).updateDisplay { it.copy(stickMute = true) }

        assertTrue(JarnLookStore(context).display.stickMute)
    }
}
