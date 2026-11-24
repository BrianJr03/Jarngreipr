package jr.brian.core.model

/**
 * Which content a [ScreenSlot] renders at a given moment. Together
 * with [ScreenAssignment], a pure function ([contentFor]) decides
 * what a pinned activity should show — the swap then only has to
 * flip the assignment and let both activities recompose.
 */
enum class SlotContent {
    /** The launcher UI (home pages, dock, app drawer, canvas tabs). */
    Launcher,

    /** The frontend UI (systems grid, game lists, scraping overlays). */
    Frontend,
}

/**
 * Pure mapping from a slot + current assignment to the content that
 * slot should render. Pinned-slot design means [slot] is constant
 * for the host's lifetime; the swap flips [assignment].
 *
 * The frontend lives wherever the assignment points. The launcher
 * takes the other slot. Single-display scenarios surface as the
 * caller never instantiating the second slot — this function stays
 * total.
 */
fun contentFor(slot: ScreenSlot, assignment: ScreenAssignment): SlotContent =
    when (assignment) {
        ScreenAssignment.FRONTEND_ON_TOP -> when (slot) {
            ScreenSlot.TOP -> SlotContent.Frontend
            ScreenSlot.BOTTOM -> SlotContent.Launcher
        }
        ScreenAssignment.FRONTEND_ON_BOTTOM -> when (slot) {
            ScreenSlot.TOP -> SlotContent.Launcher
            ScreenSlot.BOTTOM -> SlotContent.Frontend
        }
    }
