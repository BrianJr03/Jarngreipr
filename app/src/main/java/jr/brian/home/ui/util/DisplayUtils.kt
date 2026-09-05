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
import jr.brian.home.MainActivity
import jr.brian.home.esde.ui.FrontEndActivity
import jr.brian.home.model.HomeTarget
import jr.brian.home.model.MainScreen

/**
 * Display the Thor treats as primary — the top screen — and the target of every
 * gameplay/emulator launch. Pass to [android.app.ActivityOptions.setLaunchDisplayId].
 */
const val PRIMARY_DISPLAY_ID = 0

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
 * A single `startActivity` planned by [planHomeLaunches]. Kept as a pure data
 * class so the plan can be exercised in unit tests without touching Android
 * intent/options plumbing.
 *
 * The plan preserves ordering — index 0 launches first, index 1 (if present)
 * second — because focus goes to the last-started activity. See [routeHome]
 * for the focus-ordering contract.
 */
data class HomeLaunch(
    val activity: HomeLaunchActivity,
    /** `null` for the single-display fallback (no explicit display id). */
    val displayId: Int?,
)

enum class HomeLaunchActivity { MAIN, FRONTEND }

/**
 * Pure planner: given the display state and the user's preferences, returns
 * the ordered list of activity launches that [routeHome] will fire.
 *
 * Encapsulates the three coercion rules so they can be unit-tested without
 * involving `Context` / `ActivityOptions`:
 *
 *  - No external display → single MainActivity launch with `displayId == null`
 *    (the manifest's default display resolution wins). All three targets
 *    collapse to this.
 *  - [HomeTarget.BOTH] with `frontendEnabled == false` → collapse to
 *    [HomeTarget.BOTTOM].
 *  - [HomeTarget.BOTH] → two launches, ordered so that the display named by
 *    [mainScreen] starts *last* and therefore takes input focus.
 */
fun planHomeLaunches(
    bottomDisplayId: Int?,
    target: HomeTarget,
    mainScreen: MainScreen,
    frontendEnabled: Boolean,
): List<HomeLaunch> {
    if (bottomDisplayId == null) {
        return listOf(HomeLaunch(HomeLaunchActivity.MAIN, displayId = null))
    }
    val effectiveTarget =
        if (target == HomeTarget.BOTH && !frontendEnabled) HomeTarget.BOTTOM else target
    return when (effectiveTarget) {
        HomeTarget.TOP -> listOf(HomeLaunch(HomeLaunchActivity.MAIN, PRIMARY_DISPLAY_ID))
        HomeTarget.BOTTOM -> listOf(HomeLaunch(HomeLaunchActivity.MAIN, bottomDisplayId))
        HomeTarget.BOTH -> when (mainScreen) {
            MainScreen.TOP -> listOf(
                HomeLaunch(HomeLaunchActivity.FRONTEND, bottomDisplayId),
                HomeLaunch(HomeLaunchActivity.MAIN, PRIMARY_DISPLAY_ID),
            )
            MainScreen.BOTTOM -> listOf(
                HomeLaunch(HomeLaunchActivity.FRONTEND, PRIMARY_DISPLAY_ID),
                HomeLaunch(HomeLaunchActivity.MAIN, bottomDisplayId),
            )
        }
    }
}

/**
 * Routes a Home-button press to one or two activities according to [target].
 *
 * Callable from any [Context] — Activity, Service, or Application — because it
 * only uses [Intent.FLAG_ACTIVITY_NEW_TASK] and never touches Activity-only APIs.
 *
 *  - [HomeTarget.TOP] — MainActivity on the primary display; no frontend.
 *  - [HomeTarget.BOTTOM] — MainActivity on the external/bottom display; no
 *    frontend. Falls back to the primary display when no external display is
 *    connected.
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
 * test setup. See [planHomeLaunches] for the same rule as pure data.
 */
fun routeHome(
    context: Context,
    target: HomeTarget,
    mainScreen: MainScreen,
    frontendEnabled: Boolean,
) {
    val plan = planHomeLaunches(
        bottomDisplayId = resolveBottomDisplayId(context),
        target = target,
        mainScreen = mainScreen,
        frontendEnabled = frontendEnabled,
    )
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
