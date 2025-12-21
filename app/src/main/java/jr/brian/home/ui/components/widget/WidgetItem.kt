package jr.brian.home.ui.components.widget

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import jr.brian.home.model.widget.WidgetInfo
import jr.brian.home.ui.components.dialog.EditWidgetOptionsDialog
import jr.brian.home.ui.theme.ThemePrimaryColor
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

    Column(
        modifier = modifier
            .fillMaxWidth()
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
            ) {
                // Always show the actual widget content
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

                // Show clickable overlay in edit/swap mode
                if (editModeEnabled || swapModeEnabled) {
                    Card(
                        onClick = {
                            if (swapModeEnabled && !isSwapSource) {
                                onSwapSelect(widgetInfo.widgetId)
                            } else if (!swapModeEnabled) {
                                showOptionsDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSwapSource) Color.Yellow.copy(alpha = 0.3f) else ThemePrimaryColor.copy(
                                alpha = 0.15f
                            )
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    if (showOptionsDialog && !swapModeEnabled && editModeEnabled) {
        EditWidgetOptionsDialog(
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
