package jr.brian.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-function guards for [planHomeLaunches] — the "what should routeHome()
 * actually do?" planner. Covers every combination the settings UI can produce:
 *
 *  - display id + explicit target for each of TOP / BOTTOM / BOTH
 *  - TOP / BOTTOM resolve to the activity assigned to that slot under the
 *    current [ScreenAssignment]
 *  - BOTH's focus-ordering rule for each [MainScreen] value
 *  - single-display fallback for every target
 *  - BOTH coerced to BOTTOM, and TOP/BOTTOM coerced to MainActivity, when the
 *    frontend is off
 *
 * Testing the planner instead of `routeHome()` itself keeps these tests free
 * of Robolectric / ActivityOptions plumbing while still guarding the branch
 * the field actually cares about — the wrong display id or wrong launch order
 * are the two ways this feature can silently break.
 */
class HomeLaunchPlannerTest {

    private val bottomId = 7

    @Test
    fun `TOP launches the frontend on primary under the default FRONTEND_ON_TOP assignment`() {
        val plan = planHomeLaunches(
            bottomDisplayId = bottomId,
            target = HomeTarget.TOP,
            mainScreen = MainScreen.BOTTOM,
            frontendEnabled = true,
        )

        assertEquals(
            "TOP resolves to whichever activity owns the top slot — FrontEndActivity under the default assignment",
            listOf(HomeLaunch(HomeLaunchActivity.FRONTEND, PRIMARY_DISPLAY_ID)),
            plan
        )
    }

    @Test
    fun `BOTTOM launches MainActivity on the external display under the default FRONTEND_ON_TOP assignment`() {
        val plan = planHomeLaunches(
            bottomDisplayId = bottomId,
            target = HomeTarget.BOTTOM,
            mainScreen = MainScreen.BOTTOM,
            frontendEnabled = true,
        )

        assertEquals(
            "BOTTOM resolves to whichever activity owns the bottom slot — MainActivity under the default assignment",
            listOf(HomeLaunch(HomeLaunchActivity.MAIN, bottomId)),
            plan
        )
    }

    @Test
    fun `TOP launches MainActivity when the frontend is disabled`() {
        val plan = planHomeLaunches(
            bottomDisplayId = bottomId,
            target = HomeTarget.TOP,
            mainScreen = MainScreen.BOTTOM,
            frontendEnabled = false,
        )

        assertEquals(
            "with the frontend off there is only MainActivity to restore on the top slot",
            listOf(HomeLaunch(HomeLaunchActivity.MAIN, PRIMARY_DISPLAY_ID)),
            plan
        )
    }

    @Test
    fun `BOTH with mainScreen=BOTTOM launches frontend on top FIRST, then MainActivity on bottom`() {
        val plan = planHomeLaunches(
            bottomDisplayId = bottomId,
            target = HomeTarget.BOTH,
            mainScreen = MainScreen.BOTTOM,
            frontendEnabled = true,
        )

        assertEquals(
            "Focus goes to the last-started activity — MainActivity must be second so bottom wins focus",
            listOf(
                HomeLaunch(HomeLaunchActivity.FRONTEND, PRIMARY_DISPLAY_ID),
                HomeLaunch(HomeLaunchActivity.MAIN, bottomId),
            ),
            plan
        )
    }

    @Test
    fun `BOTH with mainScreen=TOP launches frontend on bottom FIRST, then MainActivity on top`() {
        val plan = planHomeLaunches(
            bottomDisplayId = bottomId,
            target = HomeTarget.BOTH,
            mainScreen = MainScreen.TOP,
            frontendEnabled = true,
        )

        assertEquals(
            "Focus goes to the last-started activity — MainActivity must be second so top wins focus",
            listOf(
                HomeLaunch(HomeLaunchActivity.FRONTEND, bottomId),
                HomeLaunch(HomeLaunchActivity.MAIN, PRIMARY_DISPLAY_ID),
            ),
            plan
        )
    }

    @Test
    fun `BOTH collapses to BOTTOM when the frontend is disabled`() {
        val plan = planHomeLaunches(
            bottomDisplayId = bottomId,
            target = HomeTarget.BOTH,
            mainScreen = MainScreen.TOP,
            frontendEnabled = false,
        )

        assertEquals(
            "BOTH's second activity is FrontEndActivity; without the frontend the target must fall back to a single MainActivity launch on the bottom",
            listOf(HomeLaunch(HomeLaunchActivity.MAIN, bottomId)),
            plan
        )
    }

    @Test
    fun `single-display fallback fires MainActivity with no explicit display id, for every target`() {
        HomeTarget.entries.forEach { target ->
            val plan = planHomeLaunches(
                bottomDisplayId = null,
                target = target,
                mainScreen = MainScreen.BOTTOM,
                frontendEnabled = true,
            )
            assertEquals(
                "target=$target should collapse to a single primary-display MainActivity launch",
                listOf(HomeLaunch(HomeLaunchActivity.MAIN, displayId = null)),
                plan
            )
        }
    }

