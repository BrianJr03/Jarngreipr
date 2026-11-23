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
import jr.brian.core.ui.theme.JarnLook
import jr.brian.core.ui.theme.JarnLooks
import jr.brian.frontend.look.JarnLookStore

interface JarnLookStoreProvider {
    val jarnLookStore: JarnLookStore
}

internal fun Context.jarnLookStoreOrNull(): JarnLookStore? =
    (applicationContext as? JarnLookStoreProvider)?.jarnLookStore

/** An unsaved look shown on both screens while the picker has focus. */
internal object JarnLookPreview {
    var lookId by mutableStateOf<String?>(null)
}

@Immutable
internal data class JarnAppearance(val look: JarnLook, val reducedMotion: Boolean)

internal fun resolveJarnLook(savedId: String?, previewId: String?): JarnLook =
    JarnLooks.fromId(previewId ?: savedId)

@Composable
internal fun rememberJarnAppearance(): JarnAppearance {
    val context = LocalContext.current
    val store = remember(context) { context.jarnLookStoreOrNull() }
    val savedId = store?.lookIdState?.collectAsState()?.value
    val reducedMotion = store?.reducedMotionState?.collectAsState()?.value ?: false
    return JarnAppearance(resolveJarnLook(savedId, JarnLookPreview.lookId), reducedMotion)
}
