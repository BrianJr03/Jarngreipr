package jr.brian.jarnfe.input

import jr.brian.core.ui.components.JarnHint
import jr.brian.core.ui.components.JarnKey
import org.junit.Assert.assertEquals
import org.junit.Test

class JarnHintSplitTest {
    private fun hint(key: JarnKey) = JarnHint(key, key.name)

    @Test
    fun `system and tab hints move to the trailing group in Start, Select, tabs order`() {
        val hints = listOf(JarnKey.A, JarnKey.L1R1, JarnKey.B, JarnKey.Select, JarnKey.X, JarnKey.Start, JarnKey.Y).map(::hint)

        val (trailing, leading) = jarnSplitTrailingHints(hints)

        assertEquals(listOf(JarnKey.Start, JarnKey.Select, JarnKey.L1R1), trailing.map { it.key })
        assertEquals(listOf(JarnKey.A, JarnKey.B, JarnKey.X, JarnKey.Y), leading.map { it.key })
    }

    @Test
    fun `bottom screen tab keys stay with the leading hints`() {
        val hints = listOf(JarnKey.A, JarnKey.L2R2).map(::hint)

        val (trailing, leading) = jarnSplitTrailingHints(hints)

        assertEquals(emptyList<JarnKey>(), trailing.map { it.key })
        assertEquals(listOf(JarnKey.A, JarnKey.L2R2), leading.map { it.key })
    }
}
