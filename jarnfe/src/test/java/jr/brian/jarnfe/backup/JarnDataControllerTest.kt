package jr.brian.jarnfe.backup

import android.net.Uri
import jr.brian.jarnfe.R
import jr.brian.jarnfe.shell.JarnNotices
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JarnDataControllerTest {
    private val controller = JarnDataController(TestScope(UnconfinedTestDispatcher()))
    private val uri: Uri = Uri.parse("content://backup")

    private val preview = JarnBackupPreview(44, 44, null, emptyList(), 0, false)
    private val candidate = object : JarnBackupCandidate {
        override val preview = this@JarnDataControllerTest.preview
    }

    private class FakeBackup(
        val inspection: JarnBackupInspection,
        val restoreResult: CompletableDeferred<Boolean> = CompletableDeferred(true),
        val exportResult: JarnBackupExport = JarnBackupExport.Saved,
    ) : JarnBackup {
        var restores = 0
        override suspend fun export(target: Uri) = exportResult
        override suspend fun inspect(source: Uri) = inspection
        override suspend fun restore(candidate: JarnBackupCandidate): Boolean {
            restores += 1
            return restoreResult.await()
        }
    }

    private class FakeUpdates(
        val result: JarnUpdateCheck = JarnUpdateCheck.UpToDate,
        val installAllowed: Boolean = true,
        val downloads: Flow<JarnUpdateDownload> = flowOf(JarnUpdateDownload.Failed),
    ) : JarnUpdates {
        val skipped = mutableListOf<String>()
        override val currentVersion = "1.0"
        override val rcEnabled = false
        override fun setRcEnabled(enabled: Boolean) = Unit
        override suspend fun check() = result
        override fun skip(version: String) { skipped += version }
        override fun markDownloaded(version: String) = Unit
        override fun canInstall() = installAllowed
        override fun openInstallPermission() = true
        override fun download(variant: JarnUpdateVariant) = downloads
        override fun install(file: JarnDownloadedUpdate) = true
        override fun openReleasePage(offer: JarnUpdateOffer) = true
    }

    private val variant = JarnUpdateVariant("a.apk", "url", 1, isHidden = false, matchesInstall = true)
    private val offer = JarnUpdateOffer("2.0", "", false, "page", listOf(variant))

    @Test
    fun `inspecting shows the preview and restores nothing`() {
        val backup = FakeBackup(JarnBackupInspection.Ready(candidate))
        controller.inspect(backup, uri, null)
        assertTrue(controller.sheet is JarnDataSheet.RestorePreview)
        assertEquals(0, backup.restores)
    }

    @Test
    fun `cancelling the preview restores nothing`() {
        val backup = FakeBackup(JarnBackupInspection.Ready(candidate))
        controller.inspect(backup, uri, null)
        controller.dismiss()
        assertNull(controller.sheet)
        assertEquals(0, backup.restores)
    }

    @Test
    fun `a rejected file gets a clear message and restores nothing`() {
        val backup = FakeBackup(JarnBackupInspection.Failed(JarnBackupProblem.TooNew))
        controller.inspect(backup, uri, null)
        val sheet = controller.sheet as JarnDataSheet.Message
        assertEquals(R.string.jarnfe_restore_too_new, sheet.messageRes)
        controller.confirmRestore(backup)
        assertEquals(0, backup.restores)
    }

    @Test
    fun `a running restore can't be dismissed or started twice`() {
        val pending = CompletableDeferred<Boolean>()
        val backup = FakeBackup(JarnBackupInspection.Ready(candidate), pending)
        controller.inspect(backup, uri, null)
        controller.confirmRestore(backup)
        controller.dismiss()
        controller.confirmRestore(backup)
        assertTrue(controller.isBusy)
        assertEquals(1, backup.restores)
        pending.complete(true)
        assertNull(controller.sheet)
    }

    @Test
    fun `a failed restore says it may be partial`() {
        val backup = FakeBackup(JarnBackupInspection.Ready(candidate), CompletableDeferred(false))
        controller.inspect(backup, uri, null)
        controller.confirmRestore(backup)
        val sheet = controller.sheet as JarnDataSheet.Message
        assertEquals(R.string.jarnfe_restore_failed_message, sheet.messageRes)
    }

    @Test
    fun `a backup missing unreadable folders still saves and warns`() {
        val backup = FakeBackup(
            JarnBackupInspection.Ready(candidate),
            exportResult = JarnBackupExport.SavedWithoutSomeFolders,
        )
        controller.export(backup, uri, null)
        assertNull(controller.sheet)
        assertEquals(R.string.jarnfe_backup_saved_without_folders, JarnNotices.current?.messageRes)
    }

    @Test
    fun `an available update opens the offer and skip records it`() {
        val updates = FakeUpdates(JarnUpdateCheck.Available(offer))
        controller.checkForUpdate(updates, null)
        assertEquals(JarnDataSheet.UpdateOffered(offer), controller.sheet)
        controller.skip(updates, offer)
        assertEquals(listOf("2.0"), updates.skipped)
        assertNull(controller.sheet)
    }

    @Test
    fun `a download without install permission asks first`() {
        val updates = FakeUpdates(installAllowed = false)
        controller.download(updates, offer, variant)
        assertEquals(JarnDataSheet.UpdateNeedsPermission(offer, variant), controller.sheet)
    }

    @Test
    fun `a failed download offers a retry`() {
        controller.download(FakeUpdates(), offer, variant)
        assertEquals(JarnDataSheet.UpdateFailed(offer, variant), controller.sheet)
    }

    @Test
    fun `a download that ends without a result can't leave the sheet stuck`() {
        val progress = JarnUpdateDownload.Progress(50, 1, 2)
        controller.download(FakeUpdates(downloads = flowOf(progress)), offer, variant)
        assertEquals(JarnDataSheet.UpdateFailed(offer, variant), controller.sheet)
    }
}
