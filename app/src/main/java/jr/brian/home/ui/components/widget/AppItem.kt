package jr.brian.home.ui.components.widget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.components.apps.AppIconImage
import jr.brian.home.ui.components.apps.NotificationBadge
import jr.brian.home.ui.components.dialog.AppOptionsDialog
import jr.brian.home.ui.components.dialog.CustomIconDialog
import jr.brian.home.ui.components.settings.AppName
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalCustomIconManager
import jr.brian.home.ui.theme.managers.LocalWidgetPageAppManager
import jr.brian.home.ui.util.rememberDialogState
import jr.brian.home.ui.util.rememberHasExternalDisplay
import jr.brian.home.util.launchApp
import jr.brian.home.util.launchAppOnOppositeDisplay
import jr.brian.home.util.openAppInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppItem(
    app: AppInfo,
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val widgetPageAppManager = LocalWidgetPageAppManager.current
    val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
    val appVisibilityManager = LocalAppVisibilityManager.current
    val customIconManager = LocalCustomIconManager.current
    val scope = rememberCoroutineScope()
    val optionsDialogState = rememberDialogState<Unit>()
    val customIconDialogState = rememberDialogState<Unit>()

    val hasExternalDisplay = rememberHasExternalDisplay()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    val displayPreference = if (hasExternalDisplay) {
                        appDisplayPreferenceManager.getAppDisplayPreference(app.packageName)
                    } else {
                        DisplayPreference.CURRENT_DISPLAY
                    }
                    launchApp(
                        context = context,
                        packageName = app.packageName,
                        displayPreference = displayPreference
                    )
                },
                onDoubleClick = {
                    launchAppOnOppositeDisplay(
                        context = context,
                        packageName = app.packageName,
                        currentPreference = appDisplayPreferenceManager.getAppDisplayPreference(app.packageName)
                    )
                },
                onLongClick = { optionsDialogState.show() }
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            AppIconImage(
                defaultIcon = app.icon,
                packageName = app.packageName,
                contentDescription = stringResource(R.string.app_icon_description, app.label),
                customIconManager = customIconManager,
                modifier = Modifier.size(48.dp)
            )
            
            NotificationBadge(
                packageName = app.packageName,
                offsetX = 4.dp,
                offsetY = (-4).dp
            )
        }

        Spacer(Modifier.height(4.dp))

        if (appVisibilityManager.showAppNames) {
            app.AppName()
        }
    }

    if (optionsDialogState.isVisible) {
        AppOptionsDialog(
            app = app,
            currentDisplayPreference = appDisplayPreferenceManager.getAppDisplayPreference(
                app.packageName
            ),
            onDismiss = optionsDialogState::dismiss,
            onAppInfoClick = {
                openAppInfo(context, app.packageName)
            },
            onDisplayPreferenceChange = { preference ->
                appDisplayPreferenceManager.setAppDisplayPreference(
                    app.packageName,
                    preference
                )
            },
            hasExternalDisplay = hasExternalDisplay,
            onHideApp = {
                scope.launch {
                    appVisibilityManager.hideApp(pageIndex, app.packageName)
                    widgetPageAppManager.removeVisibleApp(pageIndex, app.packageName)
                }
                optionsDialogState.dismiss()
            },
            onCustomIconClick = {
                customIconDialogState.show()
                optionsDialogState.dismiss()
            }
        )
    }

    if (customIconDialogState.isVisible) {
        CustomIconDialog(
            packageName = app.packageName,
            appLabel = app.label,
            onDismiss = customIconDialogState::dismiss,
            onIconChanged = {  }
        )
    }
}
