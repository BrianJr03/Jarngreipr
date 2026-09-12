package jr.brian.home

import android.os.Bundle
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

        routeHomeFromLauncher(
            context = this,
            frontendEnabled = esdePreferencesManager.state.value.frontendEnabled,
        )

        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
