package jr.brian.frontend.look.esde

import java.io.File

/**
 * Writes synthetic ES-DE themes for tests. Never copy a real theme's XML in here: themes
 * carry their own licenses. [theme] wraps its body in `<theme>`; [capabilities] writes the
 * required `capabilities.xml`.
 */
class EsdeThemeFixture(val root: File) {

    fun file(path: String, content: String): File =
        File(root, path).apply {
            parentFile?.mkdirs()
            writeText(content)
        }

    fun theme(path: String = "theme.xml", body: String): File = file(path, "<theme>$body</theme>")

    fun capabilities(
        variants: List<String> = emptyList(),
        colorSchemes: List<String> = emptyList(),
        fontSizes: List<String> = emptyList(),
        aspectRatios: List<String> = emptyList(),
        themeName: String = "Fixture",
    ): File = file(
        "capabilities.xml",
        buildString {
            append("<themeCapabilities><themeName>$themeName</themeName>")
            aspectRatios.forEach { append("<aspectRatio>$it</aspectRatio>") }
            fontSizes.forEach { append("<fontSize>$it</fontSize>") }
            colorSchemes.forEach { append("""<colorScheme name="$it"><label>$it label</label></colorScheme>""") }
            variants.forEach { append("""<variant name="$it"><label>$it label</label></variant>""") }
            append("</themeCapabilities>")
        },
    )

    /** One view with one element, the most common test shape. */
    fun element(view: String, type: String, name: String, vararg properties: Pair<String, String>): String =
        """<view name="$view"><$type name="$name">""" +
            properties.joinToString("") { (key, value) -> "<$key>$value</$key>" } +
            "</$type></view>"
}
