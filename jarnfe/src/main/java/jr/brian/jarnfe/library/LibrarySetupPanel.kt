package jr.brian.jarnfe.library

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnEmptyState
import jr.brian.frontend.library.setup.FolderSaveResult
import jr.brian.frontend.library.setup.LibrarySetup
import jr.brian.frontend.library.setup.LibrarySetupState
import jr.brian.jarnfe.R
import jr.brian.jarnfe.shell.JarnNotices
import kotlinx.coroutines.launch

/** An empty Library's next step. [folderName] fills the message when it names a folder. */
internal data class LibrarySetupPrompt(
    @param:StringRes val titleRes: Int,
    @param:StringRes val messageRes: Int,
    @param:StringRes val actionRes: Int,
    val folderName: String? = null,
    val pickerHint: String? = null,
)

/** Null when the normal Library, loading, or error content should show instead. */
internal fun librarySetupPrompt(
    setup: LibrarySetupState?,
    systems: JarnLibraryFeed<JarnLibrarySystem>,
): LibrarySetupPrompt? {
    if (systems.items.isNotEmpty() || systems.status != JarnLibraryStatus.Ready) return null
    return when (setup) {
        LibrarySetupState.NoSources -> LibrarySetupPrompt(
            R.string.jarnfe_setup_no_sources_title,
            R.string.jarnfe_setup_no_sources_message,
            R.string.jarnfe_setup_add_folder,
        )
        is LibrarySetupState.NeedsReconnect -> LibrarySetupPrompt(
            R.string.jarnfe_setup_reconnect_title,
            R.string.jarnfe_setup_reconnect_message,
            R.string.jarnfe_setup_reconnect,
            folderName = setup.nextName,
            pickerHint = setup.nextTreeUri.takeIf { it.startsWith("content://") },
        )
        LibrarySetupState.NoGamesFound -> LibrarySetupPrompt(
            R.string.jarnfe_setup_no_games_title,
            R.string.jarnfe_setup_no_games_message,
            R.string.jarnfe_setup_add_folder,
        )
        else -> null
    }
}

internal fun showFolderResult(result: FolderSaveResult) {
    when (result) {
        is FolderSaveResult.Found ->
            JarnNotices.showCount(R.plurals.jarnfe_setup_found_systems, result.systemCount)
        FolderSaveResult.NoSystemsFound -> JarnNotices.show(R.string.jarnfe_setup_no_systems)
        FolderSaveResult.AlreadyConnected -> JarnNotices.show(R.string.jarnfe_setup_already_connected)
        FolderSaveResult.Failed -> JarnNotices.show(R.string.jarnfe_setup_folder_failed)
    }
}

@Composable
internal fun LibrarySetupPanel(prompt: LibrarySetupPrompt) {
    val requester = remember { FocusRequester() }
    LaunchedEffect(prompt.actionRes) { requester.requestFocus() }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        JarnEmptyState(
            title = stringResource(prompt.titleRes),
            message = prompt.folderName?.let { stringResource(prompt.messageRes, it) }
                ?: stringResource(prompt.messageRes),
        )
        JarnButton(
            label = stringResource(prompt.actionRes),
            onClick = { JarnFolderPicker.open?.invoke(prompt.pickerHint?.toUri()) },
            modifier = Modifier.focusRequester(requester),
        )
    }
}

/** Set by the Stage activity, which owns the system folder picker. */
internal object JarnFolderPicker {
    var open by mutableStateOf<((Uri?) -> Unit)?>(null)
}

internal class JarnFolderController(
    private val activity: ComponentActivity,
    private val setup: LibrarySetup,
) {
    private val picker =
        activity.registerForActivityResult(ActivityResultContracts.OpenDocumentTree(), ::onPicked)

    fun open(hint: Uri?) {
        runCatching { picker.launch(hint) }
            .onFailure { JarnNotices.show(R.string.jarnfe_launch_folder_unavailable) }
    }

    private fun onPicked(uri: Uri?) {
        if (uri == null) return
        activity.lifecycleScope.launch {
            val result = setup.saveFolder(uri)
            showFolderResult(result)
            if (result is FolderSaveResult.Found) {
                activity.jarnArtDetectorOrNull()?.check(mediaChanged = true)
            }
        }
    }
}
