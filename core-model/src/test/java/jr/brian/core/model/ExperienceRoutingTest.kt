package jr.brian.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ExperienceRoutingTest {
    @Test
    fun `a build without jarn fe always opens classic`() {
        listOf(null, Experience.Classic, Experience.JarnFE).forEach { stored ->
            assertEquals(
                Experience.Classic,
                resolveLauncherExperience(jarnAvailable = false, stored = stored, forceClassic = false),
            )
        }
    }

    @Test
    fun `no saved choice opens classic even when jarn fe is available`() {
        assertEquals(
            Experience.Classic,
            resolveLauncherExperience(jarnAvailable = true, stored = null, forceClassic = false),
        )
    }

    @Test
    fun `a saved choice is followed in both directions`() {
        Experience.entries.forEach { stored ->
            assertEquals(
                stored,
                resolveLauncherExperience(jarnAvailable = true, stored = stored, forceClassic = false),
            )
        }
    }

    @Test
    fun `the classic override beats a saved jarn fe choice`() {
        assertEquals(
            Experience.Classic,
            resolveLauncherExperience(jarnAvailable = true, stored = Experience.JarnFE, forceClassic = true),
        )
    }

    @Test
    fun `returning on one screen opens the launcher where the caller is`() {
        assertEquals(
            listOf(HomeLaunch(HomeLaunchActivity.MAIN, displayId = null)),
            planClassicReturnLaunches(bottomDisplayId = null, frontendEnabled = true),
        )
    }

    @Test
    fun `returning on two screens with the frontend opens both`() {
        assertEquals(
            listOf(
                HomeLaunch(HomeLaunchActivity.MAIN, 4),
                HomeLaunch(HomeLaunchActivity.FRONTEND, PRIMARY_DISPLAY_ID),
            ),
            planClassicReturnLaunches(bottomDisplayId = 4, frontendEnabled = true),
        )
    }

    @Test
    fun `returning without the frontend keeps the launcher on the bottom screen`() {
        assertEquals(
            listOf(HomeLaunch(HomeLaunchActivity.MAIN, 4)),
            planClassicReturnLaunches(bottomDisplayId = 4, frontendEnabled = false),
        )
    }
}
