package jr.brian.home.model

/**
 * Where a Home-button press routes the launcher.
 *
 * Consumed by [jr.brian.home.ui.util.routeHome], which resolves each value into
 * one or two `startActivity` calls with explicit `launchDisplayId`s.
 *
 * The choice is meaningful only on dual-screen hardware (e.g. AYN Thor). When
 * [jr.brian.home.ui.util.resolveBottomDisplayId] returns `null` all three
 * values collapse to a single MainActivity launch on the primary display.
 */
enum class HomeTarget {
    /** MainActivity on the primary (top) display only. FrontEndActivity is not launched. */
    TOP,

    /**
     * MainActivity on the external (bottom) display only. Matches the launcher's
     * pre-selector behaviour when the frontend is disabled.
     */
    BOTTOM,

    /**
     * MainActivity on one display and [jr.brian.home.esde.ui.FrontEndActivity] on
     * the other, launched together. Requires the frontend feature to be enabled.
     *
     * `MainActivity` is declared `singleTask` in the manifest, so it cannot exist
     * on two displays at once; the second display always hosts FrontEndActivity.
     * The [MainScreen] value decides which display MainActivity occupies (and
     * therefore which display receives input focus).
     */
    BOTH;

    companion object {
        /** Parses a stored name back to [HomeTarget], falling back to [BOTTOM]. */
        fun fromNameOrDefault(name: String?): HomeTarget =
            entries.firstOrNull { it.name == name } ?: BOTTOM
    }
}
