package jr.brian.home.ui.components.apps

import android.content.Context
import androidx.compose.runtime.Composable
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.data.AppPositionManager
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import jr.brian.home.ui.components.dialog.AppOptionsDialog
import jr.brian.home.ui.components.dialog.CustomIconDialog
import jr.brian.home.ui.components.dialog.FolderContentsDialog
import jr.brian.home.ui.components.dialog.RenameAppDialog
import jr.brian.home.ui.util.DialogState
import jr.brian.home.util.openAppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Manages and renders dialogs for free-positioned apps layout.
 */
@Composable
fun FreePositionedDialogsManager(
    appOptionsDialogState: DialogState<AppInfo>,
    customIconDialogState: DialogState<AppInfo>,
    renameDialogState: DialogState<AppInfo>,
    folderDialogState: DialogState<Folder>,
    pageIndex: Int,
    allApps: List<AppInfo>,
    context: Context,
    hasExternalDisplay: Boolean,
    appPositionManager: AppPositionManager,
    appDisplayPreferenceManager: AppDisplayPreferenceManager,
    scope: CoroutineScope,
    onHideApp: suspend (String) -> Unit
) {
    appOptionsDialogState.item?.let { selectedApp ->
        if (appOptionsDialogState.isVisible) {
            val currentPosition = appPositionManager.getPosition(pageIndex, selectedApp.packageName)
        val currentIconSize = currentPosition?.iconSize ?: 64f

        AppOptionsDialog(
            app = selectedApp,
            currentDisplayPreference = appDisplayPreferenceManager.getAppDisplayPreference(
                selectedApp.packageName
            ),
            onDismiss = appOptionsDialogState::dismiss,
            onAppInfoClick = {
                openAppInfo(context, selectedApp.packageName)
            },
            onDisplayPreferenceChange = { preference ->
                appDisplayPreferenceManager.setAppDisplayPreference(
                    selectedApp.packageName,
                    preference
                )
            },
            hasExternalDisplay = hasExternalDisplay,
            currentIconSize = currentIconSize,
            onIconSizeChange = { newSize ->
                appPositionManager.savePosition(
                    pageIndex,
                    AppPosition(
                        packageName = selectedApp.packageName,
                        x = currentPosition?.x ?: 0f,
                        y = currentPosition?.y ?: 0f,
                        iconSize = newSize
                    )
                )
            },
            showResizeOption = true,
            onHideApp = {
                scope.launch {
                    onHideApp(selectedApp.packageName)
                }
                appOptionsDialogState.dismiss()
            },
            onCustomIconClick = {
                customIconDialogState.show(selectedApp)
                appOptionsDialogState.dismiss()
            },
            onRenameClick = {
                renameDialogState.show(selectedApp)
                appOptionsDialogState.dismiss()
            }
        )
        }
    }

    customIconDialogState.item?.let { appInfo ->
        if (customIconDialogState.isVisible) {
            CustomIconDialog(
                packageName = appInfo.packageName,
                appLabel = appInfo.label,
                onDismiss = customIconDialogState::dismiss
            )
        }
    }

    renameDialogState.item?.let { appInfo ->
        if (renameDialogState.isVisible) {
            RenameAppDialog(
                packageName = appInfo.packageName,
                appLabel = appInfo.label,
                onDismiss = renameDialogState::dismiss
            )
        }
    }

    folderDialogState.item?.let { folder ->
        if (folderDialogState.isVisible) {
            val folderApps = allApps.filter { it.packageName in folder.appPackageNames }
            FolderContentsDialog(
                folderName = folder.name,
                apps = folderApps,
                folderId = folder.id,
                pageIndex = pageIndex,
                allApps = allApps,
                onDismiss = folderDialogState::dismiss
            )
        }
    }
}
