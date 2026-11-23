package jr.brian.home

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.ui.util.routeHomeFromLauncher
import javax.inject.Inject

/**
 * Exported HOME / LAUNCHER entry. Fired by the system when Jarngreipr is the
 * default home, and by external launchers (e.g. Mjolnir on the AYN Thor) that
 * bind a gesture to Jarngreipr on a specific display.
 *
 * Delegates to [routeHomeFromLauncher] — which preserves the caller's display
 * when the frontend is disabled — rather than [jr.brian.home.ui.util.routeHome].
 * The stored `HomeTarget` / `MainScreen` selectors govern the hardware Home
 * key interception path only ([jr.brian.home.service.HomeInterceptorService]);
 * applying them here would silently override whatever display the external
 * launcher asked for.
 */
@AndroidEntryPoint
class HomeRouterActivity : ComponentActivity() {

    @Inject
    lateinit var esdePreferencesManager: ESDEPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!openJarnFeInDebug()) {
            routeHomeFromLauncher(
                context = this,
                frontendEnabled = esdePreferencesManager.state.value.frontendEnabled,
            )
        }

        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    /** Debug builds open Jarn FE by default. Pass [EXTRA_CLASSIC] to reach Classic. */
    private fun openJarnFeInDebug(): Boolean {
        if (!BuildConfig.DEBUG || intent.getBooleanExtra(EXTRA_CLASSIC, false)) return false
        val jarnFe = Intent()
            .setClassName(this, JARN_FE_ACTIVITY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY)
        return runCatching { startActivity(jarnFe, options.toBundle()) }.isSuccess
    }

    private companion object {
        const val EXTRA_CLASSIC = "classic"
        const val JARN_FE_ACTIVITY = "jr.brian.jarnfe.shell.JarnStageActivity"
    }
}
