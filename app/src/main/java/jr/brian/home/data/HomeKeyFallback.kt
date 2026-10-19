package jr.brian.home.data

import android.content.Context
import android.view.KeyEvent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import jr.brian.core.model.HomeTarget
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.ui.util.routeHome

/**
 * Activity-side safety net for the hardware Home button.
 *
 * The accessibility-based [jr.brian.home.service.HomeInterceptorService] is
 * the primary interception path and the only one that implements the
 * single / double / long gesture state machine. On Thor it is enough most
 * of the time because the firmware surfaces `KEYCODE_HOME` with
 * `scanCode == 102` from the primary input chain. However, when the
 * frontend lives on the external display and the user presses Home from
 * that side, the service often does not see the event at all. Android
 * then falls through to its default handler and the user ends up on
 * whichever activity is registered as `SECONDARY_HOME` (currently the
 * stock Thor launcher), which reads as "the Home button kicked me out
 * of the app."
 *
 * [handle] runs *after* the accessibility path — if the service already
 * consumed the event it never reaches `dispatchKeyEvent`. When we do see
 * it here, we treat it as a plain single-press and route the user's
 * bottom slot back on screen via [HomeTarget.BOTTOM] — the planner
 * resolves "bottom" to whichever activity currently lives there under
 * the active [jr.brian.core.model.ScreenAssignment]. That keeps the user
 * inside Jarngreipr on the swapped layout; the richer gestures still
 * require the accessibility path.
 *
 * No-op when "Home Button Interception" is off: disabled users get the
 * system's default Home behaviour everywhere.
 */
object HomeKeyFallback {

    fun handle(context: Context, event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HOME) return false
        val entry = runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                HomeFallbackEntryPoint::class.java,
            )
        }.getOrNull() ?: return false
        if (!entry.homeButtonManager().interceptionEnabled.value) return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            routeHome(
                context = context,
                target = HomeTarget.BOTTOM,
                mainScreen = entry.homeButtonManager().mainScreen.value,
                frontendEnabled = entry.esdePreferencesManager().state.value.frontendEnabled,
            )
        }
        return true
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface HomeFallbackEntryPoint {
        fun homeButtonManager(): HomeButtonManager
        fun esdePreferencesManager(): ESDEPreferencesManager
    }
}
