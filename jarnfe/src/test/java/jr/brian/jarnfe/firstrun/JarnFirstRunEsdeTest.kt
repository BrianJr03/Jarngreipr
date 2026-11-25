package jr.brian.jarnfe.firstrun

import jr.brian.jarnfe.session.JarnSessionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnFirstRunEsdeTest {

    @Test
    fun `the ES-DE step is left out until it is offered`() {
        assertFalse(JarnFirstRunStep.Esde in JarnFirstRunStep.available(twoScreens = true))
        val offered = JarnFirstRunStep.available(twoScreens = true, offersEsde = true)
        assertEquals(JarnFirstRunStep.Esde, offered[offered.indexOf(JarnFirstRunStep.Library) + 1])
        assertEquals(JarnFirstRunStep.Media, offered[offered.indexOf(JarnFirstRunStep.Esde) + 1])
    }

    @Test
    fun `an offer puts the step between Library and Media`() {
        val run = JarnFirstRunState(JarnFirstRunStep.Library).withEsdeOffer(true, twoScreens = true)

        val esde = run.next(twoScreens = true)!!
        assertEquals(JarnFirstRunStep.Esde, esde.step)
        assertTrue(esde.offersEsde)
        assertEquals(JarnFirstRunStep.Media, esde.next(twoScreens = true)?.step)
        assertEquals(JarnFirstRunStep.Library, esde.back(twoScreens = true).step)
        assertEquals(run.total(twoScreens = true), JarnFirstRunState().total(twoScreens = true) + 1)
    }

    @Test
    fun `losing the offer on the ES-DE step moves on to Media`() {
        val on = JarnFirstRunState(JarnFirstRunStep.Esde, offersEsde = true)

        assertEquals(JarnFirstRunStep.Media, on.withEsdeOffer(false, twoScreens = true).step)
    }

    @Test
    fun `the offer stays with the tour as it moves`() {
        val controller = JarnFirstRunController(FakeProgress(savedStep = JarnFirstRunStep.Library.name))
        controller.offerEsde(true, twoScreens = true)

        controller.next(twoScreens = true)

        assertEquals(JarnFirstRunStep.Esde, controller.state?.step)
        assertTrue(controller.state?.offersEsde == true)
    }

    @Test
    fun `the ES-DE step's primary is its own until the import is done`() {
        val controller = JarnFirstRunController(FakeProgress(savedStep = JarnFirstRunStep.Esde.name))
        controller.offerEsde(true, twoScreens = false)
        var handled = true
        val actions = JarnFirstRunActions(controller, JarnSessionStore(), esdePrimary = { handled })

        actions.primary()
        assertEquals(JarnFirstRunStep.Esde, controller.state?.step)

        handled = false
        actions.primary()
        assertEquals(JarnFirstRunStep.Media, controller.state?.step)
    }

    @Test
    fun `skipping the tour tells the ES-DE notice`() {
        var skipped = 0
        val actions = JarnFirstRunActions(
            JarnFirstRunController(FakeProgress()),
            JarnSessionStore(),
            onSkipped = { skipped++ },
        )

        actions.skip()

        assertEquals(1, skipped)
    }

    private class FakeProgress(
        override var isDone: Boolean = false,
        override var savedStep: String? = null,
    ) : JarnFirstRunProgress {
        override fun saveStep(step: String) {
            savedStep = step
        }

        override fun markDone() {
            isDone = true
        }
    }
}
