package jr.brian.home.ui.util

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import jr.brian.home.esde.ui.FrontEndActivity
import jr.brian.home.esde.ui.RomSearchResultsActivity

/**
 * Display the Thor treats as primary — the top screen — and the target of every
 * gameplay/emulator launch. Pass to [android.app.ActivityOptions.setLaunchDisplayId].
 */
const val PRIMARY_DISPLAY_ID = 0

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
 * Fires the standalone ROM search activity (original pre-frontend search experience).
 * Used as a search affordance from within the frontend.
 */
fun launchRomSearchActivity(context: Context) {
    val intent = Intent(context, RomSearchResultsActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
    }
    val options = ActivityOptions.makeBasic().apply { launchDisplayId = PRIMARY_DISPLAY_ID }
    context.startActivity(intent, options.toBundle())
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
