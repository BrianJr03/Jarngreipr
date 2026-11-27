package jr.brian.frontend.library.setup

import jr.brian.frontend.library.art.EsdeMediaRootProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val INTERNAL_STORAGE = "/storage/emulated/0"
private const val STORAGE_ROOT = "/storage"
private const val ESDE_DIR = "ES-DE"
private const val DOWNLOADED_MEDIA_DIR = "downloaded_media"
private const val DEFAULT_ROMS_DIR = "ROMs"
private const val HOME_TOKEN = "~"
private const val ESPATH_TOKEN = "%ESPATH%"
private const val PRIMARY_VOLUME = "primary"
private val VIRTUAL_VOLUMES = setOf("emulated", "self")

/** Where ES-DE keeps its games, and whether ES-DE's own settings said so. */
data class EsdeRomsFolder(val path: String, val fromSettings: Boolean)

/** Read-only lookup of ES-DE's ROMs folder. Both calls touch the disk, so keep them off the main thread. */
interface EsdeRomsFolderSource {
    /** Whether an ES-DE folder is there at all. Needs no file access, and reads no file. */
    fun hasEsde(): Boolean

    /** Null means the plain folder picker. */
    fun find(): EsdeRomsFolder?
}

/** The raw `ROMDirectory` value in an `es_settings.xml`, or null when it is missing or blank. */
internal fun parseEsdeRomDirectory(xml: String): String? = parseEsdeSetting(xml, EsdeSettingKey.ROMDirectory)

/**
 * ES-DE writes `~` for the home folder and `%ESPATH%` for its own folder. On Android the home
 * folder is internal storage. Anything that still isn't an absolute path is rejected.
 */
internal fun resolveEsdeRomDirectory(raw: String, home: String, esdeDir: String): String? {
    val slashed = raw.replace('\\', '/')
    val expanded = when {
        slashed == HOME_TOKEN -> home
        slashed.startsWith("$HOME_TOKEN/") -> home + slashed.removePrefix(HOME_TOKEN)
        slashed.startsWith(ESPATH_TOKEN) -> esdeDir + slashed.removePrefix(ESPATH_TOKEN)
        else -> slashed
    }
    val clean = expanded.replace(Regex("/+"), "/").trimEnd('/')
    return clean.takeIf { it.startsWith("/") && "%" !in it }
}

/** The storage document id the folder picker opens at, such as `primary:ROMs`. */
fun storageDocumentId(path: String): String? {
    val clean = path.trimEnd('/')
    if (clean == INTERNAL_STORAGE || clean.startsWith("$INTERNAL_STORAGE/")) {
        return "$PRIMARY_VOLUME:" + clean.removePrefix(INTERNAL_STORAGE).trimStart('/')
    }
    val segments = clean.removePrefix("$STORAGE_ROOT/").split('/')
    if (!clean.startsWith("$STORAGE_ROOT/") || segments.first() in VIRTUAL_VOLUMES) return null
    return segments.first() + ":" + segments.drop(1).joinToString("/")
}

/**
 * Looks in ES-DE's folder next to the configured media folder, then in `ES-DE` on each volume.
 * Falls back to `ROMs` on internal storage. Only reads: ES-DE's files are never written.
 */
@Singleton
class EsdeRomsFolderFinder internal constructor(
    private val mediaRoot: () -> File?,
    private val volumes: () -> List<File>,
    private val readText: (File) -> String?,
    private val isDirectory: (File) -> Boolean,
) : EsdeRomsFolderSource {
    @Inject
    constructor(esdeRoot: EsdeMediaRootProvider) : this(
        mediaRoot = esdeRoot::esdeMediaRoot,
        volumes = ::storageVolumes,
        readText = ::readTextOrNull,
        isDirectory = File::isDirectory,
    )

    override fun hasEsde(): Boolean = esdeDirs().any(isDirectory)

    /** Every ES-DE folder that is there, the one beside the media folder first. Touches the disk. */
    fun esdeFolders(): List<File> = esdeDirs().filter(isDirectory)

    override fun find(): EsdeRomsFolder? {
        fromSettings()?.let { return EsdeRomsFolder(it, fromSettings = true) }
        val fallback = File(INTERNAL_STORAGE, DEFAULT_ROMS_DIR)
        return fallback.takeIf(isDirectory)?.let { EsdeRomsFolder(it.path, fromSettings = false) }
    }

    private fun fromSettings(): String? = esdeDirs().firstNotNullOfOrNull { dir ->
        ESDE_SETTINGS_FILES.firstNotNullOfOrNull { name -> readText(File(dir, name)) }
            ?.let(::parseEsdeRomDirectory)
            ?.let { resolveEsdeRomDirectory(it, INTERNAL_STORAGE, dir.path) }
            ?.takeIf { isDirectory(File(it)) }
    }

    private fun esdeDirs(): List<File> {
        val besideMedia = mediaRoot()?.takeIf { it.name == DOWNLOADED_MEDIA_DIR }?.parentFile
        return (listOfNotNull(besideMedia) + volumes().map { File(it, ESDE_DIR) }).distinctBy { it.path }
    }
}

private fun storageVolumes(): List<File> {
    val removable = File(STORAGE_ROOT).listFiles()
        ?.filter { it.isDirectory && it.name !in VIRTUAL_VOLUMES }
        .orEmpty()
    return listOf(File(INTERNAL_STORAGE)) + removable.sortedBy { it.name }
}

internal fun readTextOrNull(file: File): String? = runCatching { file.readText() }.getOrNull()
