package jr.brian.frontend.look.esde

import jr.brian.frontend.library.setup.EsdeSettings
import jr.brian.frontend.look.themes.EsdeImportRecord
import jr.brian.frontend.look.themes.EsdeThemeFolder
import jr.brian.frontend.look.themes.JarnThemeInstaller
import jr.brian.frontend.look.themes.ThemeImportProblem
import jr.brian.frontend.look.themes.ThemeImportResult
import jr.brian.frontend.look.themes.readEsdeImportRecord
import jr.brian.frontend.look.themes.readEsdeThemeStamp
import jr.brian.frontend.look.themes.writeEsdeImportRecord
import java.io.File

private const val THEME_XML = "theme.xml"
private const val MAX_FALLBACK_SYSTEMS = 40

/**
 * Turns a translation into the colors, fonts and files the installed theme gets. The default keeps
 * the translation as it is (the caller's colors). The readable-colors fallback (#581) plugs in here.
 */
fun interface EsdeImportFallback {
    fun apply(model: EsdeThemeModel, translation: EsdeTranslation): EsdeTranslation

    companion object {
        val None = EsdeImportFallback { _, translation -> translation }
    }
}

sealed interface EsdeImportOutcome {
    /** Installed as [themeId]. [unusableFiles] are theme files that failed their checks and were left out. */
    data class Imported(
        val themeId: String,
        val folder: File,
        val name: String,
        val report: EsdeTranslationReport,
        val unusableFiles: Int,
    ) : EsdeImportOutcome

    /** A theme in the format from before ES-DE, which Jarn doesn't read. */
    data object Legacy : EsdeImportOutcome

    data class Failed(val problem: ThemeImportProblem) : EsdeImportOutcome
}

/** What the player picked in ES-DE, used for the theme ES-DE uses now and for re-imports. */
fun EsdeSettings?.selectionFor(theme: EsdeThemeFolder): EsdeThemeSelection =
    if (this != null && theme.isCurrent) {
        EsdeThemeSelection(variant = themeVariant, colorScheme = themeColorScheme, aspectRatio = themeAspectRatio)
    } else {
        EsdeThemeSelection()
    }

fun EsdeImportRecord.selection() = EsdeThemeSelection(variant = variant, colorScheme = colorScheme, aspectRatio = aspectRatio)

/**
 * Reads an ES-DE theme folder, translates it and installs it with its import record, so a later
 * update can be noticed (#580). Re-importing the same folder replaces the earlier copy. Blocking
 * file IO, never writes to ES-DE's folder; call off the main thread.
 */
class EsdeThemeImport(
    private val themesRoot: File,
    private val installer: JarnThemeInstaller = JarnThemeInstaller(themesRoot),
    private val parser: EsdeThemeParser = EsdeThemeParser(),
    private val fallback: EsdeImportFallback = EsdeImportFallback.None,
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun run(
        source: File,
        systems: List<EsdeSystemRef>,
        selection: EsdeThemeSelection,
        options: EsdeTranslateOptions,
    ): EsdeImportOutcome {
        val stamp = readEsdeThemeStamp(source) ?: return EsdeImportOutcome.Failed(ThemeImportProblem.Unreadable)
        val model = parser.parse(source, systems.ifEmpty { systemsInTheme(source) }, selection)
        if (model.isLegacy) return EsdeImportOutcome.Legacy
        val translation = fallback.apply(model, EsdeThemeTranslator.translate(model, options))
        val record = EsdeImportRecord(
            sourceFolder = source.path,
            variant = model.selection.variant,
            colorScheme = model.selection.colorScheme,
            aspectRatio = model.selection.aspectRatio,
            stamp = stamp,
            importedAtMs = now(),
        )
        val result = installer.installFiles(
            spec = translation.spec,
            sourceRoot = source,
            assets = translation.assets,
            replaceId = earlierImportOf(source),
        ) { folder -> writeEsdeImportRecord(folder, record) }
        return when (result) {
            is ThemeImportResult.Installed ->
                EsdeImportOutcome.Imported(result.id, result.folder, result.spec.meta.name, translation.report, result.dropped)
            is ThemeImportResult.Rejected -> EsdeImportOutcome.Failed(result.problem)
            is ThemeImportResult.TooNew -> EsdeImportOutcome.Failed(ThemeImportProblem.Damaged)
        }
    }

    /** The installed theme imported from [source] before, so a re-import replaces it instead of adding a copy. */
    private fun earlierImportOf(source: File): String? = themesRoot.listFiles().orEmpty()
        .filter { it.isDirectory && !it.name.startsWith(".") }
        .firstOrNull { readEsdeImportRecord(it)?.sourceFolder == source.path }
        ?.name

    /** With no library yet, the systems the theme itself styles stand in, so the import still has views. */
    private fun systemsInTheme(source: File): List<EsdeSystemRef> = source.listFiles().orEmpty()
        .filter { it.isDirectory && File(it, THEME_XML).isFile }
        .sortedBy { it.name }
        .take(MAX_FALLBACK_SYSTEMS)
        .map { EsdeSystemRef(it.name) }
}
