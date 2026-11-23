package jr.brian.jarnfe.look.commandpanel

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import jr.brian.core.model.CommandPanelPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val NoCommandPanelPalette: StateFlow<CommandPanelPalette?> = MutableStateFlow(null)

internal object CommandPanelThemeReader {
    private var controller: CommandPanelThemeController? = null

    fun controller(context: Context): CommandPanelThemeController {
        val appContext = context.applicationContext
        val current = controller ?: CommandPanelThemeController(
            port = AndroidCommandPanelThemePort(appContext),
            store = CommandPanelPaletteCache(appContext.noBackupFilesDir),
            lane = Dispatchers.IO.limitedParallelism(1),
        ).also { controller = it }
        current.seedFromCache()
        return current
    }
}

/**
 * The Command Panel palette to match, or null to use the saved Jarn look.
 * While [enabled] is false this touches neither Command Panel nor the cache.
 */
@Composable
internal fun rememberCommandPanelPalette(enabled: Boolean): State<CommandPanelPalette?> {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val controller = remember(enabled, context) {
        if (enabled) CommandPanelThemeReader.controller(context) else null
    }
    DisposableEffect(controller, owner) {
        val binding = controller?.let(::CommandPanelLifecycleBinding)
        binding?.let(owner.lifecycle::addObserver)
        onDispose {
            binding?.let(owner.lifecycle::removeObserver)
            binding?.release()
        }
    }
    return key(controller) { (controller?.palette ?: NoCommandPanelPalette).collectAsState() }
}

internal class CommandPanelLifecycleBinding(
    private val controller: CommandPanelThemeController,
) : LifecycleEventObserver {
    private var isStarted = false

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> if (!isStarted) {
                isStarted = true
                controller.start()
            }
            Lifecycle.Event.ON_RESUME -> controller.refresh()
            Lifecycle.Event.ON_STOP -> release()
            else -> Unit
        }
    }

    fun release() {
        if (!isStarted) return
        isStarted = false
        controller.stop()
    }
}
