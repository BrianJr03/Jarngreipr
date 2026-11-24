package jr.brian.frontend.library.art

import jr.brian.core.model.SystemArtSource
import java.io.File

internal const val PLATFORM_BACKGROUNDS_FOLDER = "platform_backgrounds"
internal const val PLATFORM_LOGOS_FOLDER = "platform_logos"
internal const val BUNDLED_LOGOS_DIR = "system_logos"

private const val BUNDLED_LOGO_EXTENSION = "svg"
private val PLATFORM_IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "webp")

/** A system's own artwork, separate from the art of its games. */
data class SystemArt(val backgroundPath: String? = null, val logoPath: String? = null)

data class LibrarySystemWithArt(val system: LibrarySystem, val art: SystemArt)

/**
 * The first image in `<system>/<folder>` across [systemDirs], preferring one
 * named after its system folder so stray files never win.
 */
internal fun platformImage(systemDirs: List<File>, folder: String): String? =
    systemDirs.firstNotNullOfOrNull { dir ->
        File(dir, folder).listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in PLATFORM_IMAGE_EXTENSIONS }
            ?.minWithOrNull(compareBy({ it.nameWithoutExtension != dir.name }, { it.name }))
            ?.path
    }

/** The app's bundled logo for the first of [folders] that has one. */
internal fun bundledLogo(folders: List<String>, bundledFiles: Set<String>): String? =
    folders.firstNotNullOfOrNull { folder ->
        "$folder.$BUNDLED_LOGO_EXTENSION".takeIf { it in bundledFiles }
            ?.let { "file:///android_asset/$BUNDLED_LOGOS_DIR/$it" }
    }

/** The chosen source wins, and the other only fills what it doesn't have. */
internal fun pickSystemArt(source: SystemArtSource, retroHrai: SystemArt, bundledLogo: String?): SystemArt =
    when (source) {
        SystemArtSource.RetroHrai -> retroHrai.copy(logoPath = retroHrai.logoPath ?: bundledLogo)
        SystemArtSource.Bundled -> bundledLogo?.let { SystemArt(logoPath = it) } ?: retroHrai
    }
