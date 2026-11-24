package jr.brian.jarnfe.look.commandpanel

import org.junit.Assert.assertEquals
import org.junit.Test

class CommandPanelTrustTest {
    private val pin = CommandPanelThemeContract.CERT_SHA256

    private fun identity(
        packageName: String = CommandPanelThemeContract.PACKAGE,
        signers: List<String> = listOf(pin),
        isStopped: Boolean = false,
    ) = CommandPanelProviderIdentity(packageName, signers, isStopped)

    @Test
    fun `pinned release package is trusted`() {
        assertEquals(CommandPanelTrust.Trusted, decideCommandPanelTrust(identity()))
    }

    @Test
    fun `missing provider is untrusted`() {
        assertEquals(CommandPanelTrust.Untrusted, decideCommandPanelTrust(null))
    }

    @Test
    fun `debug and lookalike packages are untrusted even with the pinned signer`() {
        listOf("jr.brian.commandpannel.debug", "jr.brian.commandpanel", "evil.app").forEach {
            assertEquals(CommandPanelTrust.Untrusted, decideCommandPanelTrust(identity(packageName = it)))
        }
    }

    @Test
    fun `wrong, missing or extra signers are untrusted`() {
        assertEquals(CommandPanelTrust.Untrusted, decideCommandPanelTrust(identity(signers = listOf("00"))))
        assertEquals(CommandPanelTrust.Untrusted, decideCommandPanelTrust(identity(signers = emptyList())))
        assertEquals(CommandPanelTrust.Untrusted, decideCommandPanelTrust(identity(signers = listOf(pin, "00"))))
    }

    @Test
    fun `force stopped package is reported as stopped`() {
        assertEquals(CommandPanelTrust.Stopped, decideCommandPanelTrust(identity(isStopped = true)))
    }

    @Test
    fun `an impostor that is stopped is still untrusted`() {
        val impostor = identity(signers = listOf("00"), isStopped = true)
        assertEquals(CommandPanelTrust.Untrusted, decideCommandPanelTrust(impostor))
    }
}
