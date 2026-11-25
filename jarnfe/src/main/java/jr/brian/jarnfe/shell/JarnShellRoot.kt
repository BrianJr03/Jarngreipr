package jr.brian.jarnfe.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.core.ui.theme.LocalJarnFocusFloatAllowed
import jr.brian.jarnfe.deck.DeckScreen
import jr.brian.jarnfe.firstrun.JarnFirstRunScreen
import jr.brian.jarnfe.firstrun.rememberJarnFirstRun
import jr.brian.jarnfe.library.JarnEsdeRomsNoticeHost
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.ProvideJarnInput
import jr.brian.jarnfe.look.JarnSwitchCuePreview
import jr.brian.jarnfe.look.rememberJarnAppearance
import jr.brian.jarnfe.look.rememberJarnDisplay
import jr.brian.jarnfe.session.JarnInput
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.RememberDeckTab
import jr.brian.jarnfe.stage.StageScreen
import jr.brian.jarnfe.transition.LocalJarnMarkHandoff
import jr.brian.jarnfe.transition.rememberJarnMarkHandoff
import jr.brian.jarnfe.wallpaper.rememberJarnFocusFloatAllowed

@Composable
internal fun JarnShellRoot(screen: JarnScreen) {
    val state = JarnSession.state
    val appearance = rememberJarnAppearance()
    val savedCue = rememberJarnDisplay().switchCue
    val switchCue = jarnShownCue(JarnSwitchCuePreview.cue ?: savedCue, state.settingsOnDeck)
    RememberDeckTab(state.deckTab)
    JarnTheme(
        look = appearance.look,
        reducedMotion = appearance.reducedMotion,
        screenActive = state.isLit(screen),
        switchCue = switchCue,
    ) {
        CompositionLocalProvider(
            LocalJarnFocusFloatAllowed provides rememberJarnFocusFloatAllowed(),
            LocalJarnMarkHandoff provides rememberJarnMarkHandoff(screen, switchCue, state),
        ) {
            ProvideJarnInput(JarnInput, screen) {
                val firstRun = rememberJarnFirstRun(state.stageRoute)
                val dimTarget = jarnDimTarget(switchCue, state.isLit(screen), state.settingsOnDeck)
                val dim = rememberJarnScreenDim(dimTarget)
                Box(Modifier.fillMaxSize().jarnScreenDim(dim)) {
                    when {
                        firstRun?.coversShell(state.stageRoute) == true -> JarnFirstRunScreen(screen, state, firstRun)
                        screen == JarnScreen.Stage -> StageScreen(state)
                        else -> DeckScreen(state)
                    }
                    JarnEsdeRomsNoticeHost(screen)
                }
            }
        }
    }
}
