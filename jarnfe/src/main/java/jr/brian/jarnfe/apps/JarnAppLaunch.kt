package jr.brian.jarnfe.apps

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import jr.brian.jarnfe.canvas.CanvasDisplayChoiceSheet
import jr.brian.jarnfe.canvas.findActivity

@Composable
internal fun rememberJarnAppsCommands(data: JarnAppsData): JarnAppsCommands {
    val host = LocalActivity.current
    return remember(data, host) {
        JarnAppsCommands(
            open = { packageName -> host?.let { data.launchApp(it, packageName) } },
            options = data,
        )
    }
}

/** What A on an app does, in the Apps drawer and in Search: [ask] for a screen when the app wants that, else open it. */
internal fun JarnAppsCommands.launchOrAsk(packageName: String, ask: (String) -> Unit) {
    if (options.appOptions(packageName).shouldAskDisplay()) ask(packageName) else open(packageName)
}

/** Which screen to open [packageName] on. [onClose] runs before the launch, and on cancel. */
@Composable
internal fun JarnAppDisplayAsk(packageName: String, commands: JarnAppsCommands, onClose: () -> Unit) {
    val activity = LocalContext.current.findActivity()
    CanvasDisplayChoiceSheet(
        onPick = { display ->
            onClose()
            activity?.let { commands.options.launchAppOn(it, packageName, display) }
        },
        onClose = onClose,
    )
}
