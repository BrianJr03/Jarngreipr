package jr.brian.jarnfe.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import jr.brian.core.ui.focus.LocalJarnTabSwitchFocus
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.core.ui.theme.LocalJarnFocusFloatAllowed
import jr.brian.jarnfe.deck.DeckScreen
import jr.brian.jarnfe.deck.NowPlayingSlot
import jr.brian.jarnfe.firstrun.JarnFirstRunScreen
import jr.brian.jarnfe.firstrun.rememberJarnFirstRun
import jr.brian.jarnfe.library.JarnEsdeRomsNoticeHost
import jr.brian.jarnfe.settings.JarnEsdeImportNoticeHost
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.ProvideJarnInput
import jr.brian.jarnfe.look.JarnSwitchCuePreview
import jr.brian.jarnfe.look.fontprompt.JarnFontPromptHost
import jr.brian.jarnfe.look.rememberJarnAppearance
import jr.brian.jarnfe.look.rememberJarnDisplay
import jr.brian.jarnfe.session.JarnInput
import jr.brian.jarnfe.settings.switchFocus
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
    val display = rememberJarnDisplay()
    val savedCue = display.switchCue
    val switchCue = jarnShownCue(JarnSwitchCuePreview.cue ?: savedCue, state.settingsOnDeck)
    RememberDeckTab(state.deckTab)
    JarnTheme(
        look = appearance.look,
        reducedMotion = appearance.reducedMotion,
        screenActive = state.isLit(screen),
        switchCue = switchCue,
        type = appearance.type,
    ) {
        CompositionLocalProvider(
            LocalJarnFocusFloatAllowed provides rememberJarnFocusFloatAllowed(),
            LocalJarnMarkHandoff provides rememberJarnMarkHandoff(screen, switchCue, state),
            LocalJarnTabSwitchFocus provides display.tabFocus.switchFocus(),
        ) {
            ProvideJarnInput(JarnInput, screen) {
                val firstRun = rememberJarnFirstRun(state.stageRoute)
                val dimTarget = jarnDimTarget(switchCue, state.isLit(screen), state.settingsOnDeck)
                val dim = rememberJarnScreenDim(dimTarget)
                Box(Modifier.fillMaxSize().jarnScreenDim(dim).jarnScreenTag(screen)) {
                    when {
                        firstRun?.coversShell(state.stageRoute) == true -> JarnFirstRunScreen(screen, state, firstRun)
                        else -> NowPlayingSlot(screen, state) {
                            if (screen == JarnScreen.Stage) StageScreen(state) else DeckScreen(state)
                        }
                    }
                    JarnEsdeRomsNoticeHost(screen)
                    JarnEsdeImportNoticeHost(screen)
                    if (screen == JarnScreen.Deck && firstRun?.coversShell(state.stageRoute) != true) {
                        JarnFontPromptHost()
                    }
                }
            }
        }
    }
}

internal const val JARN_STAGE_TAG = "jarn_stage"
internal const val JARN_DECK_TAG = "jarn_deck"

/** Names the role in uiautomator dumps, since which header holds the Jarn label depends on the cue. */
@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.jarnScreenTag(screen: JarnScreen): Modifier = this
    .semantics { testTagsAsResourceId = true }
    .testTag(if (screen == JarnScreen.Stage) JARN_STAGE_TAG else JARN_DECK_TAG)
