package jr.brian.home.ui.meetjarn

import jr.brian.home.data.preview.JarnPreviewGate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeetJarnGateTest {
    private class FakeGate(var due: Boolean) : JarnPreviewGate {
        val shown = mutableListOf<String>()
        var tried = false

        override fun shouldShow(versionName: String) = due && versionName !in shown

        override fun recordShown(versionName: String) {
            shown += versionName
        }

        override fun markTried() {
            tried = true
        }
    }

    private val content = MeetJarnContent(library = MeetJarnLibrary.EsDeArt)

    @Test
    fun `a due launch opens meet jarn and counts one showing`() = runTest {
        val gate = FakeGate(due = true)
        val state = MeetJarnState()
        assertTrue(state.openIfDue("3.2.0", gate, offered = true) { content })
        assertEquals(content, state.content)
        assertEquals(MeetJarnSource.Launch, state.source)
        assertEquals(listOf("3.2.0"), gate.shown)
    }

    @Test
    fun `never twice in one version`() = runTest {
        val gate = FakeGate(due = true)
        val state = MeetJarnState()
        state.openIfDue("3.2.0", gate, offered = true) { content }
        state.close(declined = true)
        assertFalse(state.openIfDue("3.2.0", gate, offered = true) { content })
        assertFalse(state.isOpen)
    }

    @Test
    fun `nothing opens when the gate says no or the build has no jarn`() = runTest {
        val state = MeetJarnState()
        assertFalse(state.openIfDue("3.2.0", FakeGate(due = false), offered = true) { content })
        assertFalse(state.openIfDue("3.2.0", FakeGate(due = true), offered = false) { content })
        assertFalse(state.isOpen)
    }

    @Test
    fun `not now at launch leaves the jarn card out of what's new, settings never does`() = runTest {
        val launch = MeetJarnState()
        launch.openIfDue("3.2.0", FakeGate(due = true), offered = true) { content }
        launch.close(declined = true)
        assertTrue(launch.declinedAtLaunch)

        val settings = MeetJarnState()
        settings.open(content, MeetJarnSource.Settings)
        settings.close(declined = true)
        assertFalse(settings.declinedAtLaunch)
    }
}
