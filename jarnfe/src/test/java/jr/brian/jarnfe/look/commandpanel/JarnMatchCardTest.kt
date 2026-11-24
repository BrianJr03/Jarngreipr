package jr.brian.jarnfe.look.commandpanel

import jr.brian.core.model.CommandPanelPalette
import jr.brian.core.ui.theme.JarnLooks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val Dark = CommandPanelPalette(
    isDark = true,
    bg = 0xFF101418.toInt(),
    rail = 0xFF14191E.toInt(),
    surface = 0xFF1A2026.toInt(),
    surfaceHigh = 0xFF232B33.toInt(),
    border = 0xFF39434D.toInt(),
    accent = 0xFF6FB6FF.toInt(),
    accentSoft = 0xFF25405A.toInt(),
    accentFocused = 0xFF9CCEFF.toInt(),
    onAccent = 0xFF08131E.toInt(),
    textPrimary = 0xFFF2F5F8.toInt(),
    textSecondary = 0xFFC9D2DB.toInt(),
    textTertiary = 0xFFAAB5C0.toInt(),
    danger = 0xFFFF8A80.toInt(),
    success = 0xFF7BE0A3.toInt(),
)
private val Light = Dark.copy(isDark = false)
private val Blank = Dark.copy(accent = 0)

class JarnMatchCardTest {
    private fun card(
        enabled: Boolean,
        availability: CommandPanelAvailability,
        probed: CommandPanelPalette? = null,
        applied: CommandPanelPalette? = null,
        wasShown: Boolean = false,
    ) = resolveJarnMatchCard(enabled, CommandPanelProbe(availability, probed), applied, JarnLooks.Grove, wasShown)

    @Test
    fun `off with a dark theme offers matching and previews it`() {
        val card = card(false, CommandPanelAvailability.Ready, Dark)
        assertEquals(JarnMatchCardState.Available, card.state)
        assertTrue(card.state.canTurnOn)
        assertNotNull(card.preview)
    }

    @Test
    fun `off with a light theme warns accent only`() {
        assertEquals(
            JarnMatchCardState.AvailableAccentOnly,
            card(false, CommandPanelAvailability.Ready, Light).state,
        )
    }

    @Test
    fun `on reports full and accent only matches`() {
        assertEquals(JarnMatchCardState.MatchingFull, card(true, CommandPanelAvailability.Ready, applied = Dark).state)
        assertEquals(
            JarnMatchCardState.MatchingAccentOnly,
            card(true, CommandPanelAvailability.Ready, applied = Light).state,
        )
    }

    @Test
    fun `on shows cached colors while the first check runs`() {
        assertEquals(JarnMatchCardState.MatchingFull, card(true, CommandPanelAvailability.Checking, applied = Dark).state)
    }

    @Test
    fun `on never claims a match the screens have not applied`() {
        val card = card(true, CommandPanelAvailability.Ready, probed = Dark, applied = null)
        assertEquals(JarnMatchCardState.Checking, card.state)
        assertFalse(card.state.isMatching)
        assertNull(card.preview)
    }

    @Test
    fun `missing hides the card when off`() {
        assertEquals(JarnMatchCardState.Hidden, card(false, CommandPanelAvailability.Missing).state)
    }

    @Test
    fun `missing shows unavailable when restored on`() {
        val card = card(true, CommandPanelAvailability.Missing, applied = Dark)
        assertEquals(JarnMatchCardState.Unavailable, card.state)
        assertNull(card.preview)
    }

    @Test
    fun `turning off an unavailable card keeps it for gamepad focus`() {
        val card = card(false, CommandPanelAvailability.Missing, wasShown = true)
        assertEquals(JarnMatchCardState.Unavailable, card.state)
        assertFalse(card.state.canTurnOn)
    }

    @Test
    fun `wrong signature and bad data block matching`() {
        listOf(
            CommandPanelAvailability.Untrusted to JarnMatchCardState.Unverified,
            CommandPanelAvailability.BadData to JarnMatchCardState.Unreadable,
        ).forEach { (availability, expected) ->
            val card = card(false, availability)
            assertEquals(expected, card.state)
            assertFalse(card.state.canTurnOn)
            assertNull(card.preview)
        }
    }

    @Test
    fun `blank colors read as bad data`() {
        assertEquals(JarnMatchCardState.Unreadable, card(false, CommandPanelAvailability.Ready, Blank).state)
    }

    @Test
    fun `stopped or locked waits and can still be turned on`() {
        val card = card(false, CommandPanelAvailability.Waiting)
        assertEquals(JarnMatchCardState.Waiting, card.state)
        assertTrue(card.state.canTurnOn)
    }

    @Test
    fun `off stays hidden until the first check lands`() {
        assertEquals(JarnMatchCardState.Hidden, card(false, CommandPanelAvailability.Checking).state)
    }

    @Test
    fun `reads map to the right availability`() {
        assertEquals(CommandPanelAvailability.Missing, CommandPanelRead.Untrusted.toProbe(false).availability)
        assertEquals(CommandPanelAvailability.Untrusted, CommandPanelRead.Untrusted.toProbe(true).availability)
        assertEquals(CommandPanelAvailability.BadData, CommandPanelRead.Rejected.toProbe(true).availability)
        assertEquals(CommandPanelAvailability.Waiting, CommandPanelRead.Stopped.toProbe(true).availability)
        assertEquals(Dark, CommandPanelRead.Fresh(Dark).toProbe(true).palette)
    }
}
