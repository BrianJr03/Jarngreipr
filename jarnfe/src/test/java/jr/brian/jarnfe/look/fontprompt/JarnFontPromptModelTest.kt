package jr.brian.jarnfe.look.fontprompt

import jr.brian.core.ui.theme.JarnType
import jr.brian.jarnfe.look.commandpanel.FontMatchAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JarnFontPromptModelTest {
    private val sample = JarnType()

    private fun offer(
        shown: Boolean = false,
        fontsOn: Boolean = false,
        tourDone: Boolean = true,
        availability: FontMatchAvailability = FontMatchAvailability.Available("Terminal"),
        sampleType: JarnType? = sample,
    ) = resolveFontPromptOffer(shown, fontsOn, tourDone, JarnFontPromptInput(availability, sampleType))

    @Test
    fun `a font look with the toggle off offers the prompt`() {
        assertEquals(JarnFontPromptOffer("Terminal", sample), offer())
    }

    @Test
    fun `an answered prompt never shows again, whatever the look`() {
        listOf("Terminal", "E-Ink", "Comic Panel").forEach { look ->
            assertNull(look, offer(shown = true, availability = FontMatchAvailability.Available(look)))
        }
    }

    @Test
    fun `no prompt while fonts are already on`() {
        assertNull(offer(fontsOn = true))
    }

    @Test
    fun `no prompt until the tour is done`() {
        assertNull(offer(tourDone = false))
    }

    @Test
    fun `no prompt without a detected font look`() {
        assertNull(offer(availability = FontMatchAvailability.Available()))
        assertNull(offer(availability = FontMatchAvailability.NoFonts("Aurora Glass")))
        assertNull(offer(availability = FontMatchAvailability.Missing))
        assertNull(offer(availability = FontMatchAvailability.Hidden))
        assertNull(offer(sampleType = null))
    }
}
