package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import jr.brian.core.model.Experience
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ExperienceStoreTest {
    private lateinit var context: Context
    private val storage = mutableMapOf<String, String?>()

    @Before
    fun setup() {
        storage.clear()
        context = mockk(relaxed = true)
        val prefs = mockk<SharedPreferences>(relaxed = true)
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { prefs.edit() } returns editor
        every { prefs.getString(any(), any()) } answers {
            storage[firstArg()] ?: secondArg()
        }
        every { editor.putString(any(), any()) } answers {
            storage[firstArg()] = secondArg()
            editor
        }
    }

    @Test
    fun `defaults to classic when nothing is stored`() {
        assertEquals(Experience.Classic, ExperienceStore(context).experience)
    }

    @Test
    fun `selection survives a new store instance`() {
        ExperienceStore(context).setExperience(Experience.JarnFE)

        assertEquals(Experience.JarnFE, ExperienceStore(context).experience)
    }

    @Test
    fun `unknown stored value reads as classic`() {
        storage["experience"] = "Future"

        assertEquals(Experience.Classic, ExperienceStore(context).experience)
    }
}
