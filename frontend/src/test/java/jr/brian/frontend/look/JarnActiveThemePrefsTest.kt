package jr.brian.frontend.look

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnActiveThemePrefsTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `the built-in theme is the default`() {
        assertNull(JarnLookStore(context).activeThemeId)
    }

    @Test
    fun `the chosen theme survives a restart and can go back to built-in`() {
        JarnLookStore(context).setActiveThemeId("art-book")
        val reloaded = JarnLookStore(context)
        assertEquals("art-book", reloaded.activeThemeId)
        assertEquals("art-book", reloaded.activeThemeIdState.value)

        reloaded.setActiveThemeId(null)

        assertNull(JarnLookStore(context).activeThemeId)
    }

    @Test
    fun `a blank id means the built-in theme`() {
        JarnLookStore(context).setActiveThemeId(" ")

        assertNull(JarnLookStore(context).activeThemeId)
    }
}
