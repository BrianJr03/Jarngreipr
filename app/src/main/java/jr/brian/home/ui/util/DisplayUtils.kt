package jr.brian.home.ui.util

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.os.Handler
import android.os.Looper
import jr.brian.core.model.HomeLaunch
import jr.brian.core.model.HomeLaunchActivity
import jr.brian.core.model.HomeTarget
import jr.brian.core.model.MainScreen
import jr.brian.core.model.PRIMARY_DISPLAY_ID
import jr.brian.core.model.ScreenAssignment
import jr.brian.core.model.planHomeLaunches
import jr.brian.core.model.planLauncherHomeLaunches
import jr.brian.home.MainActivity
import jr.brian.home.esde.ui.FrontEndActivity

/**
 * Returns the display id the bottom screen lives on (anything other than
 * [PRIMARY_DISPLAY_ID]), or `null` when the device only has a single display.
 */
fun resolveBottomDisplayId(context: Context): Int? {
    val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    return displayManager.displays
        .firstOrNull { it.displayId != PRIMARY_DISPLAY_ID }
        ?.displayId
}

/**
 * Fires [FrontEndActivity] (Systems → Games browse experience). Defaults to the
 * top display; supply [displayId] to route it elsewhere (used by
 * [HomeTarget.BOTH] with [MainScreen.TOP], where the frontend hosts the
 * non-focused screen).
 */
fun launchFrontend(context: Context, displayId: Int = PRIMARY_DISPLAY_ID) {
    val intent = Intent(context, FrontEndActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
    }
    val options = ActivityOptions.makeBasic().apply { launchDisplayId = displayId }
    context.startActivity(intent, options.toBundle())
}

/**
 * Fires [FrontEndActivity] on whichever display the current
 * [ScreenAssignment] has it live on. Use this when the caller just
 * wants "surface the frontend wherever it belongs" rather than
 * targeting a specific display — e.g. the Home-button double-press
 * gesture after a user screen swap. Falls back to [PRIMARY_DISPLAY_ID]
 * when the assignment points at an external display that isn't
 * currently connected.
 */
fun launchFrontendForCurrentAssignment(context: Context) {
    val displayId = when (currentAssignment(context)) {
        ScreenAssignment.FRONTEND_ON_TOP -> PRIMARY_DISPLAY_ID
        ScreenAssignment.FRONTEND_ON_BOTTOM ->
            resolveBottomDisplayId(context) ?: PRIMARY_DISPLAY_ID
    }
    launchFrontend(context, displayId)
}

/**
 * Fires [MainActivity] on whichever display the current
 * [ScreenAssignment] has it live on (the inverse of
 * [launchFrontendForCurrentAssignment]). Used by the companion
 * watchdog in [FrontEndActivity]'s `onStart` when it notices the
 * launcher side isn't up and needs to be brought along, so the user
 * doesn't end up with just one of the two activities after a crash
 * or partial restart.
 */
