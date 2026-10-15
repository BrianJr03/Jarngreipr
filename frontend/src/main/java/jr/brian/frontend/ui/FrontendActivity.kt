package jr.brian.frontend.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint

/**
 * Entry point for the new frontend. Standalone-first per the plan
 * (works with no launcher running); paired-mode interactions arrive
 * in later phases via FrontendBus.
 *
 * Window configuration mirrors the old `FrontEndActivity` enough that
 * both can coexist behind the "Legacy frontend" toggle. The pane the
 * frontend lives on hides its system bars edge-to-edge — standard
 * for a game-browse surface.
 */
@AndroidEntryPoint
class FrontendActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { FrontendApp() }
    }
}
