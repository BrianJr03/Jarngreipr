package jr.brian.jarnfe.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import jr.brian.frontend.look.JarnTapControl
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.look.rememberJarnDisplay
import jr.brian.jarnfe.session.JarnInput
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.onModalChanged
import jr.brian.jarnfe.session.releasingTapHold

@Composable
internal fun JarnTapControlEffects(role: JarnScreen) {
    val modalOpen = JarnInput.hasModalLayer(role)
    val choice = rememberJarnDisplay().tapControl
    LaunchedEffect(role, modalOpen) { JarnSession.update { onModalChanged(role, modalOpen) } }
    LaunchedEffect(choice) {
        if (choice == JarnTapControl.FollowTap) JarnSession.update { releasingTapHold() }
    }
}
