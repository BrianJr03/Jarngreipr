package jr.brian.frontend.look.themes

import jr.brian.core.model.theme.JarnThemeDecodeResult
import jr.brian.core.model.theme.JarnThemeFormat
import jr.brian.core.model.theme.JarnThemeSpec
import java.io.File
import java.util.UUID

private const val INCOMING_PREFIX = ".incoming-"
private const val OUTGOING_PREFIX = ".outgoing-"
private const val MAX_ID_LENGTH = 48
private const val FALLBACK_ID = "theme"
private val NonIdChars = Regex("[^a-z0-9]+")

sealed interface ThemeImportResult {
    /** Added at [folder]. [dropped] counts file references that failed their checks and were left out. */
    data class Installed(val id: String, val folder: File, val spec: JarnThemeSpec, val dropped: Int) : ThemeImportResult

    /** Made by a newer Jarn. The player has to update first. */
    data class TooNew(val schema: Int, val supported: Int) : ThemeImportResult

    data class Rejected(val problem: ThemeImportProblem) : ThemeImportResult
}

/** What to do when a theme with the same id is already installed. */
enum class ThemeIdClash { KeepBoth, Replace }

/** A folder-safe id from the theme's name, such as `art-book-next`. */
internal fun themeIdFor(name: String): String =
    name.lowercase().replace(NonIdChars, "-").trim('-').take(MAX_ID_LENGTH).trim('-').ifEmpty { FALLBACK_ID }

/** The theme file's root: the archive's top level, or the one folder holding `theme.json`. */
internal fun themeRootPrefix(records: List<ThemeZipRecord>): String {
    if (records.any { it.name == THEME_FILE }) return ""
    val nested = records.filter { it.name.count { c -> c == '/' } == 1 && it.name.endsWith("/$THEME_FILE") }
    return nested.singleOrNull()?.name?.removeSuffix(THEME_FILE)
        ?: throw ThemeImportException(ThemeImportProblem.NotATheme)
}

/**
 * Installs `.jarntheme` files (zips) into [themesRoot]`/<id>`. Everything is extracted and checked
 * in a hidden folder first, then renamed into place, so a failed import leaves nothing behind and
 * a theme folder is never half written. Callers keep this off the main thread.
 */
class JarnThemeInstaller(
    private val themesRoot: File,
    private val limits: ThemeArchiveLimits = ThemeArchiveLimits(),
) {
    @Synchronized
    fun install(archive: File, clash: ThemeIdClash = ThemeIdClash.KeepBoth): ThemeImportResult {
        themesRoot.mkdirs()
        clearLeftovers()
        val incoming = File(themesRoot, INCOMING_PREFIX + UUID.randomUUID())
        return try {
            incoming.mkdirs()
            extractThemeArchive(archive, incoming, limits, ::themeRootPrefix) { path ->
                path == THEME_FILE || themeAssetKind(path) != null
            }
            checkAndPlace(incoming, clash)
        } catch (e: ThemeImportException) {
            ThemeImportResult.Rejected(e.problem)
        } catch (e: java.io.IOException) {
            ThemeImportResult.Rejected(ThemeImportProblem.Damaged)
        } finally {
            incoming.deleteRecursively()
        }
    }

    private fun checkAndPlace(incoming: File, clash: ThemeIdClash): ThemeImportResult {
        val themeFile = File(incoming, THEME_FILE)
        if (!themeFile.isFile) return ThemeImportResult.Rejected(ThemeImportProblem.NotATheme)
        if (themeFile.length() > MAX_THEME_JSON_BYTES) return ThemeImportResult.Rejected(ThemeImportProblem.TooLarge)
        val spec = when (val decoded = JarnThemeFormat.decode(themeFile.readText())) {
            is JarnThemeDecodeResult.Decoded -> decoded.spec
            is JarnThemeDecodeResult.TooNew -> return ThemeImportResult.TooNew(decoded.schema, decoded.supported)
            is JarnThemeDecodeResult.Invalid -> return ThemeImportResult.Rejected(ThemeImportProblem.NotATheme)
        }
        val checked = checkFileRefs(spec, checkThemeAssets(incoming))
        themeFile.writeText(JarnThemeFormat.encode(checked.spec))
        val id = placeId(themeIdFor(spec.meta.name), clash)
        val target = File(themesRoot, id)
        moveIntoPlace(incoming, target)
        return ThemeImportResult.Installed(id, target, checked.spec, checked.dropped)
    }

    private fun placeId(base: String, clash: ThemeIdClash): String {
        if (clash == ThemeIdClash.Replace || !File(themesRoot, base).exists()) return base
        return generateSequence(2) { it + 1 }.map { "$base-$it" }.first { !File(themesRoot, it).exists() }
    }

    /** Swaps an existing theme out first, and puts it back if the new one can't be moved in. */
    private fun moveIntoPlace(incoming: File, target: File) {
        val outgoing = File(themesRoot, OUTGOING_PREFIX + UUID.randomUUID()).takeIf { target.exists() }
        if (outgoing != null && !target.renameTo(outgoing)) throw ThemeImportException(ThemeImportProblem.Damaged)
        if (!incoming.renameTo(target)) {
            outgoing?.renameTo(target)
            throw ThemeImportException(ThemeImportProblem.Damaged)
        }
        outgoing?.deleteRecursively()
    }

    private fun clearLeftovers() {
        themesRoot.listFiles().orEmpty()
            .filter { it.name.startsWith(INCOMING_PREFIX) || it.name.startsWith(OUTGOING_PREFIX) }
            .forEach { it.deleteRecursively() }
    }
}
