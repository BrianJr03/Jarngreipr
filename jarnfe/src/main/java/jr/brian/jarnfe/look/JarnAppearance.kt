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
import jr.brian.core.ui.theme.JarnLook
import jr.brian.core.ui.theme.JarnLooks
import jr.brian.core.ui.theme.JarnSwitchCue
import jr.brian.core.ui.theme.adaptCommandPanelPalette
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.frontend.look.JarnLookStore
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

/** An unsaved cue, so X can play whichever choice is highlighted in the tour. */
internal object JarnSwitchCuePreview {
    var cue by mutableStateOf<JarnSwitchCue?>(null)
}

@Immutable
internal data class JarnAppearance(val look: JarnLook, val reducedMotion: Boolean)

/** A live preview wins, then the Command Panel match, then the saved look. */
internal fun resolveJarnLook(
    savedId: String?,
    previewId: String?,
    palette: CommandPanelPalette? = null,
): JarnLook = previewId?.let { JarnLooks.fromId(it) }
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
    val look = remember(savedId, previewId, palette) { resolveJarnLook(savedId, previewId, palette) }
    return JarnAppearance(look, reducedMotion)
}

@Composable
internal fun rememberJarnDisplay(): JarnDisplayPrefs {
    val context = LocalContext.current
    val store = remember(context) { context.jarnLookStoreOrNull() }
    return store?.displayState?.collectAsState()?.value ?: JarnDisplayPrefs()
}
