package jr.brian.jarnfe.library

import androidx.compose.ui.Modifier
import jr.brian.jarnfe.transition.jarnDepthAnchor

internal fun Modifier.librarySystemAnchor(systemId: String): Modifier = jarnDepthAnchor(systemId)
