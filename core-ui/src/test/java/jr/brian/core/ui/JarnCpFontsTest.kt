package jr.brian.core.ui

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import jr.brian.core.ui.theme.JarnCpFontKey
import jr.brian.core.ui.theme.JarnType
import jr.brian.core.ui.theme.jarnCpFontFace
import jr.brian.core.ui.theme.jarnTypeWithFonts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class JarnCpFontsTest {

    private val base = JarnType()

    private val contractKeys = listOf(
        "condensed", "condensed_display", "mono", "segment", "rounded", "stencil", "comic", "chrome_wide", "serif",
    )

    @Test
    fun everyContractKeyResolves() {
        assertEquals(contractKeys.sorted(), JarnCpFontKey.entries.map { it.key }.sorted())
        contractKeys.forEach {
            assertEquals(it, JarnCpFontKey.fromKey(it)?.key)
            assertEquals(it, JarnCpFontKey.fromKey("bundled:$it")?.key)
        }
    }

    @Test
    fun everyKeyButSegmentResolvesToAFace() {
        contractKeys.filter { it != "segment" }.forEach { assertNotNull(it, jarnCpFontFace(it)) }
    }

    @Test
    fun segmentIsNeverUsedForText() {
        assertNull(jarnCpFontFace("segment"))
        assertEquals(base, jarnTypeWithFonts("bundled:segment", "segment"))
    }

    @Test
    fun unknownOrNullKeysFallBackToJarn() {
        assertNull(jarnCpFontFace("pixel_display"))
        assertNull(jarnCpFontFace("user:abc"))
        assertNull(JarnCpFontKey.fromKey(null))
        assertEquals(base, jarnTypeWithFonts(null, null))
        assertEquals(base, jarnTypeWithFonts("nope", "MONO"))
    }

    @Test
    fun captionsStayJarnForEveryKey() {
        contractKeys.forEach { key ->
            val type = jarnTypeWithFonts(key, key)
            assertEquals(base.caption, type.caption)
            assertEquals(base.captionStrong, type.captionStrong)
        }
    }

    @Test
    fun displayFontDrivesHeadingsOnly() {
        val type = jarnTypeWithFonts("chrome_wide", null)
        val michroma = jarnCpFontFace("chrome_wide")!!.family
        assertEquals(michroma, type.display.fontFamily)
        assertEquals(michroma, type.title.fontFamily)
        assertEquals(michroma, type.heading.fontFamily)
        assertEquals(base.label, type.label)
        assertEquals(base.labelStrong, type.labelStrong)
        assertEquals(base.body, type.body)
    }

    @Test
    fun labelFontDrivesLabelsOnly() {
        val type = jarnTypeWithFonts(null, "rounded")
        val baloo = jarnCpFontFace("rounded")!!.family
        assertEquals(baloo, type.label.fontFamily)
        assertEquals(baloo, type.labelStrong.fontFamily)
        assertEquals(FontWeight.ExtraBold, type.labelStrong.fontWeight)
        assertEquals(base.display, type.display)
        assertEquals(base.body, type.body)
    }

    @Test
    fun bodySwitchesOnlyForTextFaces() {
        assertEquals(base.body, jarnTypeWithFonts("rounded", "stencil").body)
        assertEquals(base.body, jarnTypeWithFonts("chrome_wide", "comic").body)
        val mono = jarnTypeWithFonts("mono", "mono")
        assertEquals(jarnCpFontFace("mono")!!.family, mono.body.fontFamily)
        assertEquals(16f * 0.88f, mono.body.fontSize.value, 0.01f)
        val serif = jarnTypeWithFonts("serif", null)
        assertEquals(jarnCpFontFace("serif")!!.family, serif.body.fontFamily)
    }

    @Test
    fun scaleAppliesToSizeAndLineHeight() {
        val type = jarnTypeWithFonts("chrome_wide", "mono")
        assertEquals(56f * 0.56f, type.display.fontSize.value, 0.01f)
        assertEquals(58f * 0.56f, type.display.lineHeight.value, 0.01f)
        assertEquals(32f * 0.62f, type.title.fontSize.value, 0.01f)
        assertEquals(24f * 0.66f, type.heading.fontSize.value, 0.01f)
        assertEquals(14f * 0.9f, type.label.fontSize.value, 0.01f)
    }

    @Test
    fun monoHeadingsUseBoldAndLabelsStepUpOnFocus() {
        val type = jarnTypeWithFonts("mono", "mono")
        assertEquals(FontWeight.Bold, type.display.fontWeight)
        assertEquals(FontWeight.Normal, type.label.fontWeight)
        assertEquals(FontWeight.Bold, type.labelStrong.fontWeight)
    }

    @Test
    fun condensedKeepsJarnHeadingWeightsAndSizes() {
        val type = jarnTypeWithFonts("condensed", null)
        assertEquals(base.display.fontWeight, type.display.fontWeight)
        assertEquals(base.title.fontWeight, type.title.fontWeight)
        assertEquals(base.display.fontSize, type.display.fontSize)
        assertEquals(FontWeight.ExtraBold, jarnTypeWithFonts("condensed_display", null).display.fontWeight)
    }

    @Test
    fun importedFamilyUsedOnlyWhenKeyDoesNotResolve() {
        val imported = FontFamily.Cursive
        val type = jarnTypeWithFonts("user:abc", null, importedDisplay = imported, importedLabel = imported)
        assertEquals(imported, type.display.fontFamily)
        assertEquals(base.display.fontSize, type.display.fontSize)
        assertEquals(base.display.fontWeight, type.display.fontWeight)
        assertEquals(imported, type.label.fontFamily)
        assertEquals(base.body, type.body)
        val bundled = jarnTypeWithFonts("stencil", null, importedDisplay = imported)
        assertNotEquals(imported, bundled.display.fontFamily)
        assertEquals(14.sp, jarnTypeWithFonts(null, null, importedDisplay = imported).label.fontSize)
    }
}
