package jr.brian.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.core.model.Experience
import jr.brian.core.model.resolveLauncherExperience
import jr.brian.home.data.ExperienceStore
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.ui.util.launchJarnFe
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

    @Inject
    lateinit var experienceStore: Lazy<ExperienceStore>

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

    /**
     * Debug builds follow the saved experience and open Jarn FE when none is saved. Release builds
     * return before reading it. [EXTRA_CLASSIC] reaches Classic once without changing the choice.
     */
    private fun openJarnFeInDebug(): Boolean {
        if (!BuildConfig.DEBUG) return false
        val store = experienceStore.get()
        val choice = resolveLauncherExperience(
            jarnAvailable = true,
            stored = store.storedExperience,
            forceClassic = intent.getBooleanExtra(EXTRA_CLASSIC, false),
        )
        if (choice != Experience.JarnFE || !launchJarnFe(this)) return false
        store.adoptIfUnset(Experience.JarnFE)
        return true
    }

    private companion object {
        const val EXTRA_CLASSIC = "classic"
    }
}
