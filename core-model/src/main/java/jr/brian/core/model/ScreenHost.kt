package jr.brian.core.model

/**
 * The pinned-slot abstraction over a host activity. Content
 * composables (launcher content, frontend content) accept a
 * [ScreenHost] so they can finish their containing activity when a
 * game is launched without caring whether that activity is
 * MainActivity or FrontEndActivity — only "the host that owns the
 * primary display" needs to go away during a game.
 *
 * Kept interface-shaped so [:core-model] stays free of Android
 * `Activity` references. The two concrete implementations in :app
 * each hard-wire [slot] and point [finishIfOn] at their own
 * `finish()`.
 */
interface ScreenHost {
    /** Which physical display this host owns. Constant for the host's lifetime. */
    val slot: ScreenSlot

    /**
     * Finish the host activity if it is currently rendering on
     * [displayId]. Used by the game-launch path to clear the primary
     * display for an incoming game while leaving the external host
     * alive — on `FRONTEND_ON_BOTTOM` the external host keeps the
     * frontend visible while the game runs on primary.
     */
    fun finishIfOn(displayId: Int)
}
