package jr.brian.jarnfe.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnHomeReadyRowTest {
    private var opened = 0

    private fun row(isHome: Boolean) = jarnHomeReadyRow(isHome, DONE, TODO, ACTION) { opened++ }

    @Test
    fun `not Home shows the button that opens the picker`() {
        val row = row(isHome = false)
        assertFalse(row.isDone)
        assertEquals(TODO, row.text)
        assertEquals(ACTION, row.actionLabel)
        row.onAction?.invoke()
        assertEquals(1, opened)
    }

    @Test
    fun `Home shows the green check with no button`() {
        val row = row(isHome = true)
        assertTrue(row.isDone)
        assertEquals(DONE, row.text)
        assertNull(row.onAction)
    }

    @Test
    fun `the row sits last under one stable key`() {
        assertEquals("home", row(isHome = false).key)
        assertEquals(row(isHome = false).key, row(isHome = true).key)
        assertEquals(50, row(isHome = true).order)
    }

    private companion object {
        const val DONE = "done"
        const val TODO = "todo"
        const val ACTION = "action"
    }
}
