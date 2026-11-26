package jr.brian.home.data.config

import jr.brian.frontend.library.collections.BackedUpCollection
import jr.brian.frontend.library.collections.BackedUpCollectionMember
import jr.brian.frontend.library.favorites.BackedUpFavorite
import kotlinx.serialization.Serializable

/** A game by system and ROM path, so it matches after a rescan under a different ROM folder. */
@Serializable
data class JarnGameKeyConfig(
    val systemId: String = "",
    val relativePath: String = "",
)

@Serializable
data class JarnCollectionConfig(
    val name: String = "",
    val position: Int = 0,
    val members: List<JarnGameKeyConfig> = emptyList(),
)

internal fun BackedUpCollection.toConfig() = JarnCollectionConfig(
    name = name,
    position = position,
    members = members.map { JarnGameKeyConfig(it.systemId, it.relativePath) },
)

internal fun JarnCollectionConfig.toBackedUpCollection() = BackedUpCollection(
    name = name,
    position = position,
    members = members.map { BackedUpCollectionMember(it.systemId, it.relativePath) },
)

internal fun BackedUpFavorite.toConfig() = JarnGameKeyConfig(systemId, relativePath)

internal fun JarnGameKeyConfig.toBackedUpFavorite() = BackedUpFavorite(systemId, relativePath)
