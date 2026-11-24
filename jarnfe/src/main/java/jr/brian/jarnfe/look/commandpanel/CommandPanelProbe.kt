package jr.brian.jarnfe.look.commandpanel

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import jr.brian.core.model.CommandPanelPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal enum class CommandPanelAvailability { Checking, Ready, Waiting, Missing, Untrusted, BadData }

@Immutable
internal data class CommandPanelProbe(
    val availability: CommandPanelAvailability,
    val palette: CommandPanelPalette? = null,
)

/** A provider that doesn't resolve is missing or too old. One that resolves but fails trust is an impostor. */
internal fun CommandPanelRead.toProbe(providerExists: Boolean): CommandPanelProbe = when (this) {
    is CommandPanelRead.Fresh -> CommandPanelProbe(CommandPanelAvailability.Ready, palette)
    CommandPanelRead.Locked,
    CommandPanelRead.Stopped,
    CommandPanelRead.Unavailable,
    -> CommandPanelProbe(CommandPanelAvailability.Waiting)
    CommandPanelRead.Untrusted -> CommandPanelProbe(
        if (providerExists) CommandPanelAvailability.Untrusted else CommandPanelAvailability.Missing,
    )
    CommandPanelRead.Rejected -> CommandPanelProbe(CommandPanelAvailability.BadData)
}

/**
 * Status for the Look settings card only, read once per resume while the card is on screen.
 * Never caches or observes, and the port won't wake a force-stopped Command Panel.
 */
@Composable
internal fun rememberCommandPanelProbe(): State<CommandPanelProbe> {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val probe = remember { mutableStateOf(CommandPanelProbe(CommandPanelAvailability.Checking)) }
    LaunchedEffect(context, owner) {
        val appContext = context.applicationContext
        val port = AndroidCommandPanelThemePort(appContext)
        owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            probe.value = withContext(Dispatchers.IO) {
                port.read().toProbe(appContext.hasCommandPanelProvider())
            }
        }
    }
    return probe
}

private fun Context.hasCommandPanelProvider(): Boolean = runCatching {
    packageManager.resolveContentProvider(
        CommandPanelThemeContract.AUTHORITY,
        PackageManager.ComponentInfoFlags.of(0),
    ) != null
}.getOrDefault(false)
