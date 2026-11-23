package jr.brian.core.ui.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnBalancedTitleTest {
    private val width: (String, Float) -> Float = { text, font -> text.length * font / 2f }
    private val lineHeight: (Float) -> Float = { it }

    @Test
    fun `splits at the most even word break`() {
        assertEquals(
            listOf("New Super", "Mario Bros. Wii").map { it.length }.max(),
            jarnBalancedLines("New Super Mario Bros. Wii") { it.length.toFloat() }.maxOf { it.length },
        )
    }

    @Test
    fun `never splits inside a word`() {
        val lines = jarnBalancedLines("Xenoblade Chronicles") { it.length.toFloat() }

        assertEquals(listOf("Xenoblade", "Chronicles"), lines)
    }

    @Test
    fun `single word stays on one line`() {
        assertEquals(listOf("Okami"), jarnBalancedLines("Okami") { it.length.toFloat() })
    }

    @Test
    fun `short title keeps the largest size on one line`() {
        val fit = jarnFitTitle("Okami", 56f, 500f, 120f, width, lineHeight)

        assertEquals(listOf("Okami"), fit.lines)
        assertEquals(56f, fit.fontSizeSp, 0f)
    }

    @Test
    fun `long title uses two lines before shrinking`() {
        val fit = jarnFitTitle("New Super Mario Bros. Wii", 56f, 460f, 120f, width, lineHeight)

        assertEquals(2, fit.lines.size)
        assertEquals(56f, fit.fontSizeSp, 0f)
    }

    @Test
    fun `title shrinks until both lines fit the height`() {
        val fit = jarnFitTitle("New Super Mario Bros. Wii", 56f, 460f, 80f, width, lineHeight)

        assertEquals(2, fit.lines.size)
        assertTrue(fit.fontSizeSp * 2 <= 80f)
    }

    @Test
    fun `result never exceeds two lines`() {
        val fit = jarnFitTitle("A Very Long Game Title With Many Words", 56f, 40f, 40f, width, lineHeight)

        assertTrue(fit.lines.size <= 2)
    }
}
