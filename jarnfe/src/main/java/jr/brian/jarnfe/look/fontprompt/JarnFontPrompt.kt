package jr.brian.jarnfe.look.fontprompt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnSheet
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.core.ui.theme.JarnType
import jr.brian.frontend.look.JarnLookStore
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.look.jarnLookStoreOrNull
import jr.brian.jarnfe.session.JarnSession

/** Hosted on the Deck. Takes control while it shows, and gives it back once answered. */
@Composable
internal fun JarnFontPromptHost() {
    val context = LocalContext.current
    val store = remember(context) { context.jarnLookStoreOrNull() } ?: return
    val offer = rememberJarnFontPromptOffer(store) ?: return
    val returnTo = remember(offer) { JarnSession.state.activeScreen }
    LaunchedEffect(offer) { JarnSession.update { activate(JarnScreen.Deck) } }
    fun answer(useFonts: Boolean) {
        if (useFonts) store.setMatchCommandPanelFonts(true)
        store.markFontsPromptShown()
        JarnSession.update { activate(returnTo) }
    }
    JarnFontPrompt(offer, onUseFonts = { answer(true) }, onNotNow = { answer(false) })
}

@Composable
private fun rememberJarnFontPromptOffer(store: JarnLookStore): JarnFontPromptOffer? {
    val shown by store.fontsPromptShownState.collectAsState()
    val fontsOn by store.matchCommandPanelFontsState.collectAsState()
    val tourDone by store.firstRunDoneState.collectAsState()
    val input = rememberJarnFontPromptInput()
    return remember(shown, fontsOn, tourDone, input) { resolveFontPromptOffer(shown, fontsOn, tourDone, input) }
}

@Composable
internal fun JarnFontPrompt(offer: JarnFontPromptOffer, onUseFonts: () -> Unit, onNotNow: () -> Unit) {
    val useFocus = remember { FocusRequester() }
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
            on(JarnAction.Back, R.string.jarnfe_font_prompt_not_now, onPress = onNotNow)
        },
    ) {
        JarnSheet(
            visible = true,
            onDismiss = onNotNow,
            title = stringResource(R.string.jarnfe_font_prompt_title, offer.lookName),
            initialFocus = useFocus,
        ) {
            JarnText(stringResource(R.string.jarnfe_font_prompt_body), JarnTheme.type.body, JarnTheme.colors.textMid)
            JarnFontPromptSamples(offer)
            JarnFontPromptActions(useFocus, onUseFonts, onNotNow)
            JarnText(stringResource(R.string.jarnfe_font_prompt_footer), JarnTheme.baseType.caption, JarnTheme.colors.textLow)
        }
    }
}

@Composable
private fun JarnFontPromptSamples(offer: JarnFontPromptOffer) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md)) {
        JarnFontSampleCard(stringResource(R.string.jarnfe_font_prompt_jarn_now), JarnTheme.baseType)
        JarnFontSampleCard(stringResource(R.string.jarnfe_font_prompt_with_look, offer.lookName), offer.sampleType)
    }
}

@Composable
private fun RowScope.JarnFontSampleCard(caption: String, type: JarnType) {
    Column(
        modifier = Modifier
            .weight(1f)
            .background(JarnTheme.colors.bg, JarnTheme.radius.mdShape)
            .padding(JarnTheme.space.md),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
    ) {
        JarnText(caption, JarnTheme.baseType.caption, JarnTheme.colors.textLow, maxLines = 1)
        JarnText(stringResource(R.string.jarnfe_font_prompt_sample_title), type.title, JarnTheme.colors.textHigh, maxLines = 1)
        JarnText(stringResource(R.string.jarnfe_font_prompt_sample_label), type.label, JarnTheme.colors.textMid, maxLines = 1)
    }
}

@Composable
private fun JarnFontPromptActions(useFocus: FocusRequester, onUseFonts: () -> Unit, onNotNow: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md)) {
        JarnButton(
            label = stringResource(R.string.jarnfe_font_prompt_use),
            onClick = onUseFonts,
            modifier = Modifier.weight(1f).focusRequester(useFocus),
        )
        JarnButton(
            label = stringResource(R.string.jarnfe_font_prompt_not_now),
            onClick = onNotNow,
            modifier = Modifier.weight(1f),
            variant = JarnButtonVariant.Secondary,
        )
    }
}
