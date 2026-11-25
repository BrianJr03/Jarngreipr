package jr.brian.jarnfe.look.commandpanel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnFontMatchTest {
    @Test
    fun `a readable Command Panel offers fonts`() {
        listOf(
            JarnMatchCardState.Available,
            JarnMatchCardState.AvailableAccentOnly,
            JarnMatchCardState.MatchingFull,
            JarnMatchCardState.MatchingAccentOnly,
        ).forEach {
            assertEquals(FontMatchAvailability.Available(), FontMatchAvailability.fromColorCard(it))
        }
    }

    @Test
    fun `unavailable color states carry over to fonts`() {
        assertEquals(FontMatchAvailability.Missing, FontMatchAvailability.fromColorCard(JarnMatchCardState.Unavailable))
        assertEquals(FontMatchAvailability.Unverified, FontMatchAvailability.fromColorCard(JarnMatchCardState.Unverified))
        assertEquals(FontMatchAvailability.Unreadable, FontMatchAvailability.fromColorCard(JarnMatchCardState.Unreadable))
        assertEquals(FontMatchAvailability.Hidden, FontMatchAvailability.fromColorCard(JarnMatchCardState.Hidden))
    }

    @Test
    fun `an off row can only turn on when fonts are available`() {
        assertTrue(resolveJarnFontMatchRow(false, FontMatchAvailability.Available()).canToggle)
        listOf(
            FontMatchAvailability.Checking,
            FontMatchAvailability.Waiting,
            FontMatchAvailability.NoFonts("Aurora Glass"),
            FontMatchAvailability.Missing,
            FontMatchAvailability.Unverified,
            FontMatchAvailability.Unreadable,
        ).forEach { assertFalse(it.toString(), resolveJarnFontMatchRow(false, it).canToggle) }
    }

    @Test
    fun `an on row can always be turned off`() {
        assertTrue(resolveJarnFontMatchRow(true, FontMatchAvailability.Unverified).canToggle)
        assertTrue(resolveJarnFontMatchRow(true, FontMatchAvailability.NoFonts()).canToggle)
    }

    @Test
    fun `an on row never hides when Command Panel is gone`() {
        val row = resolveJarnFontMatchRow(true, FontMatchAvailability.Hidden)

        assertTrue(row.isShown)
        assertEquals(FontMatchAvailability.Missing, row.availability)
    }

    @Test
    fun `an off row hides with the colors card`() {
        assertFalse(resolveJarnFontMatchRow(false, FontMatchAvailability.Hidden).isShown)
    }
}
