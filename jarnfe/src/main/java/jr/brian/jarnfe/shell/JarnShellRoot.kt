package jr.brian.jarnfe.shell

import androidx.compose.runtime.Composable
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.deck.DeckScreen
import jr.brian.jarnfe.firstrun.JarnFirstRunScreen
import jr.brian.jarnfe.firstrun.rememberJarnFirstRun
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.ProvideJarnInput
import jr.brian.jarnfe.look.rememberJarnAppearance
import jr.brian.jarnfe.session.JarnInput
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.stage.StageScreen

@Composable
internal fun JarnShellRoot(screen: JarnScreen) {
    val state = JarnSession.state
    val appearance = rememberJarnAppearance()
    JarnTheme(
        look = appearance.look,
        reducedMotion = appearance.reducedMotion,
        screenActive = state.isLit(screen),
    ) {
        ProvideJarnInput(JarnInput, screen) {
            val firstRun = rememberJarnFirstRun(state.stageRoute)
            when {
                firstRun?.coversShell(state.stageRoute) == true -> JarnFirstRunScreen(screen, state, firstRun)
                screen == JarnScreen.Stage -> StageScreen(state)
                else -> DeckScreen(state)
            }
        }
    }
}
