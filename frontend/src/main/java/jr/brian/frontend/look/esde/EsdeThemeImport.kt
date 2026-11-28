package jr.brian.frontend.look.esde

import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.ui.theme.JarnLook
import jr.brian.core.ui.theme.JarnPaletteMatch
import jr.brian.frontend.library.setup.EsdeSettings
import jr.brian.frontend.library.systems.SystemCatalog
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

/** The spec to install after the fallback, and what the fallback has to tell the player. */
data class EsdeFallbackOutcome(
    val spec: JarnThemeSpec,
    val notes: List<EsdeFallbackNote> = emptyList(),
    /** How many of the player's systems got theme art. */
    val systemsWithArt: Int = 0,
    /** The theme's own fonts were copied and set on Jarn's type roles. */
    val fontsUsed: Boolean = false,
)

/**
 * The colors, art and fonts step between translation and install. It may write files into
 * `outDir`, the theme's unchecked install folder; they go through the installer's checks.
 */
fun interface EsdeImportFallback {
    fun apply(
        model: EsdeThemeModel,
        translation: EsdeTranslation,
        themeDir: File,
        outDir: File,
        reparse: (colorScheme: String) -> EsdeThemeModel,
    ): EsdeFallbackOutcome

    companion object {
        /** No colors of the theme's own were worked out, so the import follows the player's Look. */
        val None = EsdeImportFallback { _, translation, _, _, _ ->
            EsdeFallbackOutcome(translation.spec.followingLook(true))
        }

        /**
         * #581: readable colors from the theme with [savedLook] as the floor, plus its art and fonts.
         * When none of the theme's colors could be read ([JarnPaletteMatch.SavedLook]) the import
         * follows the Look, so its card, Look > Colors and Command Panel matching all treat it as
         * a Look-following layout. Its full palette, or just its accent on the Look's colors
         * ([JarnPaletteMatch.AccentOnSavedLook]), are colors of its own, kept as imported.
         */
        fun readable(savedLook: JarnLook, catalog: SystemCatalog) =
            EsdeImportFallback { model, translation, themeDir, outDir, reparse ->
                val result = EsdeThemeFallback.build(model, themeDir, savedLook, catalog, outDir, reparse)
                EsdeFallbackOutcome(
                    spec = result.applyTo(translation.spec).followingLook(followsLookFor(result.tier)),
                    notes = result.notes,
                    systemsWithArt = result.systemArt.size,
                    fontsUsed = result.fonts.display != null || result.fonts.body != null,
                )
            }
    }
}

private fun JarnThemeSpec.followingLook(follows: Boolean) = copy(meta = meta.copy(followsLook = follows))

/** Only a theme none of whose colors could be read follows the Look; an accent or a full palette is its own. */
internal fun followsLookFor(tier: JarnPaletteMatch): Boolean = tier == JarnPaletteMatch.SavedLook

sealed interface EsdeImportOutcome {
    /** Installed as [themeId]. [unusableFiles] are theme files that failed their checks and were left out. */
    data class Imported(
        val themeId: String,
        val folder: File,
        val name: String,
        val report: EsdeTranslationReport,
        val fallbackNotes: List<EsdeFallbackNote>,
        val systemsWithArt: Int,
        val systemCount: Int,
        val unusableFiles: Int,
        val fontsUsed: Boolean = false,
    ) : EsdeImportOutcome

    /** A theme in the format from before ES-DE, which Jarn doesn't read. */
    data object Legacy : EsdeImportOutcome

    data class Failed(val problem: ThemeImportProblem) : EsdeImportOutcome
}

/** What the player picked in ES-DE, used for the theme ES-DE uses now. */
fun EsdeSettings?.selectionFor(theme: EsdeThemeFolder): EsdeThemeSelection =
    if (this != null && theme.isCurrent) {
        EsdeThemeSelection(variant = themeVariant, colorScheme = themeColorScheme, aspectRatio = themeAspectRatio)
    } else {
        EsdeThemeSelection()
    }

/** The choices a re-import repeats. */
fun EsdeImportRecord.selection() = EsdeThemeSelection(variant = variant, colorScheme = colorScheme, aspectRatio = aspectRatio)

/**
 * Reads an ES-DE theme folder, translates it, runs the [fallback] and installs it with its import
 * record, so a later update can be noticed (#580). Re-importing the same folder replaces the earlier
 * copy. Blocking file IO that never writes to ES-DE's folder; call off the main thread.
 */
class EsdeThemeImport(
    private val themesRoot: File,
    private val fallback: EsdeImportFallback = EsdeImportFallback.None,
    private val installer: JarnThemeInstaller = JarnThemeInstaller(themesRoot),
    private val parser: EsdeThemeParser = EsdeThemeParser(),
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun run(
        source: File,
        systems: List<EsdeSystemRef>,
        selection: EsdeThemeSelection,
        options: EsdeTranslateOptions,
    ): EsdeImportOutcome {
        val stamp = readEsdeThemeStamp(source) ?: return EsdeImportOutcome.Failed(ThemeImportProblem.Unreadable)
        val refs = systems.ifEmpty { systemsInTheme(source) }
        val model = parser.parse(source, refs, selection)
        if (model.isLegacy) return EsdeImportOutcome.Legacy
        val translation = EsdeThemeTranslator.translate(model, options)
        val record = EsdeImportRecord(
            sourceFolder = source.path,
            variant = model.selection.variant,
            colorScheme = model.selection.colorScheme,
            aspectRatio = model.selection.aspectRatio,
            stamp = stamp,
            importedAtMs = now(),
        )
        var fallbackOutcome = EsdeFallbackOutcome(translation.spec)
        val result = installer.installFiles(
            sourceRoot = source,
            assets = translation.assets,
            replaceId = earlierImportOf(source),
            beforePlace = { folder -> writeEsdeImportRecord(folder, record) },
        ) { outDir ->
            val reparse = { scheme: String -> parser.parse(source, refs, model.selection.copy(colorScheme = scheme)) }
            fallback.apply(model, translation, source, outDir, reparse).also { fallbackOutcome = it }.spec
        }
        return when (result) {
            is ThemeImportResult.Installed -> EsdeImportOutcome.Imported(
                result.id, result.folder, result.spec.meta.name, translation.report, fallbackOutcome.notes,
                fallbackOutcome.systemsWithArt, refs.size, result.dropped, fallbackOutcome.fontsUsed,
            )
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
