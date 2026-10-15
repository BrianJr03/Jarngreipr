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
): List<HomeLaunch> {
    if (bottomDisplayId == null || !frontendEnabled) {
        return listOf(HomeLaunch(HomeLaunchActivity.MAIN, displayId = null))
    }
    return listOf(
        HomeLaunch(HomeLaunchActivity.MAIN, bottomDisplayId),
        HomeLaunch(HomeLaunchActivity.FRONTEND, PRIMARY_DISPLAY_ID),
    )
}
