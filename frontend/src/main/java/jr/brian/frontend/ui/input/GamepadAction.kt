package jr.brian.frontend.ui.input

/**
 * Semantic gamepad / keyboard actions the frontend reacts to. The
 * input layer translates raw KeyEvents into these so a screen's
 * handler doesn't care which hardware button fired.
 *
 * Plan §3.2 default mapping: B = back everywhere, START = settings,
 * Y = search, X = sort. Later Phase 3f lets the user remap these;
 * for now the defaults are hard-wired in [GamepadKeyRouter].
 */
enum class GamepadAction {
    /** Confirm the focused item (A on gamepads, Enter on keyboards). */
    Confirm,

    /** Back / cancel (B on gamepads, Escape on keyboards). */
    Back,

    /** Open the search surface (Y on gamepads, / on keyboards). */
    Search,

    /** Open the sort menu (X on gamepads). */
    Sort,

    /** Open settings (START on gamepads, F10 on keyboards). */
    Settings,

    /** Jump by a page (L1/R1 shoulder buttons). */
    PageLeft,
    PageRight,

    /** Jump to the first / last entry (L2/R2 triggers). */
    JumpStart,
    JumpEnd,
}
