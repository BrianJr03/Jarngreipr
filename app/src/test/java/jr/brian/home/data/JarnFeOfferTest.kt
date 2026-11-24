package jr.brian.home.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnFeOfferTest {

    @Test
    fun `release build without the flag is not offered and never looks up the activity`() {
        var lookedUp = false
        val offered = shouldOfferJarnFe(
            isDebugBuild = false,
            offerInReleaseBuilds = false,
            activityInstalled = {
                lookedUp = true
                true
            },
        )
        assertFalse(offered)
        assertFalse(lookedUp)
    }

    @Test
    fun `debug build with the activity is offered`() {
        assertTrue(shouldOfferJarnFe(true, false) { true })
    }

    @Test
    fun `debug build without the activity is not offered`() {
        assertFalse(shouldOfferJarnFe(true, false) { false })
    }

    @Test
    fun `flipped flag offers a release build that has the activity`() {
        assertTrue(shouldOfferJarnFe(false, true) { true })
    }

    @Test
    fun `flipped flag never offers a release build missing the activity`() {
        assertFalse(shouldOfferJarnFe(false, true) { false })
    }
}
