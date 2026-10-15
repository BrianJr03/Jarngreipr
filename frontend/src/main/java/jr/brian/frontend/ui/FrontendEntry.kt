package jr.brian.frontend.ui

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * The one supported way for `:app` to start the frontend. Keeps the
 * module boundary clean (plan §3.1: `:app` reaches `:frontend` only
 * through `FrontendEntry`, `FrontendBus`, and `LibraryRepository`);
 * callers never `import jr.brian.frontend.ui.FrontendActivity`
 * directly.
 *
 * The dual-screen `displayId` plumbing from `:app`'s old
 * `launchFrontend` moves here so the swap in Phase 3h is a one-site
 * change — the launcher asks `FrontendEntry` to pick the right pane
 * instead of knowing which Activity class to spin up.
 */
object FrontendEntry {

    /**
     * An Intent that starts the frontend. The returned Intent carries
     * the same new-task + reorder-to-front flags the old launcher
     * used so a reopen keeps task identity.
     */
    fun intent(context: Context): Intent =
        Intent(context, FrontendActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }

    /**
     * Start the frontend. If [displayId] is non-null, routes it to
     * that physical display via ActivityOptions — mirrors `:app`'s
     * old `launchFrontend` which pinned to the top screen on dual-
     * screen hardware.
     */
    fun launch(context: Context, displayId: Int? = null) {
        val options = displayId?.let {
            ActivityOptions.makeBasic().setLaunchDisplayId(it).toBundle()
        }
        context.startActivity(intent(context), options as Bundle?)
    }
}
