package jr.brian.frontend.look.themes

/** The largest SVG a theme may carry. Logos are a few KB; anything near this is a document, not art. */
const val MAX_THEME_SVG_BYTES = 1024 * 1024

private val SvgRoot = Regex("<svg[\\s/>]", RegexOption.IGNORE_CASE)
private val Forbidden = listOf(
    Regex("<!DOCTYPE", RegexOption.IGNORE_CASE),
    Regex("<!ENTITY", RegexOption.IGNORE_CASE),
    Regex("<script", RegexOption.IGNORE_CASE),
    Regex("<foreignObject", RegexOption.IGNORE_CASE),
    Regex("<\\?xml-stylesheet", RegexOption.IGNORE_CASE),
    Regex("@import", RegexOption.IGNORE_CASE),
    Regex("\\son[a-z]+\\s*=", RegexOption.IGNORE_CASE),
)
private val Reference = Regex("""(?:xlink:)?href\s*=\s*(["'])(.*?)\1""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val CssUrl = Regex("""url\(\s*(["']?)(.*?)\1\s*\)""", RegexOption.IGNORE_CASE)
private val DataImage = Regex("^data:image/(png|jpe?g|gif|webp);", RegexOption.IGNORE_CASE)

/**
 * An SVG Jarn can draw without reaching anything outside it: UTF-8 text with an `<svg>` root
 * under [MAX_THEME_SVG_BYTES], no DTD, entities, scripts, event handlers, foreign HTML or
 * stylesheet imports, and every link or CSS `url()` either a local `#fragment` or an embedded
 * raster `data:` image. Shared by `.jarntheme` files and ES-DE imports so both accept the same files.
 */
fun isSafeThemeSvg(bytes: ByteArray): Boolean {
    if (bytes.isEmpty() || bytes.size > MAX_THEME_SVG_BYTES) return false
    val text = runCatching { Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString() }.getOrNull()
        ?: return false
    if (!SvgRoot.containsMatchIn(text) || Forbidden.any { it.containsMatchIn(text) }) return false
    val links = Reference.findAll(text).map { it.groupValues[2] } + CssUrl.findAll(text).map { it.groupValues[2] }
    return links.all { it.isLocalReference() }
}

private fun String.isLocalReference(): Boolean {
    val value = trim()
    return value.startsWith("#") || DataImage.containsMatchIn(value)
}
