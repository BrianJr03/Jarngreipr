package jr.brian.jarnfe.look.fontprompt

import androidx.compose.runtime.Composable
import jr.brian.jarnfe.look.commandpanel.FontMatchAvailability

private val NoFontsDetected = JarnFontPromptInput(FontMatchAvailability.Hidden)

/**
 * The trigger seam for the one-time fonts prompt. Until the Command Panel fonts reader (#449)
 * lands, Jarn detects no font look, so the prompt never shows. The reader replaces this body
 * with `Available(lookName)` and `jarnTypeWithFonts(...)` for that look.
 */
@Composable
internal fun rememberJarnFontPromptInput(): JarnFontPromptInput = NoFontsDetected
