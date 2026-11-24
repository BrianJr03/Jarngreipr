package jr.brian.jarnfe.library

import androidx.compose.ui.Modifier
import jr.brian.jarnfe.transition.jarnDepthAnchor

/** Marks where a system sits on the Stage, in either layout, for the open and back transitions. */
internal fun Modifier.librarySystemAnchor(systemId: String): Modifier = jarnDepthAnchor(systemId)
