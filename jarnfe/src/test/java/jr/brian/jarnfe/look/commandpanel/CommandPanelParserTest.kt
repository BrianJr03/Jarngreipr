package jr.brian.jarnfe.look.commandpanel

import android.app.Application
import android.content.Context
import android.database.MatrixCursor
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

private val COLUMNS = CommandPanelThemeContract.COLUMNS.toTypedArray()
private val GOOD_COLORS: List<Any> = List(14) { 0xFF102030.toInt() + it }
private val SAMPLE = requireNotNull(commandPanelPaletteOf(1, 1, GOOD_COLORS.map { (it as Int).toLong() }))

private fun cursorOf(vararg rows: List<Any?>, columns: Array<String> = COLUMNS) =
    MatrixCursor(columns).apply { rows.forEach { addRow(it.toTypedArray()) } }

private fun goodRow(version: Any? = 1, isDark: Any? = 1, colors: List<Any?> = GOOD_COLORS) =
    listOf(version, isDark) + colors

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CommandPanelParserTest {
    @Test
    fun `valid row parses every role by name`() {
        val palette = requireNotNull(
            parseCommandPanelRow(cursorOf(goodRow().reversed(), columns = COLUMNS.reversedArray()))
        )
        assertTrue(palette.isDark)
        assertEquals(GOOD_COLORS[0], palette.bg)
        assertEquals(GOOD_COLORS[13], palette.success)
    }

    @Test
    fun `row count other than one is rejected`() {
        assertNull(parseCommandPanelRow(cursorOf()))
        assertNull(parseCommandPanelRow(cursorOf(goodRow(), goodRow())))
    }

    @Test
    fun `unsupported version is rejected`() {
        assertNull(parseCommandPanelRow(cursorOf(goodRow(version = 2))))
        assertNull(parseCommandPanelRow(cursorOf(goodRow(version = 0))))
    }

    @Test
    fun `is dark outside zero and one is rejected`() {
        assertNull(parseCommandPanelRow(cursorOf(goodRow(isDark = 2))))
    }

    @Test
    fun `non integer columns are rejected`() {
        assertNull(parseCommandPanelRow(cursorOf(goodRow(version = "1"))))
        assertNull(parseCommandPanelRow(cursorOf(goodRow(colors = GOOD_COLORS.dropLast(1) + "red"))))
        assertNull(parseCommandPanelRow(cursorOf(goodRow(colors = GOOD_COLORS.dropLast(1) + 1.5f))))
        assertNull(parseCommandPanelRow(cursorOf(goodRow(colors = GOOD_COLORS.dropLast(1) + listOf(null)))))
    }

    @Test
    fun `color outside int range is rejected`() {
        assertNull(parseCommandPanelRow(cursorOf(goodRow(colors = GOOD_COLORS.dropLast(1) + 0x1FFFFFFFFL))))
    }

    @Test
    fun `missing renamed or extra column is rejected`() {
        val missing = COLUMNS.dropLast(1).toTypedArray()
        assertNull(parseCommandPanelRow(cursorOf(goodRow().dropLast(1), columns = missing)))
        val renamed = (COLUMNS.dropLast(1) + "other").toTypedArray()
        assertNull(parseCommandPanelRow(cursorOf(goodRow(), columns = renamed)))
        val extra = COLUMNS + "extra"
        assertNull(parseCommandPanelRow(cursorOf(goodRow() + 1, columns = extra)))
    }

    @Test
    fun `colors are forced opaque`() {
        val translucent = GOOD_COLORS.map { (it as Int) and 0x00FFFFFF }
        val palette = requireNotNull(parseCommandPanelRow(cursorOf(goodRow(colors = translucent))))
        assertEquals(0xFF, palette.bg ushr 24)
        assertEquals(0xFF, palette.success ushr 24)
        assertEquals(0xFF, palette.accentSoft ushr 24)
    }

    @Test
    fun `accent soft keeps its tint by flattening onto surface`() {
        assertEquals(0xFF808080.toInt(), flattenOnto(0x80FFFFFF.toInt(), 0xFF000000.toInt()))
        assertEquals(0xFF123456.toInt(), flattenOnto(0x00FFFFFF, 0xFF123456.toInt()))
        assertEquals(0xFFABCDEF.toInt(), flattenOnto(0xFFABCDEF.toInt(), 0xFF000000.toInt()))
    }

    @Test
    fun `cache text round trips and bad text is refused`() {
        assertEquals(SAMPLE, decodeCommandPanelPalette(encodeCommandPanelPalette(SAMPLE)))
        assertNull(decodeCommandPanelPalette(""))
        assertNull(decodeCommandPanelPalette("1,1,2"))
        assertNull(decodeCommandPanelPalette(encodeCommandPanelPalette(SAMPLE).replaceFirst("1,", "2,")))
        assertNull(decodeCommandPanelPalette(encodeCommandPanelPalette(SAMPLE) + ",x"))
    }

    @Test
    fun `cache file survives a round trip, clears, and tolerates corruption`() {
        val dir = ApplicationProvider.getApplicationContext<Context>().noBackupFilesDir
        val cache = CommandPanelPaletteCache(dir)
        cache.write(SAMPLE)
        assertEquals(SAMPLE, cache.read())
        cache.clear()
        assertNull(cache.read())
        File(dir, "command_panel_palette").writeText("garbage")
        assertNull(cache.read())
        cache.clear()
    }
}
