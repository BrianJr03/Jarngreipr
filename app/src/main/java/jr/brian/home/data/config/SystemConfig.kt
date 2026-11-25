package jr.brian.home.data.config

import jr.brian.home.esde.model.SystemFolderMapping
import kotlinx.serialization.Serializable

@Serializable
data class SystemConfig(
    val badgesVisible: Boolean = true,
    val shadeTabPage: Int = 0,
    val hiddenSystems: List<String> = emptyList(),
    /** v66: hidden games as `system/path`. Null on pre-v66 exports, and import keeps them. */
    val hiddenGames: List<String>? = null,
    val systemFolderMappings: List<SystemFolderMapping> = emptyList(),
    val rcNotificationsEnabled: Boolean = false
)