fun launchMainForCurrentAssignment(context: Context) {
    val displayId = when (currentAssignment(context)) {
        ScreenAssignment.FRONTEND_ON_TOP -> resolveBottomDisplayId(context)
        ScreenAssignment.FRONTEND_ON_BOTTOM -> PRIMARY_DISPLAY_ID
    }
    dispatchMain(context, displayId)
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
internal interface LaunchFrontendEntryPoint {
    fun screenAssignmentStore(): jr.brian.home.data.ScreenAssignmentStore
}

/**
 * Current persisted [ScreenAssignment], consulted by every launch
 * path. Collapses to [ScreenAssignment.DEFAULT] if Hilt isn't
 * reachable (unit test, early boot) so pre-Phase-5 behaviour is
 * preserved as the fallback.
 */
private fun currentAssignment(context: Context): ScreenAssignment = try {
    dagger.hilt.android.EntryPointAccessors.fromApplication(
        context.applicationContext,
        LaunchFrontendEntryPoint::class.java,
    ).screenAssignmentStore().assignment
} catch (_: Throwable) {
    ScreenAssignment.DEFAULT
}

/**
 * Routes a Home-button press to one or two activities according to [target].
 *
 * Callable from any [Context] — Activity, Service, or Application — because it
 * only uses [Intent.FLAG_ACTIVITY_NEW_TASK] and never touches Activity-only APIs.
 *
 *  - [HomeTarget.TOP] — restore whichever activity owns the top slot under
 *    the current [ScreenAssignment]. On the default assignment that's the
 *    frontend; after a screen swap it's MainActivity.
 *  - [HomeTarget.BOTTOM] — mirror of TOP for the bottom slot. Falls back to
 *    the primary display when no external display is connected.
 *  - [HomeTarget.BOTH] — MainActivity on the display named by [mainScreen] and
 *    FrontEndActivity on the other display. Coerced to [HomeTarget.BOTTOM]
 *    when [frontendEnabled] is false (BOTH's second activity is the frontend
 *    and it cannot be shown while disabled), and further coerced to a single
 *    MainActivity launch on the primary display when no external display is
 *    connected. `MainActivity` is `singleTask` in the manifest, so it cannot
 *    exist on two displays at once — the "other" display always hosts
 *    FrontEndActivity, never a second MainActivity.
 *
 * ### Focus ordering
 *
 * Android brings whichever activity started **last** to the foreground for
 * input focus. To make the display named by [mainScreen] the focused one, the
 * *other* display is launched first and the focused display second:
 *
 *  - [MainScreen.TOP] → launch bottom (FrontEndActivity), then top (MainActivity)
 *  - [MainScreen.BOTTOM] → launch top (FrontEndActivity), then bottom (MainActivity)
 *
 * Do not "tidy" this ordering. Without the deliberate reversal a future
 * refactor will swap the two calls and silently move focus to the wrong
 * screen — and dual-screen focus bugs are hard to spot from a single-display
 * test setup. See `planHomeLaunches` in :core-model for the same rule as
 * pure data.
 */
fun routeHome(
    context: Context,
    target: HomeTarget,
    mainScreen: MainScreen,
    frontendEnabled: Boolean,
) {
    dispatchPlan(
        context,
        planHomeLaunches(
            bottomDisplayId = resolveBottomDisplayId(context),
            target = target,
            mainScreen = mainScreen,
            frontendEnabled = frontendEnabled,
            assignment = currentAssignment(context),
        ),
    )
}

/**
 * Routes a launcher-entry Home press. See `planLauncherHomeLaunches` in
 * :core-model for the rule; this is the [Context]-aware wrapper that actually
 * fires the intents.
 */
fun routeHomeFromLauncher(context: Context, frontendEnabled: Boolean) {
    dispatchPlan(
        context,
        planLauncherHomeLaunches(
            bottomDisplayId = resolveBottomDisplayId(context),
            frontendEnabled = frontendEnabled,
            assignment = currentAssignment(context),
        ),
    )
}

private fun dispatchPlan(context: Context, plan: List<HomeLaunch>) {
    plan.forEach { launch ->
        when (launch.activity) {
            HomeLaunchActivity.MAIN -> dispatchMain(context, launch.displayId)
            HomeLaunchActivity.FRONTEND -> launchFrontend(context, launch.displayId ?: PRIMARY_DISPLAY_ID)
        }
    }
}

private fun dispatchMain(context: Context, displayId: Int?) {
    val flags = if (displayId == null) {
        Intent.FLAG_ACTIVITY_NEW_TASK
    } else {
        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
    }
    val intent = Intent(context, MainActivity::class.java).addFlags(flags)
    if (displayId == null) {
        context.startActivity(intent)
    } else {
        val options = ActivityOptions.makeBasic().apply { launchDisplayId = displayId }
        context.startActivity(intent, options.toBundle())
    }
}

/**
 * Checks if the device has an external display connected.
 * @return true if there are multiple displays (including external), false otherwise
 */
@Composable
fun rememberHasExternalDisplay(): Boolean {
    val context = LocalContext.current
    var hasExternal by remember {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        mutableStateOf(displayManager.displays.size > 1)
    }
    DisposableEffect(context) {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val recompute = { hasExternal = displayManager.displays.size > 1 }
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = recompute()
            override fun onDisplayRemoved(displayId: Int) = recompute()
            override fun onDisplayChanged(displayId: Int) = recompute()
        }
        displayManager.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        onDispose { displayManager.unregisterDisplayListener(listener) }
    }
    return hasExternal
}
