package jr.brian.jarnfe.themes

import android.app.Application
import jr.brian.core.model.theme.JarnThemeFormat
import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemeColor
import jr.brian.core.model.theme.ThemeColors
import jr.brian.core.model.theme.ThemeMeta
import jr.brian.core.model.theme.ThemeSource
import jr.brian.core.model.theme.ThemeTokens
import jr.brian.jarnfe.R
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnThemeShareTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val colors = ThemeColors(
        bg = ThemeColor(0xFF000000.toInt()), surface = ThemeColor(0xFF111111.toInt()),
        raised = ThemeColor(0xFF222222.toInt()), textHigh = ThemeColor(0xFFFFFFFF.toInt()),
        textMid = ThemeColor(0xFFDDDDDD.toInt()), textLow = ThemeColor(0xFFBBBBBB.toInt()),
        accent = ThemeColor(0xFFFF8800.toInt()), danger = ThemeColor(0xFFFF4444.toInt()),
        success = ThemeColor(0xFF44FF44.toInt()),
    )

    private fun installThemes(vararg themes: Pair<String, ThemeMeta>) = runBlocking {
        val root = temp.newFolder("themes")
        themes.forEach { (id, meta) ->
            File(root, id).mkdirs()
            File(root, "$id/theme.json").writeText(JarnThemeFormat.encode(JarnThemeSpec(meta = meta, tokens = ThemeTokens(colors))))
        }
        JarnImportedThemes.refresh(root)
    }

    @After
    fun clear() = runBlocking { JarnImportedThemes.refresh(temp.newFolder("empty")) }

    @Test
    fun `saved themes share unless an import's license forbids it`() {
        installThemes(
            "mine" to ThemeMeta(name = "Mine"),
            "open" to ThemeMeta(name = "Open", author = "Someone", license = "CC BY-SA 4.0", source = ThemeSource.EsDe),
            "closed" to ThemeMeta(name = "Closed", license = "CC BY-NC-SA 4.0", source = ThemeSource.EsDe),
            "unknown" to ThemeMeta(name = "Unknown", source = ThemeSource.EsDe),
        )

        assertEquals(JarnThemeShareCheck.Shareable, canShare("mine"))
        assertEquals(JarnThemeShareCheck.Shareable, canShare("open"))
        assertEquals(JarnThemeShareCheck.NotShareable(R.string.jarnfe_theme_share_license), canShare("closed"))
        assertEquals(JarnThemeShareCheck.NotShareable(R.string.jarnfe_theme_share_license), canShare("unknown"))
    }

    @Test
    fun `built-in layouts and missing themes have nothing to share`() {
        assertEquals(JarnThemeShareCheck.NotShareable(R.string.jarnfe_theme_share_built_in), canShare(JarnThemeRegistry.Grid.id))
        assertEquals(
            JarnThemeShareCheck.NotShareable(R.string.jarnfe_theme_share_built_in),
            canShare(JarnThemeRegistry.templates.first().id),
        )
        assertEquals(JarnThemeShareCheck.NotShareable(R.string.jarnfe_theme_share_missing), canShare("gone"))
    }
}
