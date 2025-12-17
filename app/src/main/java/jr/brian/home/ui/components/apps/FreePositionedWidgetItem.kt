package jr.brian.home.ui.components.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import jr.brian.home.R
import jr.brian.home.model.WidgetInfo
import jr.brian.home.ui.components.dialog.EditWidgetOptionsDialog
import jr.brian.home.ui.components.widget.WidgetItem
import jr.brian.home.ui.theme.ColorTheme
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalThemeManager
import jr.brian.home.viewmodels.WidgetViewModel
import kotlin.math.roundToInt

@Composable
fun FreePositionedWidgetItem(
    widget: WidgetInfo,
    viewModel: WidgetViewModel,
    pageIndex: Int,
    offsetX: Float,
    offsetY: Float,
    onOffsetChanged: (Float, Float) -> Unit,
    isDraggingEnabled: Boolean = true,
    onNavigateToResize: (WidgetInfo, Int) -> Unit = { _, _ -> },
    editModeEnabled: Boolean = false
) {
    var currentOffsetX by remember(offsetX) { mutableStateOf(offsetX) }
    var currentOffsetY by remember(offsetY) { mutableStateOf(offsetY) }
    var isDragging by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val themeManager = LocalThemeManager.current

    val widgetWidthDp = remember(widget.width) {
        val cellWidth = 80.dp
        (widget.width * cellWidth.value).dp.coerceAtLeast(200.dp)
    }

    val widgetHeightDp = remember(widget.height) {
        val cellHeight = 80.dp
        (widget.height * cellHeight.value).dp.coerceAtLeast(80.dp)
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(currentOffsetX.roundToInt(), currentOffsetY.roundToInt()) }
            .width(widgetWidthDp)
            .height(widgetHeightDp)
            .then(
                if (isDraggingEnabled) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                            },
                            onDragEnd = {
                                isDragging = false
                            },
                            onDragCancel = {
                                isDragging = false
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentOffsetX += dragAmount.x
                                currentOffsetY += dragAmount.y
                                onOffsetChanged(currentOffsetX, currentOffsetY)
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        if (isDragging) {
            // Show placeholder while dragging
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = ThemePrimaryColor.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = ThemePrimaryColor,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Moving Widget",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // Show actual widget without built-in edit controls
            WidgetItem(
                widgetInfo = widget,
                viewModel = viewModel,
                pageIndex = pageIndex,
                onNavigateToResize = onNavigateToResize,
                editModeEnabled = false  // Always false, we'll add our own controls
            )

            // Overlay edit controls on top when edit mode is enabled
            if (editModeEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 2.dp,
                            color = ThemePrimaryColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .zIndex(1f)
                ) {
                    // Edit button at the bottom
                    Card(
                        onClick = {
                            showOptionsDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .align(Alignment.BottomCenter),
                        shape = RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = 0.dp,
                            bottomStart = 10.dp,
                            bottomEnd = 10.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = ThemePrimaryColor
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
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
    }

    // Show options dialog
    if (showOptionsDialog && editModeEnabled) {
        EditWidgetOptionsDialog(
            widgetInfo = widget,
            currentPageIndex = pageIndex,
            onDismiss = { showOptionsDialog = false },
            onDelete = {
                viewModel.removeWidgetFromPage(widget.widgetId, pageIndex)
                showOptionsDialog = false
            },
            onMove = { targetPage ->
                viewModel.moveWidgetToPage(
                    widgetId = widget.widgetId,
                    fromPageIndex = pageIndex,
                    toPageIndex = targetPage
                )
                showOptionsDialog = false
            },
            onResize = {
                showOptionsDialog = false
                onNavigateToResize(widget, pageIndex)
            },
            onSwap = {
                showOptionsDialog = false
                // Swap mode not supported in free positioning
            }
        )
    }
}
