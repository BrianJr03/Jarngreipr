package jr.brian.home.ui.components.widget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import jr.brian.home.R
import jr.brian.home.model.AppInfo
import jr.brian.home.ui.components.dialog.AppOptionsDialog
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalWidgetPageAppManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppItem(
    app: AppInfo,
    pageIndex: Int,
    onLaunchApp: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val widgetPageAppManager = LocalWidgetPageAppManager.current
    val scope = rememberCoroutineScope()
    var showOptionsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = OledCardColor,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 2.dp,
                color = ThemePrimaryColor,
                shape = RoundedCornerShape(12.dp)
            )
            .combinedClickable(
                onClick = { onLaunchApp(app) },
                onLongClick = { showOptionsDialog = true }
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = app.icon),
            contentDescription = stringResource(R.string.app_icon_description, app.label),
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = app.label,
            color = Color.White,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            fontWeight = FontWeight.Medium
        )
    }

    if (showOptionsDialog) {
        AppOptionsDialog(
            app = app,
            onDismiss = { showOptionsDialog = false },
            onRemove = {
                scope.launch {
                    widgetPageAppManager.removeVisibleApp(pageIndex, app.packageName)
                }
                showOptionsDialog = false
            }
        )
    }
}
