package jr.brian.jarnfe.look

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import jr.brian.core.model.CommandPanelPalette
import jr.brian.core.model.theme.ThemeColors
import jr.brian.core.ui.theme.JarnLook
import jr.brian.core.ui.theme.JarnLooks
import jr.brian.core.ui.theme.JarnSwitchCue
import jr.brian.core.ui.theme.JarnType
import jr.brian.core.ui.theme.adaptCommandPanelPalette
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.frontend.look.JarnLookStore
import jr.brian.jarnfe.look.commandpanel.rememberCommandPanelFontType
import jr.brian.jarnfe.themes.rememberJarnActiveTheme
import jr.brian.jarnfe.themes.withThemeColors
import jr.brian.jarnfe.look.commandpanel.rememberCommandPanelPalette

interface JarnLookStoreProvider {
    val jarnLookStore: JarnLookStore
}

internal fun Context.jarnLookStoreOrNull(): JarnLookStore? =
    (applicationContext as? JarnLookStoreProvider)?.jarnLookStore

/** An unsaved look shown on both screens while the picker has focus. */
internal object JarnLookPreview {
    var lookId by mutableStateOf<String?>(null)
}

/** Command Panel's fonts shown on both screens while the fonts row has focus, before the toggle is on. */
internal object JarnFontPreview {
    var active by mutableStateOf(false)
}

/** An unsaved cue, so X can play whichever choice is highlighted in the tour. */
internal object JarnSwitchCuePreview {
    var cue by mutableStateOf<JarnSwitchCue?>(null)
}

@Immutable
internal data class JarnAppearance(
    val look: JarnLook,
    val reducedMotion: Boolean,
    /** Null keeps Jarn's own type. */
    val type: JarnType? = null,
)

/**
 * A live preview wins, then a theme's own [themeColors], then the Command Panel match, then the
 * saved look. A theme without its own colors passes null and follows the Look.
 */
internal fun resolveJarnLook(
    savedId: String?,
    previewId: String?,
    palette: CommandPanelPalette? = null,
    themeColors: ThemeColors? = null,
): JarnLook = previewId?.let { JarnLooks.fromId(it) }
    ?: themeColors?.let { JarnLooks.fromId(savedId).withThemeColors(it) }
    ?: adaptCommandPanelPalette(palette, JarnLooks.fromId(savedId)).look

@Composable
internal fun rememberJarnAppearance(): JarnAppearance {
    val context = LocalContext.current
    val store = remember(context) { context.jarnLookStoreOrNull() }
    val savedId = store?.lookIdState?.collectAsState()?.value
    val reducedMotion = store?.reducedMotionState?.collectAsState()?.value ?: false
    val matching = store?.matchCommandPanelState?.collectAsState()?.value ?: false
    val palette by rememberCommandPanelPalette(matching)
    val previewId = JarnLookPreview.lookId
    val themeColors = rememberJarnActiveTheme().ownColors
    val look = remember(savedId, previewId, palette, themeColors) { resolveJarnLook(savedId, previewId, palette, themeColors) }
    val matchingFonts = store?.matchCommandPanelFontsState?.collectAsState()?.value ?: false
    val previewFonts = JarnFontPreview.active
    val type = rememberCommandPanelFontType(
        enabled = matchingFonts || previewFonts,
        forgetWhenOff = store != null && !matchingFonts && !previewFonts,
    )
    return JarnAppearance(look, reducedMotion, type)
}

@Composable
internal fun rememberJarnDisplay(): JarnDisplayPrefs {
    val context = LocalContext.current
    val store = remember(context) { context.jarnLookStoreOrNull() }
    return store?.displayState?.collectAsState()?.value ?: JarnDisplayPrefs()
}
