package jr.brian.jarnfe.look.commandpanel

import android.app.Application
import android.database.MatrixCursor
import androidx.compose.ui.text.font.FontFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val FONT_ID = "e2bc6a1e-8ea0-44ae-82b1-f95fe51fb19f"
private val SHA = "ab".repeat(32)
private val COLUMNS = CommandPanelFontsContract.COLUMNS.toTypedArray()

private fun row(
    version: Any? = 2,
    lookId: Any? = "TERMINAL",
    display: Any? = "bundled:mono",
    displaySha: Any? = null,
    label: Any? = "user:$FONT_ID",
    labelSha: Any? = SHA,
) = listOf(version, lookId, display, displaySha, label, labelSha)

private fun cursorOf(vararg rows: List<Any?>, columns: Array<String> = COLUMNS) =
    MatrixCursor(columns).apply { rows.forEach { addRow(it.toTypedArray()) } }

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CommandPanelFontsTest {
    @Test
    fun `a valid row parses every role by name`() {
        val fonts = parseCommandPanelFontsRow(cursorOf(row().reversed(), columns = COLUMNS.reversedArray()))
        assertEquals(
            CommandPanelFonts("TERMINAL", CommandPanelFont.Bundled("mono"), CommandPanelFont.Imported(FONT_ID, SHA)),
            fonts,
        )
    }

    @Test
    fun `null roles mean no font`() {
        val fonts = requireNotNull(parseCommandPanelFontsRow(cursorOf(row(display = null, label = null, labelSha = null))))
        assertNull(fonts.display)
        assertNull(fonts.label)
        assertFalse(fonts.hasUsableFont)
    }

    @Test
    fun `rows that break the contract are rejected`() {
        assertNull(parseCommandPanelFontsRow(cursorOf()))
        assertNull(parseCommandPanelFontsRow(cursorOf(row(), row())))
        assertNull(parseCommandPanelFontsRow(cursorOf(row(version = 1))))
        assertNull(parseCommandPanelFontsRow(cursorOf(row(version = "2"))))
        assertNull(parseCommandPanelFontsRow(cursorOf(row(lookId = null))))
        assertNull(parseCommandPanelFontsRow(cursorOf(row(display = 7))))
        assertNull(parseCommandPanelFontsRow(cursorOf(row(labelSha = null))))
        assertNull(parseCommandPanelFontsRow(cursorOf(row(labelSha = "nothex"))))
        assertNull(parseCommandPanelFontsRow(cursorOf(row(label = "user:../../secret"))))
        assertNull(parseCommandPanelFontsRow(cursorOf(row().dropLast(1), columns = COLUMNS.dropLast(1).toTypedArray())))
    }

    @Test
    fun `an unknown prefix is no font, an unknown bundled key is kept but unusable`() {
        val fonts = requireNotNull(
            parseCommandPanelFontsRow(cursorOf(row(display = "cloud:x", label = "bundled:future", labelSha = null))),
        )
        assertNull(fonts.display)
        assertEquals(CommandPanelFont.Bundled("future"), fonts.label)
        assertFalse(fonts.hasUsableFont)
    }

    @Test
    fun `a digits-only face doesn't count as a font to match`() {
        assertFalse(CommandPanelFonts("SEGMENT_LCD", CommandPanelFont.Bundled("segment"), null).hasUsableFont)
        assertTrue(CommandPanelFonts("SEGMENT_LCD", CommandPanelFont.Bundled("segment"), CommandPanelFont.Bundled("mono")).hasUsableFont)
    }

    @Test
    fun `the cached row round trips`() {
        val fonts = requireNotNull(parseCommandPanelFontsRow(cursorOf(row())))
        assertEquals(fonts, decodeCommandPanelFonts(encodeCommandPanelFonts(fonts)))
        assertNull(decodeCommandPanelFonts("garbage"))
    }

    @Test
    fun `availability follows the row and names built-in looks`() {
        val name = { id: String -> if (id == "TERMINAL") "Terminal" else null }
        val fresh = { fonts: CommandPanelFonts -> fontMatchAvailability(CommandPanelFontsRead.Fresh(fonts), true, name) }
        assertEquals(
            FontMatchAvailability.Available("Terminal"),
            fresh(CommandPanelFonts("TERMINAL", CommandPanelFont.Bundled("mono"), null)),
        )
        assertEquals(FontMatchAvailability.NoFonts(null), fresh(CommandPanelFonts("custom:abc", null, null)))
        assertEquals(FontMatchAvailability.Checking, fontMatchAvailability(null, true, name))
        assertEquals(FontMatchAvailability.Waiting, fontMatchAvailability(CommandPanelFontsRead.Locked, true, name))
        assertEquals(FontMatchAvailability.Waiting, fontMatchAvailability(CommandPanelFontsRead.Stopped, true, name))
        assertEquals(FontMatchAvailability.Unverified, fontMatchAvailability(CommandPanelFontsRead.Untrusted, true, name))
        assertEquals(FontMatchAvailability.Hidden, fontMatchAvailability(CommandPanelFontsRead.Untrusted, false, name))
        assertEquals(FontMatchAvailability.Missing, fontMatchAvailability(CommandPanelFontsRead.Unsupported, true, name))
        assertEquals(FontMatchAvailability.Unreadable, fontMatchAvailability(CommandPanelFontsRead.Rejected, true, name))
    }

    @Test
    fun `built-in look ids have names, saved looks don't`() {
        assertTrue(commandPanelLookNameRes("E_INK") != null)
        assertNull(commandPanelLookNameRes("custom:abc"))
        assertNull(commandPanelLookNameRes("SOMETHING_NEW"))
    }

    @Test
    fun `an imported font fills its role, bundled keys win`() {
        val family = FontFamily.Cursive
        val fonts = CommandPanelFonts("custom:a", CommandPanelFont.Bundled("serif"), CommandPanelFont.Imported(FONT_ID, SHA))
        val type = buildCommandPanelFontMatch(fonts) { family }.type
        assertEquals(family, type.label.fontFamily)
        assertTrue(type.display.fontFamily != family)
    }
}
