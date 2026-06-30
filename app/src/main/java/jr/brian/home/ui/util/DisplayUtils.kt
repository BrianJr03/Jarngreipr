package jr.brian.home.ui.util

import android.content.Context
import android.hardware.display.DisplayManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Display the Thor treats as primary — the top screen — and the target of every
 * gameplay/emulator launch. Pass to [android.app.ActivityOptions.setLaunchDisplayId].
 */
const val PRIMARY_DISPLAY_ID = 0

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
