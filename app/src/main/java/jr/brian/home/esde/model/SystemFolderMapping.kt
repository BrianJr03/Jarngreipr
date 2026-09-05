package jr.brian.home.esde.model

import kotlinx.serialization.Serializable

/**
 * A user-declared "this folder is the [systemName] system" binding.
 *
 * The folder can live anywhere the SAF picker can reach — outside `/Roms`,
 * under a different name, or on an SD card. Because SD-card ROMs aren't
 * directly openable via `java.io.File` under scoped storage, the persisted
 * [treeUri] is the authoritative handle for scanning and launching. The
 * on-disk [displayPath] is derived from the tree document ID purely for the
 * UI; nothing behind the mapping should ever prefer it over [treeUri].
 */
@Serializable
data class SystemFolderMapping(
    val systemName: String,
    val treeUri: String,
    val displayPath: String,
)
