package jr.brian.core.ui

import androidx.compose.ui.unit.dp
import jr.brian.core.ui.focus.JarnFocusFloatAmplitude
import jr.brian.core.ui.focus.JarnFocusFloatMaxGrowth
import jr.brian.core.ui.focus.JarnFocusLift
import jr.brian.core.ui.focus.JarnFocusStyle
import jr.brian.core.ui.focus.jarnPulseScale
import jr.brian.core.ui.theme.JarnFocusMaxReach
import jr.brian.core.ui.theme.JarnPulseExtra
import jr.brian.core.ui.theme.JarnSwitchCue
import jr.brian.core.ui.theme.JarnSwitchRhythm
import jr.brian.core.ui.theme.jarnMarkWidth
import jr.brian.core.ui.theme.jarnPulseAdmitsFocus
import jr.brian.core.ui.theme.jarnPulseStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnSwitchCueTest {
    @Test
    fun `ids are stable and unknown ids fall back to pulse`() {
        assertEquals(listOf("pulse", "dim", "tab_mark"), JarnSwitchCue.entries.map { it.id })
        JarnSwitchCue.entries.forEach { assertEquals(it, JarnSwitchCue.fromId(it.id)) }
        assertEquals(JarnSwitchCue.Pulse, JarnSwitchCue.fromId("future"))
        assertEquals(JarnSwitchCue.Pulse, JarnSwitchCue.fromId(null))
    }

    @Test
    fun `strength is none under 150ms and full above 700ms`() {
        assertEquals(0f, jarnPulseStrength(0L), 0f)
        assertEquals(0f, jarnPulseStrength(150L), 0f)
        assertEquals(0.5f, jarnPulseStrength(425L), 0.001f)
        assertEquals(1f, jarnPulseStrength(700L), 0f)
        assertEquals(1f, jarnPulseStrength(60_000L), 0f)
    }

    @Test
    fun `the rhythm measures from the last switch on either screen`() {
        val rhythm = JarnSwitchRhythm()
        assertEquals(1f, rhythm.strengthAt(1_000L), 0f)
        assertEquals(0f, rhythm.strengthAt(1_100L), 0f)
        assertEquals(1f, rhythm.strengthAt(2_000L), 0f)
    }

    @Test
    fun `focus moved after the handoff never joins a pulse`() {
        assertTrue(jarnPulseAdmitsFocus(16L))
        assertFalse(jarnPulseAdmitsFocus(200L))
    }

    @Test
    fun `ring controls keep their size`() {
        assertEquals(0f, pulse(JarnFocusStyle.Ring, baseScale = 1f, side = 100, lift = 0f), 0f)
    }

    @Test
    fun `a tile passes rest by the pulse on each side`() {
        val added = pulse(JarnFocusStyle.Scale, baseScale = 1.08f, side = 100, lift = 0f)
        assertEquals(JarnPulseExtra.value, added * 100 / 2f, 0.001f)
    }

    @Test
    fun `a floating tile at full reach stays within its slot limit`() {
        val side = 400
        val growth = JarnFocusFloatMaxGrowth.value
        val lift = -(JarnFocusLift.value + JarnFocusFloatAmplitude.value)
        val base = 1f + 2f * growth / side
        val added = pulse(JarnFocusStyle.Float, base, side, lift)
        val reach = (base + added - 1f) * side / 2f - lift
        assertTrue(reach <= JarnFocusMaxReach.value + 0.001f)
    }

    @Test
    fun `the mark opens from 40 percent and keeps its width fading out`() {
        assertEquals(0.4f, jarnMarkWidth(1f, 0f, rising = true, reduced = false), 0.001f)
        assertEquals(0.7f, jarnMarkWidth(0.4f, 0.5f, rising = true, reduced = false), 0.001f)
        assertEquals(1f, jarnMarkWidth(1f, 0.5f, rising = false, reduced = false), 0.001f)
        assertEquals(1f, jarnMarkWidth(0.4f, 0.1f, rising = true, reduced = true), 0f)
    }

    private fun pulse(style: JarnFocusStyle, baseScale: Float, side: Int, lift: Float) = jarnPulseScale(
        style = style,
        overshoot = 1f,
        focusProgress = 1f,
        baseScale = baseScale,
        maxSidePx = side,
        translationPx = lift,
        extraPx = JarnPulseExtra.value,
        reachLimitPx = JarnFocusMaxReach.value,
    )
}
