package jr.brian.home.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import jr.brian.core.model.ScreenAssignment
import jr.brian.core.model.SwapCoordinator
import jr.brian.home.R
import jr.brian.home.data.ScreenAssignmentStore
import jr.brian.home.data.ScreenFocusLockWriter
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.ui.components.SWAP_FADE_MS
import jr.brian.home.ui.util.routeHomeFromLauncher
import jr.brian.home.util.ThorDetection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Launcher-side swap trigger. Flips [ScreenAssignmentStore] and re-fires
 * the two-activity launch plan via [routeHomeFromLauncher] so the new
 * assignment lands immediately. Mirrors what the (shelved) frontend
 * SettingsScreen does, minus the bus hop — we're already in :app and
 * can reach the store directly.
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
    val coordinator = entryPoint.swapCoordinator()
    val assignment by store.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

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
        onClick = {
            // Start the Leaving fade first, wait a full fade duration
            // before flipping the pref + routing. That means the row ↔
            // grid layout change happens while both activities are at
            // alpha 0, so the user never sees the layout transition
            // in-frame. Matches [ScreenSwapListener] / the L3/R3
            // shortcut, so every swap trigger animates the same way.
            val next = store.assignment.flipped()
            coordinator.beginLeaving(next)
            scope.launch {
                delay(SWAP_FADE_MS.toLong())
                store.setAssignment(next)
                ScreenFocusLockWriter.syncToAssignment(context, next)
                routeHomeFromLauncher(context, frontendEnabled = true)
                coordinator.markArriving()
            }
        },
    )
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ScreenSwapEntryPoint {
    fun screenAssignmentStore(): ScreenAssignmentStore
    fun swapCoordinator(): SwapCoordinator
}
