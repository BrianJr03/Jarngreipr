package jr.brian.frontend.look

import android.app.Application
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnTabFocusPrefsTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    private val prefs get() = context.getSharedPreferences("jarnfe_look_prefs", 0)

    @Test
    fun `both is the default`() {
        assertEquals(JarnTabFocus.Both, JarnLookStore(context).display.tabFocus)
    }

    @Test
    fun `every choice survives a restart under its id`() {
        JarnTabFocus.entries.forEach { choice ->
            JarnLookStore(context).updateDisplay { it.copy(tabFocus = choice) }
            assertEquals(choice, JarnLookStore(context).display.tabFocus)
            assertEquals(choice.id, prefs.getString("tab_focus", null))
        }
    }

    @Test
    fun `an unknown stored id reads as both`() {
        prefs.edit { putString("tab_focus", "touch") }
        assertEquals(JarnTabFocus.Both, JarnLookStore(context).display.tabFocus)
    }
}
