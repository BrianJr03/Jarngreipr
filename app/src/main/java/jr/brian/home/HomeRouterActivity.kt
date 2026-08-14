package jr.brian.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.ui.util.routeHome
import javax.inject.Inject

@AndroidEntryPoint
class HomeRouterActivity : ComponentActivity() {

    @Inject
    lateinit var esdePreferencesManager: ESDEPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        routeHome(
            context = this,
            frontendEnabled = esdePreferencesManager.state.value.frontendEnabled
        )

        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
