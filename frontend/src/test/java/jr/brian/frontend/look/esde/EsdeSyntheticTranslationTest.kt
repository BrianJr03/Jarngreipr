package jr.brian.frontend.look.esde

import android.app.Application
import jr.brian.core.model.theme.JarnThemeDecodeResult
import jr.brian.core.model.theme.JarnThemeFormat
import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemeColor
import jr.brian.core.model.theme.ThemeColors
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Every synthetic theme through parser and translator, pinned as a `theme.json` and `report.txt` golden. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EsdeSyntheticTranslationTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val prettyJson = Json(from = JarnThemeFormat.json) { prettyPrint = true }

    private val options = EsdeTranslateOptions(
        fallbackColors = ThemeColors(
            bg = ThemeColor(0xFF000000.toInt()), surface = ThemeColor(0xFF111111.toInt()),
            raised = ThemeColor(0xFF222222.toInt()), textHigh = ThemeColor(0xFFFFFFFF.toInt()),
            textMid = ThemeColor(0xFFDDDDDD.toInt()), textLow = ThemeColor(0xFFBBBBBB.toInt()),
            accent = ThemeColor(0xFFFF8800.toInt()), danger = ThemeColor(0xFFFF4444.toInt()),
            success = ThemeColor(0xFF44FF44.toInt()),
        ),
        fallbackName = "Imported",
        systemsTopClear = 0.2f,
    )

    private fun translate(theme: EsdeSyntheticTheme): EsdeTranslation =
        EsdeThemeParser().translate(theme.install(temp.newFolder()), theme.systems, theme.selection, options)

    @Test
    fun `every fixture matches its translator goldens`() {
        val failures = EsdeSyntheticTheme.entries.mapNotNull { theme ->
            runCatching {
                val result = translate(theme)
                EsdeFixtureGoldens.assertTranslated(
                    theme,
                    prettyJson.encodeToString(JarnThemeSpec.serializer(), result.spec),
                    result.toGoldenText(theme),
                )
            }.exceptionOrNull()
        }
        failures.firstOrNull()?.let { first ->
            throw AssertionError(failures.joinToString("\n") { it.message.orEmpty() }, first)
        }
    }

    @Test
    fun `every translated fixture is a loadable theme file`() {
        EsdeSyntheticTheme.entries.forEach { theme ->
            val spec = translate(theme).spec
            assertEquals(theme.id, JarnThemeDecodeResult.Decoded(spec), JarnThemeFormat.decode(JarnThemeFormat.encode(spec)))
        }
    }

    @Test
    fun `hostile theme leaves no escaped asset or empty color behind`() {
        val result = translate(EsdeSyntheticTheme.Hostile)

        assertTrue(result.assets.none { it.startsWith("..") || it.startsWith("/") })
        assertTrue(result.colorCandidates.none { it.value.isBlank() })
    }
}

private fun EsdeTranslation.toGoldenText(theme: EsdeSyntheticTheme): String = buildString {
    appendLine("# ES-DE translator report: ${theme.id}")
    section("translated", report.translated)
    section("adjusted", report.adjusted)
    section("notUsed", report.notUsed)
    appendLine("[assets]")
    assets.forEach { appendLine("  $it") }
    appendLine("[colors]")
    colorCandidates.forEach { appendLine("  ${it.view.xmlName} ${it.element.type} ${it.element.name} ${it.property}=${it.value}") }
}

private fun StringBuilder.section(title: String, items: List<EsdeReportItem>) {
    appendLine("[$title]")
    items.forEach { item ->
        val view = item.view?.xmlName ?: "theme"
        val element = item.element?.let { "${it.type} ${it.name}" } ?: "-"
        appendLine("  $view $element${item.note?.let { ": $it" } ?: ""}")
    }
}
