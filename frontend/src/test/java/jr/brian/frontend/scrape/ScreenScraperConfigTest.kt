package jr.brian.frontend.scrape

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phase 4a dev-cred config. In unit tests the BuildConfig values are
 * whatever the local gradle environment emitted — empty strings when
 * the SCREENSCRAPER_DEV_* env vars aren't set (the normal local
 * developer case). The test pins the dependent-on-build behaviour:
 * `hasDevCredentials` is the AND of two non-blank BuildConfig fields.
 */
class ScreenScraperConfigTest {

    @Test
    fun `hasDevCredentials matches the AND of the two fields`() {
        val config = ScreenScraperConfig()
        val expected = config.devId.isNotBlank() && config.devPassword.isNotBlank()
        assertEquals(expected, config.hasDevCredentials)
    }

    @Test
    fun `the BuildConfig fields are observable at runtime`() {
        // Just prove the fields are reachable; the actual value depends
        // on whether SCREENSCRAPER_DEV_ID / SCREENSCRAPER_DEV_PASSWORD
        // are set in the gradle environment.
        val config = ScreenScraperConfig()
        // No exception thrown == pass. If BuildConfig generation were
        // broken this would throw ClassNotFound at access time.
        config.devId
        config.devPassword
    }
}
