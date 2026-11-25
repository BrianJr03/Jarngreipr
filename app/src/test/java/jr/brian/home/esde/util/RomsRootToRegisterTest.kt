package jr.brian.home.esde.util

import android.content.Context
import androidx.core.net.toUri
import jr.brian.home.esde.data.ESDEPreferencesManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** #459: a Jarn FE pick of one system's folder must not save its parent as a ROMs root. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RomsRootToRegisterTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test
    fun `system folder pick saves no parent root when asked not to`() {
        assertNull(romsRootToRegister("/storage/3A0D-5000/Ayn Thor/n3ds", "n3ds", registerParentRoot = false))
    }

    @Test
    fun `legacy pick still saves the parent root`() {
        assertEquals(
            "/storage/3A0D-5000/Ayn Thor",
            romsRootToRegister("/storage/3A0D-5000/Ayn Thor/n3ds", "n3ds", registerParentRoot = true),
        )
    }

    @Test
    fun `a picked library root is still saved`() {
        assertEquals(
            "/storage/3A0D-5000/Roms",
            romsRootToRegister("/storage/3A0D-5000/Roms", "n3ds", registerParentRoot = false),
        )
    }

    @Test
    fun `jarn pick of a system folder keeps the tree but adds no roms path`() {
        val esdePrefs = ESDEPreferencesManager(context)
        val treeUri = "content://com.android.externalstorage.documents/tree/3A0D-5000%3AAyn%20Thor%2Fn3ds".toUri()

        runCatching {
            persistSafTreeForSystem(context, esdePrefs, "n3ds", treeUri, registerParentRoot = false)
        }

        assertTrue(esdePrefs.state.value.romsPaths.none { it.endsWith("Ayn Thor") })
    }
}
