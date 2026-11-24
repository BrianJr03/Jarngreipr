package jr.brian.core.model

/**
 * Which display hosts the frontend on dual-screen hardware. Per the
 * Phase 0 decision: the frontend defaults to the top screen and the
 * launcher to the bottom; the user can swap at runtime and the
 * choice persists.
 *
 * Replaces the hardcoded `PRIMARY_DISPLAY_ID` assumption from the
 * old `launchFrontend` with an explicit assignment that
 * [planHomeLaunches] / [planLauncherHomeLaunches] / `routeHome`
 * all consult.
 *
 * ### Semantics
 *
 * The enum is which display the FRONTEND lives on. The launcher
 * always ends up on the other display. For single-display devices
 * the assignment collapses to a no-op — the launcher starts without
 * an explicit `launchDisplayId` and the frontend is never shown.
 */
enum class ScreenAssignment {
    /** Default — frontend on the top (primary) display, launcher on bottom. */
    FRONTEND_ON_TOP,

    /** Swapped — frontend on the bottom display, launcher on top. */
    FRONTEND_ON_BOTTOM;

    /** Flip. Used by the swap action to produce the next assignment. */
    fun flipped(): ScreenAssignment = when (this) {
        FRONTEND_ON_TOP -> FRONTEND_ON_BOTTOM
        FRONTEND_ON_BOTTOM -> FRONTEND_ON_TOP
    }

    companion object {
        val DEFAULT = FRONTEND_ON_TOP

        fun fromNameOrDefault(name: String?): ScreenAssignment =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
