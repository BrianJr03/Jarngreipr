package jr.brian.home.ui.util

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import jr.brian.home.MainActivity
import jr.brian.home.esde.ui.FrontEndActivity

/**
 * Display the Thor treats as primary — the top screen — and the target of every
 * gameplay/emulator launch. Pass to [android.app.ActivityOptions.setLaunchDisplayId].
 */
const val PRIMARY_DISPLAY_ID = 0

/**
 * Returns the display id the bottom screen lives on (anything other than
 * [PRIMARY_DISPLAY_ID]), or `null` when the device only has a single display.
 */
fun resolveBottomDisplayId(context: Context): Int? {
    val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    return displayManager.displays
        .firstOrNull { it.displayId != PRIMARY_DISPLAY_ID }
        ?.displayId
}

/**
 * Fires [FrontEndActivity] (Systems → Games browse experience). Always targets the
 * top display.
 */
fun launchFrontend(context: Context) {
    val intent = Intent(context, FrontEndActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
    }
    val options = ActivityOptions.makeBasic().apply { launchDisplayId = PRIMARY_DISPLAY_ID }
    context.startActivity(intent, options.toBundle())
}

/**
 * Brings [MainActivity] to the front on the external/bottom display when one exists,
 * falling back to a plain launch on the primary display otherwise. When [frontendEnabled]
 * and a bottom display is present, also fires [FrontEndActivity] on the top display
 * (unless it is already running).
 *
 * Callable from any [Context] — Activity, Service, or Application — because it only
 * uses [Intent.FLAG_ACTIVITY_NEW_TASK] and never touches Activity-only APIs.
 */
fun routeHome(context: Context, frontendEnabled: Boolean) {
    val bottomId = resolveBottomDisplayId(context)
    if (bottomId == null || !frontendEnabled) {
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return
    }

    context.startActivity(
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
        ActivityOptions.makeBasic()
            .apply { launchDisplayId = bottomId }
            .toBundle()
    )
    if (!FrontEndActivity.isRunning) launchFrontend(context)
}

/**
 * Checks if the device has an external display connected.
 * @return true if there are multiple displays (including external), false otherwise
 */
@Composable
fun rememberHasExternalDisplay(): Boolean {
    val context = LocalContext.current
    return remember {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.displays.size > 1
    }
}
