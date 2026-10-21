package jr.brian.core.model

/**
 * Which of the two physical displays a given host activity owns. The
 * assignment is permanent for the activity's lifetime — PR 2 of the
 * seamless-swap work pins each activity to one display and routes the
 * swap through content changes instead of window reparenting, so the
 * slot never changes under the composition.
 *
 * - [TOP] is the primary display (`Display.DEFAULT_DISPLAY`).
 * - [BOTTOM] is the external display on dual-screen hardware.
 *
 * The display-count semantics live elsewhere ([ScreenAssignment]
 * decides *which content* a slot renders); this enum only encodes
 * *where the slot physically draws*.
 */
enum class ScreenSlot {
    TOP,
    BOTTOM,
}
