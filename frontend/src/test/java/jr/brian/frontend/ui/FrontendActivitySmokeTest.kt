package jr.brian.frontend.ui

import android.app.Application
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 3a smoke test: FrontendActivity launches, Compose root wires
 * up, no exceptions. Nothing deep — this is the module-boundary
 * check that the Activity declaration + Compose + Hilt wiring are
 * all consistent. Real screen tests arrive with Phase 3c+ UI.
 *
 * NOT @AndroidEntryPoint-dependent: Robolectric doesn't run Hilt's
 * code gen, so we exercise the activity in a non-Hilt harness. The
 * instrumented tests (device-level) in a future phase will cover
 * the Hilt graph.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class FrontendActivitySmokeTest {

    @Test
    fun `FrontendActivity constructs without throwing`() {
        val activity = Robolectric.buildActivity(FrontendActivity::class.java).get()
        assertNotNull("FrontendActivity should instantiate", activity)
    }
}
