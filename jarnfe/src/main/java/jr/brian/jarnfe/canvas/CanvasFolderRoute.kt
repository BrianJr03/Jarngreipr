package jr.brian.jarnfe.canvas

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.jarnfe.R
import jr.brian.jarnfe.shell.JarnNotices
import kotlinx.coroutines.launch

private val FolderImageTypes = arrayOf("image/*")

private sealed interface FolderStep {
    data object Contents : FolderStep
    data object Rename : FolderStep
    data object Background : FolderStep
    data object EditApps : FolderStep
    data object ConfirmDelete : FolderStep
    data class AppMenu(val packageName: String) : FolderStep
    data class RenameApp(val packageName: String) : FolderStep
    data class AskDisplay(val packageName: String) : FolderStep
    data class ConfirmLastApp(val packageName: String) : FolderStep
}

/** Opens the system image picker. Cancelling changes nothing. */
@Composable
internal fun rememberFolderImagePicker(onPicked: (String) -> Unit): () -> Unit {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onPicked(it.toString()) }
    }
    return {
        try {
            picker.launch(FolderImageTypes)
        } catch (_: ActivityNotFoundException) {
            JarnNotices.show(R.string.jarnfe_canvas_notice_picker_failed)
        }
    }
}

@Composable
internal fun CanvasFolderRoute(
    data: JarnCanvasData,
    pageIndex: Int,
    folder: JarnCanvasFolder,
    onClose: () -> Unit,
) {
    var step by remember(folder.id) { mutableStateOf<FolderStep>(FolderStep.Contents) }
    var lastApp by remember(folder.id) { mutableStateOf<String?>(null) }
    var iconTarget by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val toContents: () -> Unit = { step = FolderStep.Contents }
    val deleteFolder: () -> Unit = {
        scope.launch {
            data.deleteFolder(pageIndex, folder.id)
            onClose()
        }
    }
    val launchOn: (String, JarnAppDisplay) -> Unit = { packageName, display ->
        onClose()
        activity?.let { data.launchAppOn(it, packageName, display) }
    }
    val openApp: (JarnCanvasApp) -> Unit = { app ->
        lastApp = app.packageName
        val options = data.folderAppOptions(app.packageName)
        if (options.askOnLaunch && options.hasSecondDisplay) {
            step = FolderStep.AskDisplay(app.packageName)
        } else {
            onClose()
            activity?.let { data.launchApp(it, app.packageName) }
        }
    }
    val chooseBackground = rememberFolderImagePicker { uri ->
        scope.launch {
            if (data.setFolderBackgroundImage(pageIndex, folder.id, uri)) {
                step = FolderStep.Contents
            } else {
                JarnNotices.show(R.string.jarnfe_canvas_notice_photo_failed)
            }
        }
    }
    val chooseIcon = rememberFolderImagePicker { uri ->
        val packageName = iconTarget ?: return@rememberFolderImagePicker
        scope.launch {
            if (!data.setAppIcon(packageName, uri)) {
                JarnNotices.show(R.string.jarnfe_canvas_notice_photo_failed)
            }
        }
    }

    when (val current = step) {
        FolderStep.Contents -> CanvasFolderContentsSheet(
            folder = folder,
            restoreFocusTo = lastApp,
            onOpenApp = openApp,
            onAppOptions = {
                lastApp = it.packageName
                step = FolderStep.AppMenu(it.packageName)
            },
            onRename = { step = FolderStep.Rename },
            onBackground = { step = FolderStep.Background },
            onEditApps = { step = FolderStep.EditApps },
            onDelete = { step = FolderStep.ConfirmDelete },
            onClose = onClose,
        )
        FolderStep.Rename -> CanvasKeyboardSheet(
            label = stringResource(R.string.jarnfe_folder_name_label),
            onDone = { text ->
                jarnFolderNameOrNull(text)?.let { name ->
                    scope.launch { data.renameFolder(pageIndex, folder.id, name) }
                }
                toContents()
            },
            onCancel = toContents,
            initialText = folder.name,
        )
        FolderStep.Background -> CanvasFolderBackgroundSheet(
            folder = folder,
            onColor = { argb -> scope.launch { data.setFolderBackgroundColor(pageIndex, folder.id, argb) } },
            onChooseImage = chooseBackground,
            onClose = toContents,
        )
        FolderStep.EditApps -> CanvasFolderEditApps(
            data = data,
            folder = folder,
            onSave = { added, removed ->
                scope.launch { data.editFolderApps(pageIndex, folder.id, added, removed) }
                toContents()
            },
            onDeleteFolder = deleteFolder,
            onClose = toContents,
        )
        FolderStep.ConfirmDelete -> CanvasFolderConfirmDialog(
            messageRes = R.string.jarnfe_folder_delete_message,
            onConfirm = deleteFolder,
            onDismiss = toContents,
        )
        is FolderStep.AppMenu -> {
            val app = folder.apps.firstOrNull { it.packageName == current.packageName }
            if (app == null) {
                LaunchedEffect(current) { toContents() }
            } else {
                CanvasFolderAppMenu(
                    data = data,
                    app = app,
                    onOpenOther = { display -> launchOn(app.packageName, display.opposite()) },
                    onAppInfo = {
                        onClose()
                        activity?.let { data.openAppInfo(it, app.packageName) }
                    },
                    onChooseIcon = {
                        iconTarget = app.packageName
                        chooseIcon()
                    },
                    onClearIcon = { scope.launch { data.clearAppIcon(app.packageName) } },
                    onRename = { step = FolderStep.RenameApp(app.packageName) },
                    onRemove = {
                        if (folder.apps.size <= 1) {
                            step = FolderStep.ConfirmLastApp(app.packageName)
                        } else {
                            scope.launch {
                                data.editFolderApps(pageIndex, folder.id, emptySet(), setOf(app.packageName))
                            }
                            lastApp = null
                            toContents()
                        }
                    },
                    onClose = toContents,
                )
            }
        }
        is FolderStep.RenameApp -> CanvasKeyboardSheet(
            label = stringResource(R.string.jarnfe_folder_app_name_label),
            onDone = { text ->
                jarnFolderNameOrNull(text)?.let { data.setAppName(current.packageName, it) }
                step = FolderStep.AppMenu(current.packageName)
            },
            onCancel = { step = FolderStep.AppMenu(current.packageName) },
            initialText = data.folderAppOptions(current.packageName).customName
                ?: folder.apps.firstOrNull { it.packageName == current.packageName }?.label.orEmpty(),
        )
        is FolderStep.AskDisplay -> CanvasDisplayChoiceSheet(
            onPick = { display -> launchOn(current.packageName, display) },
            onClose = toContents,
        )
        is FolderStep.ConfirmLastApp -> CanvasFolderConfirmDialog(
            messageRes = R.string.jarnfe_folder_last_app_message,
            onConfirm = deleteFolder,
            onDismiss = { step = FolderStep.AppMenu(current.packageName) },
        )
    }
}
