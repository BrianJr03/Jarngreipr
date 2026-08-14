package jr.brian.home.ui.util

import jr.brian.home.model.HomeTarget
import jr.brian.home.model.MainScreen
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-function guards for [planHomeLaunches] — the "what should routeHome()
 * actually do?" planner. Covers every combination the settings UI can produce:
 *
 *  - display id + explicit target for each of TOP / BOTTOM / BOTH
 *  - BOTH's focus-ordering rule for each [MainScreen] value
 *  - single-display fallback for every target
 *  - BOTH coerced to BOTTOM when the frontend is off
 *
 * Testing the planner instead of `routeHome()` itself keeps these tests free
 * of Robolectric / ActivityOptions plumbing while still guarding the branch
 * the field actually cares about — the wrong display id or wrong launch order
 * are the two ways this feature can silently break.
 */
class HomeLaunchPlannerTest {

    private val bottomId = 7

    @Test
    fun `TOP launches MainActivity on the primary display`() {
        val plan = planHomeLaunches(
            bottomDisplayId = bottomId,
            target = HomeTarget.TOP,
            mainScreen = MainScreen.BOTTOM,
            frontendEnabled = true,
        )

        assertEquals(
            listOf(HomeLaunch(HomeLaunchActivity.MAIN, PRIMARY_DISPLAY_ID)),
            plan
        )
    }

    @Test
    fun `BOTTOM launches MainActivity on the external display`() {
        val plan = planHomeLaunches(
            bottomDisplayId = bottomId,
            target = HomeTarget.BOTTOM,
            mainScreen = MainScreen.BOTTOM,
            frontendEnabled = true,
        )

        assertEquals(
            listOf(HomeLaunch(HomeLaunchActivity.MAIN, bottomId)),
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
}
