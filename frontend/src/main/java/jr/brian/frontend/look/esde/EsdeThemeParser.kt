package jr.brian.frontend.look.esde

import android.util.Xml
import jr.brian.frontend.look.esde.EsdeParseIssue.Kind
import org.xmlpull.v1.XmlPullParser
import java.io.File

internal const val ESDE_MAX_INCLUDE_DEPTH = 8
private const val CAPABILITIES_FILE = "capabilities.xml"
private const val THEME_FILE = "theme.xml"
private const val ALL_OPTIONS = "all"
private val VARIABLE_PATTERN = Regex("""\$\{([^}]+)\}""")

/**
 * Reads an ES-DE theme folder into an [EsdeThemeModel] for the given player systems.
 * Follows ES-DE's rules (per-system `theme.xml`, includes, variables, capability selection,
 * elements merged by type and name, last definition wins) but never throws: anything it
 * can't read is skipped and reported as an [EsdeParseIssue].
 *
 * Only the current format is read. A theme with `<formatVersion>` is reported as legacy.
 * Blocking file IO; call off the main thread.
 */
class EsdeThemeParser(
    private val newParser: () -> XmlPullParser = Xml::newPullParser,
) {

    fun parse(
        themeDir: File,
        systems: List<EsdeSystemRef>,
        requested: EsdeThemeSelection = EsdeThemeSelection(),
    ): EsdeThemeModel {
        val issues = mutableListOf<EsdeParseIssue>()
        val reader = EsdeXmlReader(themeDir, newParser) { issues += it }
        val capabilities = readCapabilities(reader, issues)
        val selection = resolveSelection(capabilities, requested, reader, issues)

        val rootTheme = File(reader.root, THEME_FILE)
        if (rootTheme.isFile && reader.read(rootTheme)?.child("formatVersion") != null) {
            issues += reader.issue(Kind.LegacyFormat, rootTheme, "Pre ES-DE theme format")
            return EsdeThemeModel(capabilities, selection, emptyMap(), issues.distinct())
        }

        val themes = systems.associate { system ->
            system.name to SystemPass(system, selection, reader, issues).run()
        }
        return EsdeThemeModel(capabilities, selection, themes, issues.distinct())
    }

    private fun readCapabilities(reader: EsdeXmlReader, issues: MutableList<EsdeParseIssue>): EsdeThemeCapabilities {
        val file = File(reader.root, CAPABILITIES_FILE)
        if (!file.isFile) {
            issues += reader.issue(Kind.MissingCapabilities, file, "ES-DE needs $CAPABILITIES_FILE")
            return EsdeThemeCapabilities()
        }
        val root = reader.read(file) ?: return EsdeThemeCapabilities()
        if (root.name != "themeCapabilities") {
            issues += reader.issue(Kind.Malformed, file, "Root is <${root.name}>", root.line)
            return EsdeThemeCapabilities()
        }
        return EsdeThemeCapabilities(
            themeName = root.child("themeName")?.text?.ifEmpty { null },
            variants = root.childrenNamed("variant").mapNotNull { it.toOption() },
            colorSchemes = root.childrenNamed("colorScheme").mapNotNull { it.toOption() },
            fontSizes = root.childrenNamed("fontSize").map { it.text }.filter { it.isNotEmpty() },
            aspectRatios = root.childrenNamed("aspectRatio").map { it.text }.filter { it.isNotEmpty() },
        )
    }

    private fun EsdeXmlNode.toOption(): EsdeNamedOption? {
        val name = attributes["name"]?.trim()?.ifEmpty { null } ?: return null
        return EsdeNamedOption(
            name = name,
            label = child("label")?.text?.ifEmpty { null },
            selectable = child("selectable")?.text?.trim() != "false",
        )
    }

    private fun resolveSelection(
        capabilities: EsdeThemeCapabilities,
        requested: EsdeThemeSelection,
        reader: EsdeXmlReader,
        issues: MutableList<EsdeParseIssue>,
    ): EsdeThemeSelection {
        fun pick(kind: String, wanted: String?, options: List<String>, fallback: String?): String? {
            if (wanted == null || wanted in options) return wanted ?: fallback
            issues += reader.issue(
                Kind.UnknownSelection,
                File(reader.root, CAPABILITIES_FILE),
                "No $kind named $wanted",
            )
            return fallback
        }
        val variantNames = capabilities.variants.map { it.name }
        return EsdeThemeSelection(
            variant = pick(
                "variant",
                requested.variant,
                variantNames,
                capabilities.variants.firstOrNull { it.selectable }?.name ?: variantNames.firstOrNull(),
            ),
            colorScheme = capabilities.colorSchemes.map { it.name }
                .let { pick("color scheme", requested.colorScheme, it, it.firstOrNull()) },
            fontSize = capabilities.fontSizes.let { pick("font size", requested.fontSize, it, it.firstOrNull()) },
            aspectRatio = pick(
                "aspect ratio",
                requested.aspectRatio,
                capabilities.aspectRatios,
                capabilities.aspectRatios.firstOrNull(),
            ),
        )
    }

    /** One system's walk through the theme, with its own variables and merged elements. */
    private class SystemPass(
        private val system: EsdeSystemRef,
        private val selection: EsdeThemeSelection,
        private val reader: EsdeXmlReader,
        private val issues: MutableList<EsdeParseIssue>,
    ) {
        private val variables = linkedMapOf(
            "system.name" to system.name,
            "system.fullName" to system.fullName,
            "system.theme" to system.theme,
        )
        private val views = linkedMapOf<EsdeView, LinkedHashMap<EsdeElementKey, LinkedHashMap<String, String>>>()
        private val includeStack = ArrayDeque<File>()

        fun run(): EsdeSystemTheme {
            val systemTheme = File(reader.root, "${system.theme}${File.separator}$THEME_FILE").canonicalFile
            val rootTheme = File(reader.root, THEME_FILE)
            val main = systemTheme.takeIf { reader.isInside(it) && it.isFile } ?: rootTheme
            if (main.isFile) {
                processFile(main, depth = 0)
            } else {
                issues += reader.issue(Kind.MissingThemeFile, main, "No $THEME_FILE for this system")
                    .copy(systemId = system.name)
            }
            return EsdeSystemTheme(system, views.mapValues { (_, elements) -> EsdeViewModel(elements.toMap()) })
        }

        private fun processFile(file: File, depth: Int) {
            if (file in includeStack) {
                report(Kind.IncludeCycle, includeStack.last(), "Includes loop back to ${reader.relative(file)}")
                return
            }
            if (depth > ESDE_MAX_INCLUDE_DEPTH) {
                report(Kind.IncludeTooDeep, file, "More than $ESDE_MAX_INCLUDE_DEPTH nested includes")
                return
            }
            val root = reader.read(file) ?: return
            if (root.name != "theme") {
                report(Kind.Malformed, file, "Root is <${root.name}>", root.line)
                return
            }
            includeStack.addLast(file)
            processBlock(root, file, depth)
            includeStack.removeLast()
        }

        /** ES-DE's fixed order: variables, color schemes, font sizes, includes, views, variants, aspect ratios. */
        private fun processBlock(node: EsdeXmlNode, file: File, depth: Int) {
            node.childrenNamed("variables").forEach { defineVariables(it, file) }
            node.selected("colorScheme", selection.colorScheme).forEach { scheme ->
                scheme.childrenNamed("variables").forEach { defineVariables(it, file) }
            }
            node.selected("fontSize", selection.fontSize).forEach { size ->
                size.childrenNamed("variables").forEach { defineVariables(it, file) }
            }
            node.childrenNamed("include").forEach { include(it, file, depth) }
            node.childrenNamed("view").forEach { mergeView(it, file) }
            node.selected("variant", selection.variant).forEach { processBlock(it, file, depth) }
            node.selected("aspectRatio", selection.aspectRatio).forEach { processBlock(it, file, depth) }
            node.childrenNamed("feature").firstOrNull()?.let {
                report(Kind.LegacyFormat, file, "<feature> blocks aren't read", it.line)
            }
        }

        private fun report(kind: Kind, file: File, detail: String, line: Int? = null) {
            issues += reader.issue(kind, file, detail, line)
        }

        private fun EsdeXmlNode.selected(tag: String, chosen: String?): List<EsdeXmlNode> =
            childrenNamed(tag).filter { block -> block.names().any { it == ALL_OPTIONS || it == chosen } }

        /** Like ES-DE, a later definition replaces an earlier one and an empty value is ignored. */
        private fun defineVariables(node: EsdeXmlNode, file: File) {
            node.children.forEach { variable ->
                val value = substitute(variable.text, file, variable.line)
                if (value.isNotEmpty()) variables[variable.name] = value
            }
        }

        private fun include(node: EsdeXmlNode, file: File, depth: Int) {
            val path = substitute(node.text, file, node.line).ifBlank { return }
            val target = reader.resolveInside(file.parentFile ?: reader.root, path, file, node.line) ?: return
            if (!target.isFile) {
                report(Kind.MissingInclude, file, "Missing include $path", node.line)
                return
            }
            processFile(target, depth + 1)
        }

        private fun mergeView(view: EsdeXmlNode, file: File) {
            val targets = view.names().mapNotNull { EsdeView.fromXml(it) }
            if (targets.isEmpty()) return
            view.children.forEach { element ->
                val names = element.names()
                if (names.isEmpty()) {
                    report(Kind.Malformed, file, "<${element.name}> has no name", element.line)
                    return@forEach
                }
                val properties = element.children.mapNotNull { property ->
                    resolveValue(property, file)?.let { property.name to it }
                }
                targets.forEach { target ->
                    val elements = views.getOrPut(target) { linkedMapOf() }
                    names.forEach { name ->
                        elements.getOrPut(EsdeElementKey(element.name, name)) { linkedMapOf() }.putAll(properties)
                    }
                }
            }
        }

        /** Substitutes variables, then makes a relative path relative to the theme root. Null when it leaves it. */
        private fun resolveValue(property: EsdeXmlNode, file: File): String? {
            val value = substitute(property.text, file, property.line)
            if (value.startsWith("~")) {
                report(Kind.PathOutsideTheme, file, "Path leaves the theme folder: $value", property.line)
                return null
            }
            if (!value.startsWith("./") && !value.startsWith("../") && !value.startsWith("/")) return value
            val baseDir = file.parentFile ?: reader.root
            val resolved = reader.resolveInside(baseDir, value, file, property.line) ?: return null
            return "./" + reader.relative(resolved)
        }

        private fun substitute(text: String, file: File, line: Int): String =
            VARIABLE_PATTERN.replace(text) { match ->
                val name = match.groupValues[1]
                variables[name] ?: run {
                    issues += reader.issue(Kind.UnknownVariable, file, "Unknown variable $name", line)
                        .copy(systemId = system.name)
                    ""
                }
            }
    }
}
