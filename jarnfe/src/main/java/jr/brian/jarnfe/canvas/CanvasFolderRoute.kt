package jr.brian.jarnfe.canvas

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
import jr.brian.jarnfe.apps.JarnAppOptionsExtra
import jr.brian.jarnfe.apps.JarnAppOptionsFlow
import jr.brian.jarnfe.apps.JarnAppOptionsTarget
import jr.brian.jarnfe.apps.rememberJarnImagePicker
import jr.brian.jarnfe.apps.shouldAskDisplay
import jr.brian.jarnfe.shell.JarnNotices
import kotlinx.coroutines.launch

private sealed interface FolderStep {
    data object Contents : FolderStep
    data object Rename : FolderStep
    data object Background : FolderStep
    data object EditApps : FolderStep
    data object ConfirmDelete : FolderStep
    data class AppOptions(val packageName: String) : FolderStep
    data class AskDisplay(val packageName: String) : FolderStep
    data class ConfirmLastApp(val packageName: String) : FolderStep
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
        if (data.appOptions(app.packageName).shouldAskDisplay()) {
            step = FolderStep.AskDisplay(app.packageName)
        } else {
            onClose()
            activity?.let { data.launchApp(it, app.packageName) }
        }
    }
    val chooseBackground = rememberJarnImagePicker { uri ->
        scope.launch {
            if (data.setFolderBackgroundImage(pageIndex, folder.id, uri)) {
                step = FolderStep.Contents
            } else {
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
                step = FolderStep.AppOptions(it.packageName)
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
        is FolderStep.AppOptions -> {
            val app = folder.apps.firstOrNull { it.packageName == current.packageName }
            if (app == null) {
                LaunchedEffect(current) { toContents() }
            } else {
                JarnAppOptionsFlow(
                    data = data,
                    target = JarnAppOptionsTarget(app.packageName, app.label, app.iconPath != null),
                    extras = listOf(
                        JarnAppOptionsExtra(R.string.jarnfe_folder_remove_app) {
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
                    ),
                    onClose = toContents,
                    onLeave = onClose,
                )
            }
        }
        is FolderStep.AskDisplay -> CanvasDisplayChoiceSheet(
            onPick = { display -> launchOn(current.packageName, display) },
            onClose = toContents,
        )
        is FolderStep.ConfirmLastApp -> CanvasFolderConfirmDialog(
            messageRes = R.string.jarnfe_folder_last_app_message,
            onConfirm = deleteFolder,
            onDismiss = { step = FolderStep.AppOptions(current.packageName) },
        )
    }
}
