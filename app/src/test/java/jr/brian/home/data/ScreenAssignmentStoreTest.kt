package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import jr.brian.core.model.ScreenAssignment
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Guards the persistence contract of [ScreenAssignmentStore]. v34 of
 * [jr.brian.home.data.config.JarngreiprConfig] relies on the store
 * being authoritative across restarts, so a Backup → Restore writes
 * through [ScreenAssignmentStore.setAssignment] and must survive a
 * subsequent fresh instantiation.
 *
 * Mirrors the mockk/SharedPreferences pattern used by HomeButtonManagerTest.
 */
class ScreenAssignmentStoreTest {
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private val storage = mutableMapOf<String, Any?>()

    @Before
    fun setup() {
        storage.clear()

        context = mockk(relaxed = true)
        prefs = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } answers {
            storage[firstArg()] = secondArg<String?>()
            editor
        }
        every { editor.apply() } returns Unit
        every { editor.commit() } returns true
        every { prefs.getString(any(), any()) } answers {
            storage[firstArg()] as? String ?: secondArg<String?>()
        }
    }

    @Test
    fun `defaults to FRONTEND_ON_TOP when no value has ever been persisted`() {
        val store = ScreenAssignmentStore(context)

        assertEquals(ScreenAssignment.FRONTEND_ON_TOP, store.assignment)
        assertEquals(ScreenAssignment.FRONTEND_ON_TOP, ScreenAssignment.DEFAULT)
    }

    @Test
    fun `setAssignment updates state and persists the enum name`() {
        val store = ScreenAssignmentStore(context)

        store.setAssignment(ScreenAssignment.FRONTEND_ON_BOTTOM)

        assertEquals(ScreenAssignment.FRONTEND_ON_BOTTOM, store.assignment)
        assertEquals("FRONTEND_ON_BOTTOM", storage["assignment"])
    }

    @Test
    fun `flip inverts the current assignment and persists`() {
        val store = ScreenAssignmentStore(context)

        store.flip()

        assertEquals(ScreenAssignment.FRONTEND_ON_BOTTOM, store.assignment)
        assertEquals("FRONTEND_ON_BOTTOM", storage["assignment"])

        store.flip()

        assertEquals(ScreenAssignment.FRONTEND_ON_TOP, store.assignment)
        assertEquals("FRONTEND_ON_TOP", storage["assignment"])
    }

    @Test
    fun `assignment survives re-instantiation when prefs are preserved`() {
        storage["assignment"] = "FRONTEND_ON_BOTTOM"

        val store = ScreenAssignmentStore(context)

        assertEquals(ScreenAssignment.FRONTEND_ON_BOTTOM, store.assignment)
    }

    @Test
    fun `garbage persisted value falls back to the default`() {
        storage["assignment"] = "nonsense-left-by-a-bad-import"

        val store = ScreenAssignmentStore(context)

        assertEquals(ScreenAssignment.DEFAULT, store.assignment)
    }

    @Test
    fun `fromNameOrDefault returns DEFAULT for garbage`() {
        assertEquals(ScreenAssignment.DEFAULT, ScreenAssignment.fromNameOrDefault("garbage"))
        assertEquals(ScreenAssignment.DEFAULT, ScreenAssignment.fromNameOrDefault(null))
    }

    @Test
    fun `fromNameOrDefault round-trips a valid enum name`() {
        assertEquals(
            ScreenAssignment.FRONTEND_ON_BOTTOM,
            ScreenAssignment.fromNameOrDefault("FRONTEND_ON_BOTTOM"),
        )
        assertEquals(
            ScreenAssignment.FRONTEND_ON_TOP,
            ScreenAssignment.fromNameOrDefault("FRONTEND_ON_TOP"),
        )
    }
}
