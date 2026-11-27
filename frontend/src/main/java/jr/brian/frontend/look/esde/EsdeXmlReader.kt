package jr.brian.frontend.look.esde

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.IOException
import java.io.StringReader

internal const val ESDE_MAX_FILE_BYTES = 512L * 1024
internal const val ESDE_MAX_TOTAL_BYTES = 8L * 1024 * 1024

/** A parsed XML element. [text] is the trimmed text directly inside it. */
internal data class EsdeXmlNode(
    val name: String,
    val attributes: Map<String, String>,
    val text: String,
    val children: List<EsdeXmlNode>,
    val line: Int,
) {
    fun child(name: String): EsdeXmlNode? = children.firstOrNull { it.name == name }
    fun childrenNamed(name: String): List<EsdeXmlNode> = children.filter { it.name == name }

    /** ES-DE lets one tag name several things: `name="system, gamelist"`. */
    fun names(): List<String> =
        attributes["name"].orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
}

/**
 * Reads theme XML from inside [root] only. Each file is read once and cached, files over
 * [ESDE_MAX_FILE_BYTES] or past the [ESDE_MAX_TOTAL_BYTES] budget are skipped, and any
 * document type declaration is refused so entities never expand.
 */
internal class EsdeXmlReader(
    root: File,
    private val newParser: () -> XmlPullParser,
    private val report: (EsdeParseIssue) -> Unit,
) {
    val root: File = root.canonicalFile
    private val cache = HashMap<File, EsdeXmlNode?>()
    private var totalBytes = 0L

    /** [path] resolved against [baseDir] and kept inside the theme, or null after reporting why not. */
    fun resolveInside(baseDir: File, path: String, from: File, line: Int?): File? {
        val candidate = if (File(path).isAbsolute) File(path) else File(baseDir, path)
        val resolved = try {
            candidate.canonicalFile
        } catch (e: IOException) {
            null
        }
        if (resolved == null || !isInside(resolved)) {
            report(issue(EsdeParseIssue.Kind.PathOutsideTheme, from, "Path leaves the theme folder: $path", line))
            return null
        }
        return resolved
    }

    fun isInside(file: File): Boolean = file == root || file.path.startsWith(root.path + File.separator)

    fun relative(file: File): String = file.relativeTo(root).invariantSeparatorsPath

    /** The document's root element, or null after reporting why it can't be read. */
    fun read(file: File): EsdeXmlNode? = cache.getOrPut(file) { load(file) }

    private fun load(file: File): EsdeXmlNode? {
        if (!file.isFile) {
            report(issue(EsdeParseIssue.Kind.Unreadable, file, "Not a file"))
            return null
        }
        val size = file.length()
        if (size > ESDE_MAX_FILE_BYTES) {
            report(issue(EsdeParseIssue.Kind.FileTooLarge, file, "File is $size bytes"))
            return null
        }
        if (totalBytes + size > ESDE_MAX_TOTAL_BYTES) {
            report(issue(EsdeParseIssue.Kind.TotalSizeExceeded, file, "Theme passed $ESDE_MAX_TOTAL_BYTES bytes"))
            return null
        }
        totalBytes += size
        val text = try {
            file.readText()
        } catch (e: IOException) {
            report(issue(EsdeParseIssue.Kind.Unreadable, file, e.message ?: "Read failed"))
            return null
        }
        if (text.contains("<!DOCTYPE", ignoreCase = true) || text.contains("<!ENTITY", ignoreCase = true)) {
            report(issue(EsdeParseIssue.Kind.DocTypeRefused, file, "Document type declarations aren't read"))
            return null
        }
        return try {
            parse(text)
        } catch (e: XmlPullParserException) {
            report(issue(EsdeParseIssue.Kind.Malformed, file, e.message ?: "Malformed XML", e.lineNumber))
            null
        } catch (e: IOException) {
            report(issue(EsdeParseIssue.Kind.Malformed, file, e.message ?: "Malformed XML"))
            null
        }
    }

    private fun parse(text: String): EsdeXmlNode? {
        val parser = newParser()
        runCatching { parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false) }
        parser.setInput(StringReader(text))
        val stack = ArrayDeque<NodeBuilder>()
        var rootNode: EsdeXmlNode? = null
        var event = parser.next()
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> stack.addLast(NodeBuilder(parser))
                XmlPullParser.TEXT -> stack.lastOrNull()?.text?.append(parser.text)
                XmlPullParser.END_TAG -> {
                    val node = stack.removeLast().build()
                    val parent = stack.lastOrNull()
                    if (parent == null) rootNode = node else parent.children += node
                }
            }
            event = parser.next()
        }
        return rootNode
    }

    fun issue(kind: EsdeParseIssue.Kind, file: File, detail: String, line: Int? = null) = EsdeParseIssue(
        kind = kind,
        file = if (isInside(file)) relative(file) else file.name,
        detail = detail,
        line = line?.takeIf { it > 0 },
    )

    private class NodeBuilder(parser: XmlPullParser) {
        val name: String = parser.name
        val line: Int = parser.lineNumber
        val attributes: Map<String, String> =
            (0 until parser.attributeCount).associate { parser.getAttributeName(it) to parser.getAttributeValue(it) }
        val text = StringBuilder()
        val children = mutableListOf<EsdeXmlNode>()

        fun build() = EsdeXmlNode(name, attributes, text.toString().trim(), children.toList(), line)
    }
}
