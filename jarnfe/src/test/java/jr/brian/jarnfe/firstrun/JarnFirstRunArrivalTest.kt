package jr.brian.jarnfe.firstrun

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnFirstRunArrivalTest {
    private class FakeProgress(
        override var isDone: Boolean = false,
        override var savedStep: String? = null,
    ) : JarnFirstRunProgress {
        override fun saveStep(step: String) {
            savedStep = step
        }

        override fun markDone() {
            isDone = true
            savedStep = null
        }
    }

    private fun walk(start: JarnFirstRunState, twoScreens: Boolean): List<JarnFirstRunStep> {
        val seen = mutableListOf<JarnFirstRunStep>()
        var run: JarnFirstRunState? = start
        while (run != null) {
            seen += run.step
            run = run.next(twoScreens)
        }
        return seen
    }

    @Test
    fun `a Classic arrival runs Ready, Look and Done only`() {
        val controller = JarnFirstRunController(FakeProgress())
        controller.startArrival()
        val run = controller.state!!
        assertEquals(JarnFirstRunPath.Arrival, run.path)
        listOf(true, false).forEach { twoScreens ->
            assertEquals(
                listOf(JarnFirstRunStep.Ready, JarnFirstRunStep.Look, JarnFirstRunStep.Done),
                walk(run, twoScreens),
            )
        }
    }

    @Test
    fun `the full tour never shows Ready`() {
        listOf(true, false).forEach { twoScreens ->
            assertFalse(JarnFirstRunStep.Ready in walk(JarnFirstRunState(), twoScreens))
        }
    }

    @Test
    fun `finishing the short tour marks the first run done`() {
        val progress = FakeProgress()
        val controller = JarnFirstRunController(progress)
        controller.startArrival()
        repeat(3) { controller.next(twoScreens = true) }
        assertNull(controller.state)
        assertTrue(progress.isDone)
    }

    @Test
    fun `show me the buttons runs the lessons, then Done`() {
        val done = JarnFirstRunState(JarnFirstRunStep.Done, path = JarnFirstRunPath.Arrival)
        assertTrue(done.offersLessons)
        val twoScreens = done.withLessons(twoScreens = true)
        assertEquals(JarnFirstRunStep.SwitchCue, twoScreens.step)
        assertEquals(JarnFirstRunStep.Done, walk(twoScreens, true).last())
        assertTrue(JarnFirstRunStep.PressOtherTabs in walk(twoScreens, true))
        assertFalse(walk(twoScreens, true).last().let { JarnFirstRunState(it, path = twoScreens.path).offersLessons })

        val single = done.withLessons(twoScreens = false)
        assertEquals(JarnFirstRunStep.PressA, single.step)
        assertFalse(JarnFirstRunStep.SwitchCue in walk(single, false))
    }

    @Test
    fun `an arrival only shortens a tour that has not started`() {
        val midTour = JarnFirstRunController(FakeProgress(savedStep = JarnFirstRunStep.Look.name))
        midTour.startArrival()
        assertEquals(JarnFirstRunPath.Full, midTour.state?.path)
        assertEquals(JarnFirstRunStep.Look, midTour.state?.step)

        val done = JarnFirstRunController(FakeProgress(isDone = true))
        done.startArrival()
        assertNull(done.state)
    }

    @Test
    fun `a restart on Ready stays on the short tour`() {
        val progress = FakeProgress()
        JarnFirstRunController(progress).startArrival()
        assertEquals(JarnFirstRunStep.Ready.name, progress.savedStep)
        val restarted = JarnFirstRunController(progress)
        assertEquals(JarnFirstRunPath.Arrival, restarted.state?.path)
    }

    @Test
    fun `replay from Settings always plays the full tour`() {
        val controller = JarnFirstRunController(FakeProgress(isDone = true))
        controller.replay()
        controller.startArrival()
        assertEquals(JarnFirstRunStep.Welcome, controller.state?.step)
        assertEquals(JarnFirstRunPath.Full, controller.state?.path)
        assertEquals(JarnFirstRunStep.available(twoScreens = true), walk(controller.state!!, true))
    }

    @Test
    fun `Back on Ready asks before leaving the tour`() {
        val ready = JarnFirstRunState(JarnFirstRunStep.Ready, path = JarnFirstRunPath.Arrival)
        assertTrue(ready.back(twoScreens = true).confirmingSkip)
    }

    @Test
    fun `Ready starts on Next, or on the folder row without games`() {
        assertEquals(FirstRunStartFocus.Primary, firstRunStartFocus(JarnFirstRunStep.Ready, hasLeadRow = false))
        assertEquals(FirstRunStartFocus.LeadRow, firstRunStartFocus(JarnFirstRunStep.Ready, hasLeadRow = true))
        assertEquals(FirstRunStartFocus.Primary, firstRunStartFocus(JarnFirstRunStep.Look, hasLeadRow = true))
    }

    @Test
    fun `added rows replace built-ins by key and a leading row that needs a tap comes first`() {
        val folder = JarnReadyRow(READY_ROW_GAMES_FOLDER, "No games", isDone = false, "Add", {}, leads = true)
        val art = JarnReadyRow(READY_ROW_ART, "Art", isDone = true, order = 10)
        val esde = folder.copy(text = "Use ES-DE")
        val home = JarnReadyRow("home", "Home", isDone = false, "Make", {}, order = 50)
        val rows = mergeReadyRows(listOf(art, folder), listOf(home, esde))
        assertEquals(listOf("Use ES-DE", "Art", "Home"), rows.map { it.text })
        assertEquals(esde, readyLeadRow(rows))
        assertNull(readyLeadRow(mergeReadyRows(listOf(art), listOf(home))))
        assertNull(readyLeadRow(listOf(esde.copy(isDone = true))))
    }

    @Test
    fun `rows fade in 80ms apart, or together in 120ms with reduced motion`() {
        assertEquals(ReadyRowFade(160, 160), readyRowFade(2, reduced = false))
        assertEquals(ReadyRowFade(0, 120), readyRowFade(2, reduced = true))
    }
}
