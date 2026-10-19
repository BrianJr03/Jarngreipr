package jr.brian.core.model

/**
 * Display the Thor treats as primary — the top screen — and the target of
 * every gameplay/emulator launch. Pass to
 * `android.app.ActivityOptions.setLaunchDisplayId`.
 */
const val PRIMARY_DISPLAY_ID = 0

/**
 * A single `startActivity` planned by [planHomeLaunches]. Kept as a pure data
 * class so the plan can be exercised in unit tests without touching Android
 * intent/options plumbing.
 *
 * The plan preserves ordering — index 0 launches first, index 1 (if present)
 * second — because focus goes to the last-started activity. See `routeHome`
 * in `:app` for the focus-ordering contract.
 */
data class HomeLaunch(
    val activity: HomeLaunchActivity,
    /** `null` for the single-display fallback (no explicit display id). */
    val displayId: Int?,
)

enum class HomeLaunchActivity { MAIN, FRONTEND }

/**
 * Pure planner: given the display state and the user's preferences, returns
 * the ordered list of activity launches that `routeHome` will fire.
 *
 * [HomeTarget.TOP] / [HomeTarget.BOTTOM] are *slot* targets, not activity
 * targets — "restore whichever activity lives on the top (or bottom) screen
 * under the current [ScreenAssignment]." That lets the Home-button gestures
 * stay consistent across a user screen swap: single-press always brings back
 * the bottom slot, double-press always brings back the top slot, regardless
 * of whether the frontend or the launcher is physically there.
 *
 * Encapsulates the coercion rules so they can be unit-tested without
 * involving `Context` / `ActivityOptions`:
 *
 *  - No external display → single MainActivity launch with `displayId == null`
 *    (the manifest's default display resolution wins). All three targets
 *    collapse to this.
 *  - [HomeTarget.BOTH] with `frontendEnabled == false` → collapse to
 *    [HomeTarget.BOTTOM]; a disabled frontend also forces TOP/BOTTOM to
 *    resolve to `MainActivity` for both slots (there is no other activity to
 *    launch).
 *  - [HomeTarget.BOTH] → two launches, ordered so that the display named by
 *    [mainScreen] starts *last* and therefore takes input focus.
 */
fun planHomeLaunches(
    bottomDisplayId: Int?,
    target: HomeTarget,
    mainScreen: MainScreen,
    frontendEnabled: Boolean,
    assignment: ScreenAssignment = ScreenAssignment.DEFAULT,
): List<HomeLaunch> {
    if (bottomDisplayId == null) {
        return listOf(HomeLaunch(HomeLaunchActivity.MAIN, displayId = null))
    }
    val effectiveTarget =
        if (target == HomeTarget.BOTH && !frontendEnabled) HomeTarget.BOTTOM else target
    val (topId, bottomId) = displayIdsFor(bottomDisplayId, assignment)
    val (topActivity, bottomActivity) = activitiesFor(assignment, frontendEnabled)
    return when (effectiveTarget) {
        HomeTarget.TOP -> listOf(HomeLaunch(topActivity, topId))
        HomeTarget.BOTTOM -> listOf(HomeLaunch(bottomActivity, bottomId))
        HomeTarget.BOTH -> when (mainScreen) {
            MainScreen.TOP -> listOf(
                HomeLaunch(HomeLaunchActivity.FRONTEND, bottomId),
                HomeLaunch(HomeLaunchActivity.MAIN, topId),
            )
            MainScreen.BOTTOM -> listOf(
                HomeLaunch(HomeLaunchActivity.FRONTEND, topId),
                HomeLaunch(HomeLaunchActivity.MAIN, bottomId),
            )
        }
    }
}

/**
 * Which activity owns each perceived slot under the given [assignment]. With
 * the frontend disabled there is no second activity to place, so both slots
 * collapse to [HomeLaunchActivity.MAIN] — a TOP or BOTTOM gesture still routes
 * MainActivity to the requested display without trying to launch a disabled
 * frontend.
 */
private fun activitiesFor(
    assignment: ScreenAssignment,
    frontendEnabled: Boolean,
): Pair<HomeLaunchActivity, HomeLaunchActivity> {
    if (!frontendEnabled) return HomeLaunchActivity.MAIN to HomeLaunchActivity.MAIN
    return when (assignment) {
        ScreenAssignment.FRONTEND_ON_TOP ->
            HomeLaunchActivity.FRONTEND to HomeLaunchActivity.MAIN
        ScreenAssignment.FRONTEND_ON_BOTTOM ->
            HomeLaunchActivity.MAIN to HomeLaunchActivity.FRONTEND
    }
}

/**
 * Resolves which concrete display id MainActivity and the frontend
 * each live on, given the user's [assignment]. The naming here keeps
 * the planner readable — `topId` is wherever "top" means to the user,
 * `bottomId` is "bottom" — rather than literal hardware primary/secondary.
 *
 * - [ScreenAssignment.FRONTEND_ON_TOP] (default): main gets primary,
 *   frontend gets the external display.
 * - [ScreenAssignment.FRONTEND_ON_BOTTOM]: main gets the external
 *   display, frontend gets primary.
 */
private fun displayIdsFor(
    externalDisplayId: Int,
    assignment: ScreenAssignment,
): Pair<Int, Int> = when (assignment) {
    ScreenAssignment.FRONTEND_ON_TOP -> PRIMARY_DISPLAY_ID to externalDisplayId
    ScreenAssignment.FRONTEND_ON_BOTTOM -> externalDisplayId to PRIMARY_DISPLAY_ID
}

/**
 * Launcher-path planner: what `routeHomeFromLauncher` should fire when
 * `jr.brian.home.HomeRouterActivity` is entered from the LAUNCHER intent
 * (an external launcher like Mjolnir, or the system when Jarngreipr is the
 * default home).
 *
 * Deliberately ignores [HomeTarget] / [MainScreen] — those are governed by
 * the "Home Button Interception" setting, which only applies to
 * `jr.brian.home.service.HomeInterceptorService` (the accessibility path).
 * The router activity's job is to preserve v2.7.1's caller-display behaviour
 * so external launchers can route Jarngreipr to whichever display they asked
 * for.
 *
 *  - No external display (or frontend disabled) → single MainActivity launch
 *    with `displayId == null`. Because no explicit `launchDisplayId` is set,
 *    Android puts MainActivity on the same display HomeRouterActivity was
 *    started on — i.e. the display the external launcher targeted.
 *  - Frontend enabled + external display → MainActivity on the bottom
 *    display plus the frontend on the top display, matching v2.7.1's
 *    dual-launch behaviour when the frontend is on.
 */
fun planLauncherHomeLaunches(
    bottomDisplayId: Int?,
    frontendEnabled: Boolean,
    assignment: ScreenAssignment = ScreenAssignment.DEFAULT,
): List<HomeLaunch> {
    if (bottomDisplayId == null || !frontendEnabled) {
        return listOf(HomeLaunch(HomeLaunchActivity.MAIN, displayId = null))
    }
    return when (assignment) {
        ScreenAssignment.FRONTEND_ON_TOP -> listOf(
            HomeLaunch(HomeLaunchActivity.MAIN, bottomDisplayId),
            HomeLaunch(HomeLaunchActivity.FRONTEND, PRIMARY_DISPLAY_ID),
        )
        ScreenAssignment.FRONTEND_ON_BOTTOM -> listOf(
            HomeLaunch(HomeLaunchActivity.MAIN, PRIMARY_DISPLAY_ID),
            HomeLaunch(HomeLaunchActivity.FRONTEND, bottomDisplayId),
        )
    }
}
