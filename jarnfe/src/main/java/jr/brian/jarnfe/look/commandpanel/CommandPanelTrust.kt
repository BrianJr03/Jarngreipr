package jr.brian.jarnfe.look.commandpanel

internal data class CommandPanelProviderIdentity(
    val packageName: String,
    val signerDigests: List<String>,
    val isStopped: Boolean,
)

internal enum class CommandPanelTrust { Trusted, Stopped, Untrusted }

/** Every current signer must match the pin, so an added signer can't borrow Command Panel's identity. */
internal fun decideCommandPanelTrust(identity: CommandPanelProviderIdentity?): CommandPanelTrust {
    if (identity == null) return CommandPanelTrust.Untrusted
    if (identity.packageName != CommandPanelThemeContract.PACKAGE) return CommandPanelTrust.Untrusted
    if (identity.signerDigests.isEmpty()) return CommandPanelTrust.Untrusted
    if (identity.signerDigests.any { it != CommandPanelThemeContract.CERT_SHA256 }) {
        return CommandPanelTrust.Untrusted
    }
    return if (identity.isStopped) CommandPanelTrust.Stopped else CommandPanelTrust.Trusted
}
