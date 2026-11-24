package jr.brian.home.jarn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppJarnHomeAppTest {
    @Test
    fun `the Home role decides when Android offers it`() {
        assertTrue(jarngreiprIsHome(roleHeld = true, resolvedHomePackage = "other", ownPackage = OWN))
        assertFalse(jarngreiprIsHome(roleHeld = false, resolvedHomePackage = OWN, ownPackage = OWN))
    }

    @Test
    fun `without the role the default Home resolution decides`() {
        assertTrue(jarngreiprIsHome(roleHeld = null, resolvedHomePackage = OWN, ownPackage = OWN))
        assertFalse(jarngreiprIsHome(roleHeld = null, resolvedHomePackage = "com.other.launcher", ownPackage = OWN))
    }

    @Test
    fun `no default Home set reads as not Home`() {
        assertFalse(jarngreiprIsHome(roleHeld = null, resolvedHomePackage = "android", ownPackage = OWN))
        assertFalse(jarngreiprIsHome(roleHeld = null, resolvedHomePackage = null, ownPackage = OWN))
    }

    @Test
    fun `each build checks its own package`() {
        assertFalse(jarngreiprIsHome(roleHeld = null, resolvedHomePackage = OWN, ownPackage = DEBUG))
        assertTrue(jarngreiprIsHome(roleHeld = null, resolvedHomePackage = DEBUG, ownPackage = DEBUG))
    }

    private companion object {
        const val OWN = "jr.brian.home"
        const val DEBUG = "jr.brian.home.debug"
    }
}
