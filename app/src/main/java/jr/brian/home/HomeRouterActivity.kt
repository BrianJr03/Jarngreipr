package jr.brian.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.data.HomeButtonManager
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.ui.util.routeHome
import javax.inject.Inject

@AndroidEntryPoint
class HomeRouterActivity : ComponentActivity() {

    @Inject
    lateinit var esdePreferencesManager: ESDEPreferencesManager

    @Inject
    lateinit var homeButtonManager: HomeButtonManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val frontendEnabled = esdePreferencesManager.state.value.frontendEnabled
        routeHome(
            context = this,
            target = homeButtonManager.resolveHomeTarget(frontendEnabled),
            mainScreen = homeButtonManager.mainScreen.value,
            frontendEnabled = frontendEnabled,
        )

        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