    @Test
    fun `single-display fallback ignores mainScreen and frontendEnabled entirely`() {
        val combos = listOf(true, false).flatMap { fe ->
            MainScreen.entries.map { fe to it }
        }
        combos.forEach { (frontendEnabled, mainScreen) ->
            val plan = planHomeLaunches(
                bottomDisplayId = null,
                target = HomeTarget.BOTH,
                mainScreen = mainScreen,
                frontendEnabled = frontendEnabled,
            )
            assertEquals(
                "no-external-display fallback should be identical regardless of mainScreen ($mainScreen) or frontendEnabled ($frontendEnabled)",
                listOf(HomeLaunch(HomeLaunchActivity.MAIN, displayId = null)),
                plan
            )
        }
    }

    @Test
    fun `launcher plan with frontend off inherits the caller's display`() {
        val plan = planLauncherHomeLaunches(
            bottomDisplayId = bottomId,
            frontendEnabled = false,
        )

        assertEquals(
            "displayId==null lets Android place MainActivity on whichever display the external launcher targeted — the v2.7.1 behaviour Mjolnir depends on",
            listOf(HomeLaunch(HomeLaunchActivity.MAIN, displayId = null)),
            plan
        )
    }

    @Test
    fun `launcher plan with no external display always inherits the caller's display`() {
        listOf(true, false).forEach { frontendEnabled ->
            val plan = planLauncherHomeLaunches(
                bottomDisplayId = null,
                frontendEnabled = frontendEnabled,
            )
            assertEquals(
                "single-display devices should always collapse to an inheriting MainActivity launch (frontendEnabled=$frontendEnabled)",
                listOf(HomeLaunch(HomeLaunchActivity.MAIN, displayId = null)),
                plan
            )
        }
    }

    @Test
    fun `launcher plan with frontend on fires MainActivity on bottom and FrontEndActivity on top`() {
        val plan = planLauncherHomeLaunches(
            bottomDisplayId = bottomId,
            frontendEnabled = true,
        )

        assertEquals(
            "frontend-on launcher path should reproduce v2.7.1's dual-launch: MainActivity on the bottom, FrontEndActivity on the top",
            listOf(
                HomeLaunch(HomeLaunchActivity.MAIN, bottomId),
                HomeLaunch(HomeLaunchActivity.FRONTEND, PRIMARY_DISPLAY_ID),
            ),
            plan
        )
    }

    // ─────────── Phase 5: ScreenAssignment-aware tests ───────────

    @Test
    fun `FRONTEND_ON_BOTTOM swaps TOP to MainActivity on the external display`() {
        val plan = planHomeLaunches(
            bottomDisplayId = bottomId,
            target = HomeTarget.TOP,
            mainScreen = MainScreen.BOTTOM,
            frontendEnabled = true,
            assignment = ScreenAssignment.FRONTEND_ON_BOTTOM,
        )
        // After swap: the user's top slot now hosts MainActivity, and that
        // slot is physically the external display.
        assertEquals(
            listOf(HomeLaunch(HomeLaunchActivity.MAIN, bottomId)),
            plan,
        )
    }

    @Test
    fun `FRONTEND_ON_BOTTOM swaps BOTTOM to the frontend on the primary display`() {
        val plan = planHomeLaunches(
            bottomDisplayId = bottomId,
            target = HomeTarget.BOTTOM,
            mainScreen = MainScreen.BOTTOM,
            frontendEnabled = true,
            assignment = ScreenAssignment.FRONTEND_ON_BOTTOM,
        )
        // After swap: the user's bottom slot now hosts FrontEndActivity, and
        // that slot is physically the primary display.
        assertEquals(
            listOf(HomeLaunch(HomeLaunchActivity.FRONTEND, PRIMARY_DISPLAY_ID)),
            plan,
        )
    }

    @Test
    fun `FRONTEND_ON_BOTTOM with BOTH swaps both activities' display ids`() {
        val plan = planHomeLaunches(
            bottomDisplayId = bottomId,
            target = HomeTarget.BOTH,
            mainScreen = MainScreen.BOTTOM,
            frontendEnabled = true,
            assignment = ScreenAssignment.FRONTEND_ON_BOTTOM,
        )
        // With FRONTEND_ON_BOTTOM: user's "top" is the external
        // display, "bottom" is primary. MainScreen=BOTTOM pins
        // MainActivity on bottom (primary); frontend goes on top
        // (external, bottomId).
        assertEquals(
            listOf(
                HomeLaunch(HomeLaunchActivity.FRONTEND, bottomId),
                HomeLaunch(HomeLaunchActivity.MAIN, PRIMARY_DISPLAY_ID),
            ),
            plan,
        )
    }

    @Test
    fun `launcher plan with FRONTEND_ON_BOTTOM swaps MainActivity to top and FrontEndActivity to bottom`() {
        val plan = planLauncherHomeLaunches(
            bottomDisplayId = bottomId,
            frontendEnabled = true,
            assignment = ScreenAssignment.FRONTEND_ON_BOTTOM,
        )
        assertEquals(
            listOf(
                HomeLaunch(HomeLaunchActivity.MAIN, PRIMARY_DISPLAY_ID),
                HomeLaunch(HomeLaunchActivity.FRONTEND, bottomId),
            ),
            plan,
        )
    }
}
