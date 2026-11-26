package jr.brian.jarnfe.settings

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import jr.brian.core.ui.theme.JarnSwitchCue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Settings and the first-run picker both list [JarnSwitchCue.entries] with these labels. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnSwitchCueOptionsTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `three cues are offered, each with its own label and hint`() {
        val labels = JarnSwitchCue.entries.map { context.getString(it.cueLabelRes) }
        assertEquals(listOf("Pulse", "Screen dim", "Tab mark"), labels)
        assertEquals(3, JarnSwitchCue.entries.map { context.getString(it.cueHintRes) }.distinct().size)
    }

    @Test
    fun `A in settings cycles through the three cues`() {
        assertEquals(JarnSwitchCue.Dim, JarnSwitchCue.Pulse.nextCue())
        assertEquals(JarnSwitchCue.TabMark, JarnSwitchCue.Dim.nextCue())
        assertEquals(JarnSwitchCue.Pulse, JarnSwitchCue.TabMark.nextCue())
    }
}
