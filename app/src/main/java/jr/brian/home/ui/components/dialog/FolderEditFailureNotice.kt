package jr.brian.home.ui.components.dialog

import android.os.SystemClock
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import jr.brian.home.R
import jr.brian.home.ui.theme.managers.LocalFolderManager

private const val NOTICE_INTERVAL_MS = 3_500L

@Composable
fun FolderEditFailureNotice() {
    val context = LocalContext.current
    val folderManager = LocalFolderManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val message = stringResource(R.string.folder_edit_failed)

    LaunchedEffect(folderManager, lifecycleOwner, message) {
        var lastShownAt = -NOTICE_INTERVAL_MS
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            folderManager.editRefused.collect {
                val now = SystemClock.elapsedRealtime()
                if (now - lastShownAt >= NOTICE_INTERVAL_MS) {
                    lastShownAt = now
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
