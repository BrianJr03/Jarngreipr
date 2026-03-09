package jr.brian.home.esde.model

import jr.brian.home.model.JingleIndex

data class JingleSource(
    val key: String, // repoSlug for GitHub, URI string for local
    val index: JingleIndex,
    val isLocal: Boolean
)