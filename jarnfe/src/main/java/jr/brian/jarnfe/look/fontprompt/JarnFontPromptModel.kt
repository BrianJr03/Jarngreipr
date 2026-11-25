package jr.brian.jarnfe.look.fontprompt

import androidx.compose.runtime.Immutable
import jr.brian.core.ui.theme.JarnType
import jr.brian.jarnfe.look.commandpanel.FontMatchAvailability

/** What the fonts reader knows right now: the active look and how Jarn's text would look in its fonts. */
@Immutable
internal data class JarnFontPromptInput(
    val availability: FontMatchAvailability,
    val sampleType: JarnType? = null,
)

@Immutable
internal data class JarnFontPromptOffer(
    val lookName: String,
    val sampleType: JarnType,
)

/**
 * The prompt shows once, ever. After it is answered ([shown]), no later look, font change or
 * restore brings it back. It also waits for the tour, and stays away while fonts are already on.
 */
internal fun resolveFontPromptOffer(
    shown: Boolean,
    fontsOn: Boolean,
    tourDone: Boolean,
    input: JarnFontPromptInput,
): JarnFontPromptOffer? {
    if (shown || fontsOn || !tourDone) return null
    val available = input.availability as? FontMatchAvailability.Available ?: return null
    val lookName = available.lookName ?: return null
    val sample = input.sampleType ?: return null
    return JarnFontPromptOffer(lookName, sample)
}
