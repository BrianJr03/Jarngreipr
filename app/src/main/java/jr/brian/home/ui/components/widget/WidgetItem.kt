package jr.brian.home.ui.components.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import jr.brian.home.R
import jr.brian.home.model.WidgetInfo
import jr.brian.home.ui.components.dialog.WidgetOptionsDialog
import jr.brian.home.ui.theme.ColorTheme
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalThemeManager
import jr.brian.home.viewmodels.WidgetViewModel

@Composable
fun WidgetItem(
    widgetInfo: WidgetInfo,
    viewModel: WidgetViewModel,
    pageIndex: Int,
    modifier: Modifier = Modifier,
    onNavigateToResize: (WidgetInfo, Int) -> Unit = { _, _ -> },
    swapModeEnabled: Boolean = false,
    isSwapSource: Boolean = false,
    onSwapSelect: (Int) -> Unit = {},
    onEnableSwapMode: () -> Unit = {},
    editModeEnabled: Boolean = false
) {
    var showOptionsDialog by remember { mutableStateOf(false) }

    val currentWidgetId by rememberUpdatedState(widgetInfo.widgetId)
    val currentProviderInfo by rememberUpdatedState(widgetInfo.providerInfo)

    val widgetHeightDp = remember(widgetInfo.height) {
        val cellHeight = 80.dp
        val calculatedHeight = (widgetInfo.height * cellHeight.value).dp
        calculatedHeight.coerceAtLeast(80.dp)
    }

    val themeManager = LocalThemeManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (swapModeEnabled && !isSwapSource) {
                    OledCardColor.copy(alpha = 0.8f)
                } else {
                    OledCardColor
                },
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isSwapSource) 4.dp else if (editModeEnabled || swapModeEnabled) 2.dp else 0.dp,
                color = if (isSwapSource) Color.Yellow else if (editModeEnabled || swapModeEnabled) ThemePrimaryColor else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        key("${currentWidgetId}_${widgetInfo.width}x${widgetInfo.height}") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(widgetHeightDp)
                    .background(OledCardColor)
            ) {
                AndroidView(
                    factory = { ctx ->
                        val host = viewModel.getAppWidgetHost()
                        val widgetView = host?.createView(
                            ctx,
                            currentWidgetId,
                            currentProviderInfo
                        )
                        widgetView ?: ComposeView(ctx)
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        view.requestLayout()
                    }
                )
            }
        }

        if (editModeEnabled || swapModeEnabled) {
            Card(
                onClick = {
                    if (swapModeEnabled && !isSwapSource) {
                        onSwapSelect(widgetInfo.widgetId)
                    } else if (!swapModeEnabled) {
                        showOptionsDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                shape = RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomStart = 10.dp,
                    bottomEnd = 10.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSwapSource) Color.Yellow else ThemePrimaryColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,

                    ) {
                    if (swapModeEnabled) {
                        if (isSwapSource) {
                            Text(
                                text = "Selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black
                            )
                        } else {
                            Text(
                                text = "Tap to Swap",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.widget_edit_description),
                            tint = if (themeManager.currentTheme == ColorTheme.OLED_BLACK_WHITE) {
                                Color.Gray
                            } else {
                                Color.White
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showOptionsDialog && !swapModeEnabled && editModeEnabled) {
        WidgetOptionsDialog(
            widgetInfo = widgetInfo,
            currentPageIndex = pageIndex,
            onDismiss = { showOptionsDialog = false },
            onDelete = {
                viewModel.removeWidgetFromPage(widgetInfo.widgetId, pageIndex)
                showOptionsDialog = false
            },
            onMove = { targetPage ->
                viewModel.moveWidgetToPage(
                    widgetId = widgetInfo.widgetId,
                    fromPageIndex = pageIndex,
                    toPageIndex = targetPage
                )
                showOptionsDialog = false
            },
            onResize = {
                showOptionsDialog = false
                onNavigateToResize(widgetInfo, pageIndex)
            },
            onSwap = {
                showOptionsDialog = false
                onEnableSwapMode()
            }
        )
    }
}
