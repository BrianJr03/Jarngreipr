package jr.brian.home.model.jingles

import jr.brian.home.esde.model.JingleSource
import jr.brian.home.model.JingleEntry

data class LookupSnapshot(
    val sources: List<JingleSource> = emptyList(),
    val regexEntries: List<Triple<Regex, JingleSource, JingleEntry>> = emptyList(),
    val exactMap: Map<String, Pair<JingleSource, JingleEntry>> = emptyMap(),
    val lowerEntries: List<Triple<String, JingleSource, JingleEntry>> = emptyList(),
    val branchMap: Map<String, String> = emptyMap(),
)