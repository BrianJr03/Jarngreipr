package jr.brian.home.data.config

import jr.brian.frontend.library.art.ArtChoice
import jr.brian.frontend.library.art.GameArtChoices
import jr.brian.frontend.library.play.BackedUpPlayStat
import jr.brian.frontend.library.setup.BackedUpFolder
import kotlinx.serialization.Serializable

@Serializable
data class JarnPlayStatConfig(
    val systemId: String = "",
    val relativePath: String = "",
    val playCount: Int = 0,
    val playTimeMinutes: Int = 0,
)

internal fun BackedUpPlayStat.toConfig() =
    JarnPlayStatConfig(systemId, relativePath, playCount, playTimeMinutes)

internal fun JarnPlayStatConfig.toBackedUpPlayStat() =
    BackedUpPlayStat(systemId, relativePath, playCount, playTimeMinutes)

@Serializable
data class JarnArtChoicesConfig(
    val topBySystem: Map<String, String> = emptyMap(),
    val bottomBySystem: Map<String, String> = emptyMap(),
    val topByGame: Map<String, String> = emptyMap(),
    val bottomByGame: Map<String, String> = emptyMap()
)

internal fun GameArtChoices.toConfig() = JarnArtChoicesConfig(
    topBySystem = topBySystem.mapValues { it.value.name },
    bottomBySystem = bottomBySystem.mapValues { it.value.name },
    topByGame = topByGame.mapValues { it.value.name },
    bottomByGame = bottomByGame.mapValues { it.value.name }
)

internal fun JarnArtChoicesConfig.toChoices() = GameArtChoices(
    topBySystem = topBySystem.knownArtChoices(),
    bottomBySystem = bottomBySystem.knownArtChoices(),
    topByGame = topByGame.knownArtChoices(),
    bottomByGame = bottomByGame.knownArtChoices()
)

private fun Map<String, String>.knownArtChoices(): Map<String, ArtChoice> =
    entries.mapNotNull { (key, name) -> ArtChoice.fromNameOrNull(name)?.let { key to it } }.toMap()

@Serializable
data class LibraryFolderConfig(
    val treeUri: String = "",
    val displayPath: String = "",
    val volumeId: String? = null
)

internal fun BackedUpFolder.toConfig() = LibraryFolderConfig(treeUri, displayPath, volumeId)

internal fun LibraryFolderConfig.toBackedUpFolder() = BackedUpFolder(treeUri, displayPath, volumeId)
