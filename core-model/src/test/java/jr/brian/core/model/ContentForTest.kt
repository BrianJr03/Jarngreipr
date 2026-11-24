package jr.brian.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the 2 × 2 truth table that drives the pinned-slot swap. The
 * seamless-swap refactor relies on this being pure and total — if a
 * (slot, assignment) pair ever returned the wrong [SlotContent], both
 * displays would render the same content and the swap would appear to
 * do nothing (or render the launcher twice).
 */
class ContentForTest {

    @Test
    fun `frontend on top maps TOP to Frontend and BOTTOM to Launcher`() {
        assertEquals(
            SlotContent.Frontend,
            contentFor(ScreenSlot.TOP, ScreenAssignment.FRONTEND_ON_TOP),
        )
        assertEquals(
            SlotContent.Launcher,
            contentFor(ScreenSlot.BOTTOM, ScreenAssignment.FRONTEND_ON_TOP),
        )
    }

    @Test
    fun `frontend on bottom maps TOP to Launcher and BOTTOM to Frontend`() {
        assertEquals(
            SlotContent.Launcher,
            contentFor(ScreenSlot.TOP, ScreenAssignment.FRONTEND_ON_BOTTOM),
        )
        assertEquals(
            SlotContent.Frontend,
            contentFor(ScreenSlot.BOTTOM, ScreenAssignment.FRONTEND_ON_BOTTOM),
        )
    }

    @Test
    fun `each slot flips its content when the assignment flips`() {
        for (slot in ScreenSlot.entries) {
            val before = contentFor(slot, ScreenAssignment.FRONTEND_ON_TOP)
            val after = contentFor(slot, ScreenAssignment.FRONTEND_ON_BOTTOM)
            assertEquals(
                "slot=$slot should swap content when assignment flips",
                opposite(before),
                after,
            )
        }
    }

    @Test
    fun `the two slots always render different content for a given assignment`() {
        for (assignment in ScreenAssignment.entries) {
            val top = contentFor(ScreenSlot.TOP, assignment)
            val bottom = contentFor(ScreenSlot.BOTTOM, assignment)
            assertEquals(
                "both slots rendered the same content for $assignment",
                opposite(top),
                bottom,
            )
        }
    }

    private fun opposite(content: SlotContent): SlotContent = when (content) {
        SlotContent.Frontend -> SlotContent.Launcher
        SlotContent.Launcher -> SlotContent.Frontend
    }
}
