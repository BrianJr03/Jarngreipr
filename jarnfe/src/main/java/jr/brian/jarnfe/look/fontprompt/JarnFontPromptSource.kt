package jr.brian.jarnfe.look.fontprompt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import jr.brian.jarnfe.look.commandpanel.rememberCommandPanelFontOffer

/** The trigger for the one-time fonts prompt: the active Command Panel look and its sample type. */
@Composable
internal fun rememberJarnFontPromptInput(): JarnFontPromptInput {
    val offer by rememberCommandPanelFontOffer()
    return remember(offer) { JarnFontPromptInput(offer.availability, offer.sample) }
}
