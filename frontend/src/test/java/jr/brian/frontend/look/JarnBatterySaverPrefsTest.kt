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
class JarnBatterySaverPrefsTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `pausing on Battery Saver starts on and off survives a restart`() {
        assertTrue(JarnLookStore(context).display.pauseOnBatterySaver)

        JarnLookStore(context).updateDisplay { it.copy(pauseOnBatterySaver = false) }

        assertFalse(JarnLookStore(context).display.pauseOnBatterySaver)
    }
}
