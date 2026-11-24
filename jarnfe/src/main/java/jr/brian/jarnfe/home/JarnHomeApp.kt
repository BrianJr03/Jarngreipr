package jr.brian.jarnfe.home

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** Whether this app is Android's Home app. Only ever reads the role; the player picks it. */
interface JarnHomeApp {
    fun isHome(): Boolean

    /** Android's own Home picker, or its Home settings screen where the picker isn't offered. */
    fun pickerIntent(): Intent
}

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnHomeAppProvider {
    val jarnHomeApp: JarnHomeApp
}

fun Context.jarnHomeAppOrNull(): JarnHomeApp? = (applicationContext as? JarnHomeAppProvider)?.jarnHomeApp

/** [isHome] is rechecked each time the window resumes and when the picker returns. */
class JarnHomeAppState internal constructor(isHome: Boolean) {
    var isHome by mutableStateOf(isHome)
        internal set

    internal var open: () -> Unit = {}

    fun openPicker() = open()
}

/** Null when the host can't tell, so the caller shows nothing. */
@Composable
fun rememberJarnHomeAppState(): JarnHomeAppState? {
    val context = LocalContext.current
    val home = remember(context) { context.jarnHomeAppOrNull() } ?: return null
    val state = remember(home) { JarnHomeAppState(home.isHome()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        state.isHome = home.isHome()
    }
    SideEffect { state.open = { runCatching { launcher.launch(home.pickerIntent()) } } }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { state.isHome = home.isHome() }
    return state
}
