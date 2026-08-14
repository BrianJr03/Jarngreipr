package jr.brian.home.esde.util

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import jr.brian.home.esde.data.ESDEPreferencesManager
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Blocker 1 in the Add Systems spec: `persistSafTreeForSystem` used to hard-
 * code a `primary:` check and silently drop SD-card grants. This test locks
 * the fix: a non-primary tree ID must still register its ROMs root.
 *
 * We can't verify the persistable-permission grant here (Robolectric's
 * ContentResolver rejects an unowned SAF URI), so we ignore the SecurityException
 * that comes out of takePersistableUriPermission and assert only the side
 * effect that regressed — addRomsPath being reached.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class PersistSafTreeForSystemTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test
    fun `sd-card tree URI registers a volume-scoped ROMs root`() {
        val esdePrefs = ESDEPreferencesManager(context)
        // A fake SD-card ExternalStorageProvider tree. The URL shape here
        // matches what Android hands back from OpenDocumentTree for a folder
        // named `Roms/ps2` on volume 1A2B-3C4D.
        val treeUri: Uri = "content://com.android.externalstorage.documents/tree/1A2B-3C4D%3ARoms".toUri()

        runCatching {
            persistSafTreeForSystem(context, esdePrefs, systemName = "ps2", treeUri = treeUri)
        }

        val roots = esdePrefs.state.value.romsPaths
        assertTrue(
            "Expected an SD-volume path to be registered; got $roots",
            roots.any { it.startsWith("/storage/1A2B-3C4D") }
        )
    }

    @Test
    fun `primary tree URI still registers an internal storage root`() {
        val esdePrefs = ESDEPreferencesManager(context)
        val treeUri: Uri = "content://com.android.externalstorage.documents/tree/primary%3ARoms".toUri()

        runCatching {
            persistSafTreeForSystem(context, esdePrefs, systemName = "ps2", treeUri = treeUri)
        }

        val roots = esdePrefs.state.value.romsPaths
        assertTrue(
            "Expected an internal-storage path to be registered; got $roots",
            roots.any { it.startsWith("/storage/emulated/0") }
        )
    }
}
