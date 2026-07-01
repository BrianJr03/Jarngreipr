package jr.brian.home

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.ui.FrontEndActivity
import jr.brian.home.ui.util.launchFrontend
import jr.brian.home.ui.util.resolveBottomDisplayId
import javax.inject.Inject

@AndroidEntryPoint
class HomeRouterActivity : ComponentActivity() {

    @Inject
    lateinit var esdePreferencesManager: ESDEPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bottomId = resolveBottomDisplayId(this)
        val frontendOn = esdePreferencesManager.state.value.frontendEnabled

        if (bottomId == null || !frontendOn) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } else {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                ActivityOptions.makeBasic()
                    .apply { launchDisplayId = bottomId }
                    .toBundle()
            )
            if (!FrontEndActivity.isRunning) launchFrontend(this)
        }

        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
