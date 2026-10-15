package jr.brian.frontend.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dagger.hilt.android.AndroidEntryPoint

/**
 * Entry point for the new frontend. Standalone-first per the plan
 * (works with no launcher running); paired-mode interactions arrive
 * in later phases via FrontendBus.
 *
 * The pane the frontend lives on draws edge-to-edge AND hides the
 * status + navigation bars entirely — the system chrome is noise on
 * a 10-foot game-browse surface. Swipe from the top edge still
 * reveals them transiently (via `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`),
 * matching the old `FrontEndActivity` so users who need a status
 * glance can still get one.
 */
@AndroidEntryPoint
class FrontendActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent { FrontendApp() }
    }
}
