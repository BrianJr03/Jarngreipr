package jr.brian.frontend.look

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnCoverGlowPrefsTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `the cover glow starts off and full screen survives a restart`() {
        assertEquals(JarnCoverGlow.Off, JarnLookStore(context).display.coverGlow)

        JarnLookStore(context).updateDisplay { it.copy(coverGlow = JarnCoverGlow.Full) }

        assertEquals(JarnCoverGlow.Full, JarnLookStore(context).display.coverGlow)
    }

    @Test
    fun `an unknown stored id reads as off`() {
        context.getSharedPreferences("jarnfe_look_prefs", 0).edit().putString("cover_glow", "rainbow").commit()

        assertEquals(JarnCoverGlow.Off, JarnLookStore(context).display.coverGlow)
    }
}
