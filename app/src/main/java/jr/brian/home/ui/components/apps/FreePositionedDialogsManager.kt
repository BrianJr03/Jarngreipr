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
import jr.brian.home.util.openAppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Manages and renders dialogs for free-positioned apps layout.
 */
@Composable
fun FreePositionedDialogsManager(
    showOptionsDialog: Boolean,
    showCustomIconDialog: Boolean,
    showFolderDialog: Boolean,
    selectedApp: AppInfo?,
    selectedFolder: Folder?,
    pageIndex: Int,
    allApps: List<AppInfo>,
    context: Context,
    hasExternalDisplay: Boolean,
    appPositionManager: AppPositionManager,
    appDisplayPreferenceManager: AppDisplayPreferenceManager,
    scope: CoroutineScope,
    onDismissOptionsDialog: () -> Unit,
    onDismissCustomIconDialog: () -> Unit,
    onDismissFolderDialog: () -> Unit,
    onHideApp: suspend (String) -> Unit,
    onShowCustomIconDialog: () -> Unit
) {
    if (showOptionsDialog && selectedApp != null) {
        val currentPosition = appPositionManager.getPosition(pageIndex, selectedApp.packageName)
        val currentIconSize = currentPosition?.iconSize ?: 64f

        AppOptionsDialog(
            app = selectedApp,
            currentDisplayPreference = appDisplayPreferenceManager.getAppDisplayPreference(
                selectedApp.packageName
            ),
            onDismiss = onDismissOptionsDialog,
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
                onDismissOptionsDialog()
            },
            onCustomIconClick = {
                onDismissOptionsDialog()
                onShowCustomIconDialog()
            }
        )
    }

    if (showCustomIconDialog && selectedApp != null) {
        CustomIconDialog(
            packageName = selectedApp.packageName,
            appLabel = selectedApp.label,
            onDismiss = onDismissCustomIconDialog
        )
    }

    if (showFolderDialog && selectedFolder != null) {
        val folderApps = allApps.filter { it.packageName in selectedFolder.appPackageNames }
        FolderContentsDialog(
            folderName = selectedFolder.name,
            apps = folderApps,
            folderId = selectedFolder.id,
            pageIndex = pageIndex,
            allApps = allApps,
            onDismiss = onDismissFolderDialog
        )
    }
}
