package jr.brian.frontend.look.esde

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import java.io.File

private val GOLDENS_DIR = File("src/test/resources/esde-goldens")

/** The parser's golden for every fixture. */
const val PARSER_GOLDEN = "parser.txt"

/** The translator (#574) adds these per fixture: the Jarn theme it writes and its import report. */
const val TRANSLATOR_THEME_GOLDEN = "theme.json"
const val TRANSLATOR_REPORT_GOLDEN = "report.txt"

/**
 * Checked-in expectations per fixture in `src/test/resources/esde-goldens/<fixture>/<name>`.
 * A missing golden is written from this run and the test fails, so a new golden is always a
 * reviewed change: delete the file and run again to re-record it.
 */
object EsdeFixtureGoldens {

    fun assertGolden(theme: EsdeSyntheticTheme, name: String, actual: String) {
        val file = File(GOLDENS_DIR, "${theme.id}/$name")
        val text = actual.trimEnd() + "\n"
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.writeText(text)
            fail("Wrote a new golden to ${file.absolutePath}. Review it and run again.")
        }
        assertEquals("Golden $name changed for ${theme.id}", file.readText(), text)
    }

    /**
     * The translator's hook: call once per fixture with the `theme.json` and report it wrote.
     * Both go through [assertGolden], so the first run records them for review.
     */
    fun assertTranslated(theme: EsdeSyntheticTheme, themeJson: String, report: String) {
        assertGolden(theme, TRANSLATOR_THEME_GOLDEN, themeJson)
        assertGolden(theme, TRANSLATOR_REPORT_GOLDEN, report)
    }
}

/** A stable, line-per-fact rendering of a parse, for goldens. Elements keep the parser's merge order. */
fun EsdeThemeModel.toGoldenText(theme: EsdeSyntheticTheme): String = buildString {
    appendLine("# ES-DE parser golden: ${theme.id}")
    appendLine("themeName=${capabilities.themeName}")
    appendLine("variants=${capabilities.variants.joinToString { it.golden() }}")
    appendLine("colorSchemes=${capabilities.colorSchemes.joinToString { it.golden() }}")
    appendLine("fontSizes=${capabilities.fontSizes.joinToString()}")
    appendLine("aspectRatios=${capabilities.aspectRatios.joinToString()}")
    appendLine(
        "selection=variant:${selection.variant} colorScheme:${selection.colorScheme} " +
            "fontSize:${selection.fontSize} aspectRatio:${selection.aspectRatio}",
    )
    appendLine("legacy=$isLegacy")
    systems.values.forEach { system ->
        appendLine("[system ${system.system.name}]")
        system.views.forEach { (view, model) ->
            appendLine("  view ${view.xmlName}")
            model.elements.forEach { (key, properties) ->
                val props = properties.entries.joinToString("; ") { (name, value) -> "$name=$value" }
                appendLine("    ${key.type} ${key.name}: $props")
            }
        }
    }
    appendLine("[issues]")
    issues.forEach { issue ->
        val where = issue.file + (issue.line?.let { ":$it" } ?: "")
        appendLine("  ${issue.kind} $where${issue.systemId?.let { " ($it)" } ?: ""}: ${issue.detail}")
    }
}

private fun EsdeNamedOption.golden(): String =
    name + (label?.let { " \"$it\"" } ?: "") + if (selectable) "" else " (hidden)"
