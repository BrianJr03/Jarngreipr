package jr.brian.home.model

import kotlinx.serialization.Serializable

@Serializable
data class JingleEntry(
    val game: String,
    val file: String
)

@Serializable
data class JingleIndex(
    val name: String = "",
    val entries: List<JingleEntry> = emptyList()
)
