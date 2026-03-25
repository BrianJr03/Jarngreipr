package jr.brian.home.esde.model

import jr.brian.home.model.JingleIndex

data class JingleSource(
    val key: String,
    val index: JingleIndex,
    val isLocal: Boolean
)