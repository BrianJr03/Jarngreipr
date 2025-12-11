package jr.brian.home.ui.components.widget

import android.content.Context
import android.hardware.display.DisplayManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.model.AppInfo
import jr.brian.home.ui.components.dialog.AppOptionsDialog
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalWidgetPageAppManager
import jr.brian.home.util.launchApp
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
    val scope = rememberCoroutineScope()
    var showOptionsDialog by remember { mutableStateOf(false) }

    val hasExternalDisplay = remember {
        val displayManager =
            context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.displays.size > 1
    }

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
                onLongClick = { showOptionsDialog = true }
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = app.icon),
            contentDescription = stringResource(R.string.app_icon_description, app.label),
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    }

    if (showOptionsDialog) {
        AppOptionsDialog(
            app = app,
            currentDisplayPreference = appDisplayPreferenceManager.getAppDisplayPreference(
                app.packageName
            ),
            onDismiss = { showOptionsDialog = false },
            onRemove = {
                scope.launch {
                    widgetPageAppManager.removeVisibleApp(pageIndex, app.packageName)
                }
                showOptionsDialog = false
            },
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
                showOptionsDialog = false
            }
        )
    }
}
