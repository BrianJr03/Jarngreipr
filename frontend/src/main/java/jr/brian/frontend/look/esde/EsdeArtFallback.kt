package jr.brian.frontend.look.esde

import jr.brian.frontend.library.systems.SystemCatalog
import jr.brian.frontend.library.systems.SystemDef

private const val FULL_SCREEN = "1 1"
private val LOGO_NAMES = setOf("logo", "systemlogo")
private val BACKGROUND_NAMES = setOf("background", "backdrop", "systembackground")

/**
 * ES-DE system folders that are a variant of another system (hacks, regional sets). When the
 * theme has nothing for the variant, the parent's art stands in, as ES-DE itself does.
 */
private val VARIANT_SUFFIXES = listOf("hacks", "h", "na", "jp", "eu", "us", "widescreen")

/** Theme-relative candidate files for one player system's art, best first. */
data class EsdeSystemArtCandidates(
    val jarnSystemId: String,
    val esdeName: String,
    val logo: List<String>,
    val background: List<String>,
)

/** Which of Jarn's type roles a theme font would fill: titles and headings, or body and labels. */
enum class EsdeFontRole { Display, Body }

/** A theme-relative font path and the role it would fill, before the glyph check. */
data class EsdeFontCandidate(val role: EsdeFontRole, val path: String)

/**
 * Finds each player system's logo and background in an ES-DE theme and maps the ES-DE folder
 * name to a Jarn system through [SystemCatalog.findByFolderAlias]. A variant system with nothing
 * of its own (`snesh`) borrows its parent's (`snes`) by swapping the name in the path.
 */
internal object EsdeArtFallback {

    fun systemArt(model: EsdeThemeModel, catalog: SystemCatalog): Pair<List<EsdeSystemArtCandidates>, List<String>> {
        val unknown = mutableListOf<String>()
        val art = model.systems.values.mapNotNull { system ->
            val ref = system.system
            val def = catalog.findByFolderAlias(ref.name) ?: catalog.findByFolderAlias(ref.theme)
            val parent = parentOf(ref, catalog)
            val target = def ?: parent?.second
            if (target == null) {
                unknown += ref.name
                return@mapNotNull null
            }
            val own = system.elements()
            EsdeSystemArtCandidates(
                jarnSystemId = target.id,
                esdeName = ref.name,
                logo = own.logoPaths().withParent(ref, parent?.first),
                background = own.backgroundPaths().withParent(ref, parent?.first),
            )
        }
        return art to unknown
    }

    /** Body text from the game list's list, display text from the system view or any other text. */
    fun fonts(model: EsdeThemeModel): List<EsdeFontCandidate> {
        val elements = model.allElements()
        val body = elements.filter { it.view == EsdeView.Gamelist && it.key.type == "textlist" }.fontPaths() +
            elements.filter { it.key.type == "textlist" }.fontPaths()
        val display = elements.filter { it.view == EsdeView.System && it.key.type != "textlist" }.fontPaths() +
            elements.filter { it.key.type == "text" }.fontPaths()
        return (body.distinct().map { EsdeFontCandidate(EsdeFontRole.Body, it) } +
            display.distinct().map { EsdeFontCandidate(EsdeFontRole.Display, it) }).distinct()
    }

    private fun parentOf(ref: EsdeSystemRef, catalog: SystemCatalog): Pair<String, SystemDef>? =
        VARIANT_SUFFIXES.asSequence()
            .filter { ref.theme.length > it.length && ref.theme.endsWith(it) }
            .map { ref.theme.removeSuffix(it).trimEnd('_', '-') }
            .mapNotNull { name -> catalog.findByFolderAlias(name)?.let { name to it } }
            .firstOrNull()

    private fun EsdeSystemTheme.elements(): List<EsdeElement> = EsdeView.entries.flatMap { view ->
        views[view]?.elements.orEmpty().map { (key, props) -> EsdeElement(view, key, props) }
    }

    private fun List<EsdeElement>.logoPaths(): List<String> = (
        filter { it.view == EsdeView.System && it.key.type == "carousel" }.mapNotNull { it.props["staticImage"] } +
            filter { it.key.type == "image" && it.key.name.lowercase() in LOGO_NAMES }.mapNotNull { it.props["path"] } +
            filter { it.key.type == "carousel" }.mapNotNull { it.props["defaultImage"] }
        ).distinct()

    private fun List<EsdeElement>.backgroundPaths(): List<String> = (
        filter { it.key.type == "image" && it.key.name.lowercase() in BACKGROUND_NAMES }.mapNotNull { it.props["path"] } +
            filter { it.key.type == "image" && it.props["size"] == FULL_SCREEN }.mapNotNull { it.props["path"] }
        ).distinct()

    private fun List<EsdeElement>.fontPaths(): List<String> = mapNotNull { it.props["fontPath"] }

    /** Each path, then the same path with the variant's name swapped for its parent's. */
    private fun List<String>.withParent(ref: EsdeSystemRef, parent: String?): List<String> {
        if (parent == null) return this
        val name = Regex("(?<![A-Za-z0-9])${Regex.escape(ref.theme)}(?![A-Za-z0-9])")
        return (this + map { it.replace(name, parent) }).distinct()
    }
}
