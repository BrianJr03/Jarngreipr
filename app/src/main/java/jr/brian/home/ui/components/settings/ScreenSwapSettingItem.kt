package jr.brian.home.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import jr.brian.core.model.ScreenAssignment
import jr.brian.home.R
import jr.brian.home.data.ScreenAssignmentStore
import jr.brian.home.data.ScreenSwapOrchestrator
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.util.ThorDetection

/**
 * Launcher-side swap trigger. Delegates to [ScreenSwapOrchestrator],
 * same path as the L3/R3 shortcut and the frontend's bus command, so
 * every trigger runs the same choreography.
 *
 * Hidden on single-display hardware: [ScreenAssignment] collapses to
 * a no-op when there's only one display, so surfacing it would just
 * confuse.
 */
@Composable
fun ScreenSwapSettingItem() {
    if (!ThorDetection.isThor()) return

    val context = LocalContext.current
    val entryPoint = remember(context) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, ScreenSwapEntryPoint::class.java)
    }
    val store = entryPoint.screenAssignmentStore()
    val orchestrator = entryPoint.screenSwapOrchestrator()
    val assignment by store.state.collectAsStateWithLifecycle()

    val description = when (assignment) {
        ScreenAssignment.FRONTEND_ON_TOP ->
            stringResource(R.string.screen_swap_description_top)
        ScreenAssignment.FRONTEND_ON_BOTTOM ->
            stringResource(R.string.screen_swap_description_bottom)
    }

    ToggleSetting(
        title = stringResource(R.string.screen_swap_title),
        description = description,
        checked = false,
        showToggle = false,
        onClick = { orchestrator.requestSwap() },
    )
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ScreenSwapEntryPoint {
    fun screenAssignmentStore(): ScreenAssignmentStore
    fun screenSwapOrchestrator(): ScreenSwapOrchestrator
}
