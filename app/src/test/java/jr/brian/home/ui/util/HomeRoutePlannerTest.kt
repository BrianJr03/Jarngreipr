package jr.brian.home.ui.util

import jr.brian.core.model.Experience
import jr.brian.core.model.HomeDestination
import jr.brian.core.model.HomeEntry
import jr.brian.core.model.HomeLaunch
import jr.brian.core.model.HomeLaunchActivity
import jr.brian.core.model.HomeTarget
import jr.brian.core.model.MainScreen
import jr.brian.core.model.PRIMARY_DISPLAY_ID
import jr.brian.core.model.ScreenAssignment
import jr.brian.core.model.planHomeLaunches
import jr.brian.core.model.planHomeRoute
import jr.brian.core.model.planLauncherHomeLaunches
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeRoutePlannerTest {

    private val displays = listOf(null, BOTTOM_DISPLAY)
    private val booleans = listOf(false, true)
    private val keyEntries = HomeTarget.entries.flatMap { target ->
        MainScreen.entries.map { screen -> HomeEntry.HomeKey(target, screen) }
    }
    private val allEntries: List<HomeEntry> = keyEntries + HomeEntry.Launcher

    @Test
    fun `classic launcher route matches the existing launcher plan`() {
        forEachState { bottom, frontendEnabled, assignment ->
            val route = planHomeRoute(
                entry = HomeEntry.Launcher,
                experience = Experience.Classic,
                jarnFeAvailable = false,
                bottomDisplayId = bottom,
                frontendEnabled = frontendEnabled,
                assignment = assignment,
            )

            assertEquals(HomeDestination.CLASSIC, route.destination)
            assertEquals(
                planLauncherHomeLaunches(bottom, frontendEnabled, assignment),
                route.classicLaunches,
            )
        }
    }

    @Test
    fun `classic home key route matches the existing target plan`() {
        keyEntries.forEach { entry ->
            forEachState { bottom, frontendEnabled, assignment ->
                val route = planHomeRoute(
                    entry = entry,
                    experience = Experience.Classic,
                    jarnFeAvailable = false,
                    bottomDisplayId = bottom,
                    frontendEnabled = frontendEnabled,
                    assignment = assignment,
                )

                assertEquals(HomeDestination.CLASSIC, route.destination)
                assertEquals(
                    planHomeLaunches(bottom, entry.target, entry.mainScreen, frontendEnabled, assignment),
                    route.classicLaunches,
                )
            }
        }
    }

    @Test
    fun `launcher with frontend off keeps the caller display`() {
        val route = planHomeRoute(
            entry = HomeEntry.Launcher,
            experience = Experience.Classic,
            jarnFeAvailable = false,
            bottomDisplayId = BOTTOM_DISPLAY,
            frontendEnabled = false,
        )

        assertEquals(listOf(HomeLaunch(HomeLaunchActivity.MAIN, null)), route.classicLaunches)
    }

    @Test
    fun `key fallback routes to the bottom slot`() {
        val route = planHomeRoute(
            entry = HomeEntry.HomeKey(HomeTarget.BOTTOM, MainScreen.BOTTOM),
            experience = Experience.Classic,
            jarnFeAvailable = false,
            bottomDisplayId = BOTTOM_DISPLAY,
            frontendEnabled = true,
        )

        assertEquals(
            listOf(HomeLaunch(HomeLaunchActivity.MAIN, BOTTOM_DISPLAY)),
            route.classicLaunches,
        )
    }

    @Test
    fun `both keeps focus ordering for the main screen`() {
        val route = planHomeRoute(
            entry = HomeEntry.HomeKey(HomeTarget.BOTH, MainScreen.TOP),
            experience = Experience.Classic,
            jarnFeAvailable = false,
            bottomDisplayId = BOTTOM_DISPLAY,
            frontendEnabled = true,
        )

        assertEquals(
            listOf(
                HomeLaunch(HomeLaunchActivity.MAIN, BOTTOM_DISPLAY),
                HomeLaunch(HomeLaunchActivity.FRONTEND, PRIMARY_DISPLAY_ID),
            ),
            route.classicLaunches,
        )
    }

    @Test
    fun `jarn fe without a destination falls back to the identical classic route`() {
        allEntries.forEach { entry ->
            forEachState { bottom, frontendEnabled, assignment ->
                val selected = planHomeRoute(
                    entry, Experience.JarnFE, false, bottom, frontendEnabled, assignment,
                )
                val classic = planHomeRoute(
                    entry, Experience.Classic, false, bottom, frontendEnabled, assignment,
                )

                assertEquals(classic, selected)
            }
        }
    }

    @Test
    fun `jarn fe with a destination wins and keeps a classic fallback plan`() {
        allEntries.forEach { entry ->
            forEachState { bottom, frontendEnabled, assignment ->
                val route = planHomeRoute(
                    entry, Experience.JarnFE, true, bottom, frontendEnabled, assignment,
                )
                val classic = planHomeRoute(
                    entry, Experience.Classic, true, bottom, frontendEnabled, assignment,
                )

                assertEquals(HomeDestination.JARN_FE, route.destination)
                assertEquals(HomeDestination.CLASSIC, classic.destination)
                assertEquals(classic.classicLaunches, route.classicLaunches)
            }
        }
    }

    @Test
    fun `experience names parse and anything else reads as classic`() {
        assertEquals(Experience.Classic, Experience.DEFAULT)
        assertEquals(Experience.JarnFE, Experience.fromNameOrDefault("JarnFE"))
        assertEquals(Experience.Classic, Experience.fromNameOrDefault("Classic"))
        assertEquals(Experience.Classic, Experience.fromNameOrDefault(null))
        assertEquals(Experience.Classic, Experience.fromNameOrDefault("jarnfe"))
        assertEquals(Experience.Classic, Experience.fromNameOrDefault("Future"))
    }

    private fun forEachState(block: (Int?, Boolean, ScreenAssignment) -> Unit) {
        displays.forEach { bottom ->
            booleans.forEach { frontendEnabled ->
                ScreenAssignment.entries.forEach { assignment ->
                    block(bottom, frontendEnabled, assignment)
                }
            }
        }
    }

    private companion object {
        const val BOTTOM_DISPLAY = 7
    }
}
