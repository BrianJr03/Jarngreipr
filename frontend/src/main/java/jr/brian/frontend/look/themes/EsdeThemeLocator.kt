package jr.brian.frontend.look.themes

import android.os.Environment
import jr.brian.frontend.library.setup.EsdeRomsFolderFinder
import jr.brian.frontend.library.setup.EsdeSettings
import jr.brian.frontend.library.setup.readEsdeSettings
import jr.brian.frontend.library.setup.unescapeXml
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val THEMES_DIR = "themes"
private const val CAPABILITIES_FILE = "capabilities.xml"
private const val MAX_TEXT_BYTES = 512L * 1024

private val ThemeNameEntry = Regex("""<themeName>\s*([^<]*?)\s*</themeName>""")

/** An ES-DE theme the player copied into `ES-DE/themes`. [isCurrent] is the one ES-DE uses now. */
data class EsdeThemeFolder(val folder: File, val name: String, val isCurrent: Boolean)

sealed interface EsdeThemeScan {
    /** `ES-DE/themes` can't be read without all-files access, and a folder grant doesn't cover it. */
    data object NoAccess : EsdeThemeScan

    /** No themes there. [themesFolder] is where to copy one, or null when there is no ES-DE folder. */
    data class NotFound(val themesFolder: File?) : EsdeThemeScan

    /** Every theme found, the current one first, then by name. */
    data class Found(val themes: List<EsdeThemeFolder>, val settings: EsdeSettings?) : EsdeThemeScan
}

/** The `<themeName>` in a `capabilities.xml`, or null when it is missing or blank. */
internal fun parseEsdeThemeName(xml: String): String? =
    ThemeNameEntry.find(xml)?.groupValues?.get(1)?.let(::unescapeXml)?.trim()?.takeIf { it.isNotEmpty() }

/**
 * Finds ES-DE themes copied by hand into `ES-DE/themes`, beside the media folder first. Themes ES-DE
 * downloaded itself sit in its app data, which no other app can read. Only reads; touches the disk.
 */
@Singleton
class EsdeThemeLocator internal constructor(
    private val esdeFolders: () -> List<File>,
    private val hasAllFilesAccess: () -> Boolean,
    private val readText: (File) -> String?,
) {
    @Inject
    constructor(finder: EsdeRomsFolderFinder) : this(
        esdeFolders = finder::esdeFolders,
        hasAllFilesAccess = Environment::isExternalStorageManager,
        readText = ::readSmallText,
    )

    fun scan(): EsdeThemeScan {
        if (!hasAllFilesAccess()) return EsdeThemeScan.NoAccess
        val folders = esdeFolders()
        val settings = folders.firstNotNullOfOrNull { readEsdeSettings(it, readText) }
        val current = settings?.themeSet
        val themes = folders.flatMap(::themesIn)
            .distinctBy { it.folder.name }
            .map { it.copy(isCurrent = it.folder.name == current) }
            .sortedWith(compareByDescending<EsdeThemeFolder> { it.isCurrent }.thenBy { it.name.lowercase() })
        if (themes.isEmpty()) return EsdeThemeScan.NotFound(folders.firstOrNull()?.let { File(it, THEMES_DIR) })
        return EsdeThemeScan.Found(themes, settings)
    }

    private fun themesIn(esdeDir: File): List<EsdeThemeFolder> =
        File(esdeDir, THEMES_DIR).listFiles().orEmpty()
            .filter { it.isDirectory && !it.name.startsWith(".") }
            .mapNotNull { dir ->
                val capabilities = File(dir, CAPABILITIES_FILE).takeIf(File::isFile) ?: return@mapNotNull null
                val name = readText(capabilities)?.let(::parseEsdeThemeName) ?: dir.name
                EsdeThemeFolder(folder = dir, name = name, isCurrent = false)
            }
}

private fun readSmallText(file: File): String? = runCatching {
    file.takeIf { it.length() <= MAX_TEXT_BYTES }?.readText()
}.getOrNull()
