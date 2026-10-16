package jr.brian.home.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import jr.brian.core.model.ScreenAssignment
import jr.brian.core.model.SwapCoordinator
import jr.brian.core.model.SwapPhase
import jr.brian.home.data.ScreenAssignmentStore

/**
 * The assignment the current composition should render *as if* it were on,
 * which during a swap is the destination assignment, not the currently
 * attached one.
 *
 * Why: display-aware layout picks (row ↔ grid per display, carousel vs
 * one-card-at-a-time spacing) used to key off `LocalView.current.display`,
 * which only updates when Android reparents the activity to the new
 * display — that happens *after* `routeHomeFromLauncher` fires its
 * startActivity IPC, i.e. mid fade-in. The expensive LazyRow(10_000)
 * teardown then stalls the fade-in on both screens.
 *
 * Driving layout off [SwapCoordinator.phase] instead means composition
 * flips the instant `beginLeaving(next)` fires, under the alpha=0 veil
 * of the fade-out, so by the time the activity actually reattaches the
 * composition is already settled.
 */
@Composable
fun rememberEffectiveAssignment(): ScreenAssignment {
    val context = LocalContext.current
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            EffectiveAssignmentEntryPoint::class.java,
        )
    }
    val phase by entryPoint.swapCoordinator().phase.collectAsStateWithLifecycle()
    val current by entryPoint.screenAssignmentStore().state.collectAsStateWithLifecycle()
    return when (val p = phase) {
        is SwapPhase.Leaving -> p.targetAssignment
        is SwapPhase.Arriving -> p.targetAssignment
        SwapPhase.Idle -> current
    }
}

/**
 * The assignment that reflects where each activity is *physically
 * rendered right now*, which during a swap is the pre-reparent
 * position while leaving and the post-reparent position while
 * arriving. Different semantics from [rememberEffectiveAssignment],
 * which eagerly reports the destination so layout can settle under
 * the fade-out.
 *
 * Use this for animations that depend on the activity's actual
 * on-screen display (e.g. direction of a swap slide), not for layout
 * decisions.
 */
@Composable
fun rememberPhysicalAssignment(): ScreenAssignment {
    val context = LocalContext.current
    val entryPoint = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            EffectiveAssignmentEntryPoint::class.java,
        )
    }
    val phase by entryPoint.swapCoordinator().phase.collectAsStateWithLifecycle()
    val current by entryPoint.screenAssignmentStore().state.collectAsStateWithLifecycle()
    return when (val p = phase) {
        is SwapPhase.Leaving -> current
        is SwapPhase.Arriving -> p.targetAssignment
        SwapPhase.Idle -> current
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface EffectiveAssignmentEntryPoint {
    fun swapCoordinator(): SwapCoordinator
    fun screenAssignmentStore(): ScreenAssignmentStore
}
