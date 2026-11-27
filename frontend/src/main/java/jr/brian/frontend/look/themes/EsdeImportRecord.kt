package jr.brian.frontend.look.themes

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

internal const val ESDE_IMPORT_RECORD_FILE = "esde-import.json"
private const val CAPABILITIES_FILE = "capabilities.xml"
private const val THEME_XML_FILE = "theme.xml"
private const val GIT_DIR = ".git"
private const val GIT_REF_PREFIX = "ref:"
private const val MAX_READ_BYTES = 512L * 1024
private const val MAX_REF_DEPTH = 4

private val recordJson = Json { ignoreUnknownKeys = true }

/**
 * What an ES-DE theme folder looked like when it was read. Any change means the player updated it:
 * the folder, `capabilities.xml` or `theme.xml` were touched, its git HEAD moved, or its
 * capabilities changed. Nulls are files that weren't there.
 */
@Serializable
data class EsdeThemeStamp(
    val folderModifiedMs: Long,
    val capabilitiesModifiedMs: Long? = null,
    val themeXmlModifiedMs: Long? = null,
    val gitHead: String? = null,
    val capabilitiesSha256: String? = null,
)

/**
 * Where an installed theme came from in ES-DE and what was chosen, saved beside the installed theme
 * so a re-import can repeat it. [dismissedStamp] is an update the player said no to, so it is only
 * offered again after a newer change.
 */
@Serializable
data class EsdeImportRecord(
    val sourceFolder: String,
    val variant: String? = null,
    val colorScheme: String? = null,
    val aspectRatio: String? = null,
    val stamp: EsdeThemeStamp,
    val importedAtMs: Long = 0L,
    val dismissedStamp: EsdeThemeStamp? = null,
)

/** The stamp of [folder] now, or null when it isn't there (SD card out, folder moved). Only reads. */
fun readEsdeThemeStamp(folder: File): EsdeThemeStamp? {
    if (!folder.isDirectory) return null
    val capabilities = File(folder, CAPABILITIES_FILE).takeIf(File::isFile)
    return EsdeThemeStamp(
        folderModifiedMs = folder.lastModified(),
        capabilitiesModifiedMs = capabilities?.lastModified(),
        themeXmlModifiedMs = File(folder, THEME_XML_FILE).takeIf(File::isFile)?.lastModified(),
        gitHead = readGitHead(File(folder, GIT_DIR)),
        capabilitiesSha256 = capabilities?.let(::sha256OrNull),
    )
}

/** The commit `.git/HEAD` points at, following a branch ref loose or packed. Null without a repo. */
internal fun readGitHead(gitDir: File): String? {
    if (!gitDir.isDirectory) return null
    var value = readSmall(File(gitDir, "HEAD"))?.trim() ?: return null
    repeat(MAX_REF_DEPTH) {
        if (!value.startsWith(GIT_REF_PREFIX)) return value.takeIf { it.isNotEmpty() }
        val ref = value.removePrefix(GIT_REF_PREFIX).trim()
        if (ref.contains("..")) return null
        value = readSmall(File(gitDir, ref))?.trim()
            ?: packedRef(gitDir, ref)
            ?: return null
    }
    return value
}

private fun packedRef(gitDir: File, ref: String): String? =
    readSmall(File(gitDir, "packed-refs"))?.lineSequence()
        ?.map { it.trim().split(' ') }
        ?.firstOrNull { it.size == 2 && it[1] == ref }
        ?.first()

private fun sha256OrNull(file: File): String? = runCatching {
    if (file.length() > MAX_READ_BYTES) return null
    MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
}.getOrNull()

private fun readSmall(file: File): String? = runCatching {
    file.takeIf { it.isFile && it.length() <= MAX_READ_BYTES }?.readText()
}.getOrNull()

/** The record saved beside an installed theme, or null for a theme that didn't come from ES-DE. */
fun readEsdeImportRecord(themeFolder: File): EsdeImportRecord? = try {
    readSmall(File(themeFolder, ESDE_IMPORT_RECORD_FILE))?.let { recordJson.decodeFromString<EsdeImportRecord>(it) }
} catch (_: SerializationException) {
    null
} catch (_: IllegalArgumentException) {
    null
}

/** Saves [record] beside the installed theme, through a temp file so a crash never leaves half a record. */
fun writeEsdeImportRecord(themeFolder: File, record: EsdeImportRecord) {
    val target = File(themeFolder, ESDE_IMPORT_RECORD_FILE)
    val temp = File(themeFolder, "$ESDE_IMPORT_RECORD_FILE.tmp")
    temp.writeText(recordJson.encodeToString(EsdeImportRecord.serializer(), record))
    if (!temp.renameTo(target)) {
        temp.delete()
        throw java.io.IOException("Couldn't save $ESDE_IMPORT_RECORD_FILE")
    }
}
