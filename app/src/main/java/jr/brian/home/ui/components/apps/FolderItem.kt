package jr.brian.home.ui.components.apps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.data.CustomIconManager
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderItem(
    apps: List<AppInfo>,
    folderName: String = "Folder",
    keyboardVisible: Boolean,
    focusRequester: FocusRequester,
    offsetX: Float,
    offsetY: Float,
    onOffsetChanged: (Float, Float) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onFocusChanged: () -> Unit = {},
    isDraggingEnabled: Boolean = true,
    iconSize: Float = 64f,
    isFocusable: Boolean = false,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    customIconManager: CustomIconManager? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    var currentOffsetX by remember(offsetX) { mutableStateOf(offsetX) }
    var currentOffsetY by remember(offsetY) { mutableStateOf(offsetY) }
    val appVisibilityManager = LocalAppVisibilityManager.current

    val previewApps = apps.take(4)
    val iconPreviewSize = (iconSize * 0.4f).dp

    Box(
        modifier = Modifier
            .offset { IntOffset(currentOffsetX.roundToInt(), currentOffsetY.roundToInt()) }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(iconSize.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        color = OledCardColor.copy(alpha = 0.9f)
                    )
                    .border(
                        width = 2.dp,
                        color = if (isFocused) ThemePrimaryColor else ThemePrimaryColor.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .then(
                        if (isDraggingEnabled) {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { onDragStart() },
                                    onDragEnd = { onDragEnd() },
                                    onDragCancel = { onDragEnd() }
                                ) { change, dragAmount ->
                                    change.consume()
                                    currentOffsetX += dragAmount.x
                                    currentOffsetY += dragAmount.y
                                    onOffsetChanged(currentOffsetX, currentOffsetY)
                                }
                            }
                        } else {
                            Modifier
                        }
                    )
                    .then(
                        if (isFocusable) {
                            Modifier
                                .focusRequester(focusRequester)
                                .onFocusChanged {
                                    if (it.isFocused && !isFocused) {
                                        onFocusChanged()
                                    }
                                    isFocused = it.isFocused
                                }
                                .focusable()
                        } else {
                            Modifier
                        }
                    )
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                when (previewApps.size) {
                    0 -> {
                        Text(
                            text = "Empty",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    1 -> {
                        AppIconImage(
                            defaultIcon = previewApps[0].icon,
                            packageName = previewApps[0].packageName,
                            contentDescription = previewApps[0].label,
                            customIconManager = customIconManager,
                            modifier = Modifier
                                .size((iconSize * 0.6f).dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    2 -> {
                        Row {
                            previewApps.forEach { app ->
                                AppIconImage(
                                    defaultIcon = app.icon,
                                    packageName = app.packageName,
                                    contentDescription = app.label,
                                    customIconManager = customIconManager,
                                    modifier = Modifier
                                        .size(iconPreviewSize)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                            }
                        }
                    }
                    3 -> {
                        Column {
                            AppIconImage(
                                defaultIcon = previewApps[0].icon,
                                packageName = previewApps[0].packageName,
                                contentDescription = previewApps[0].label,
                                customIconManager = customIconManager,
                                modifier = Modifier
                                    .size(iconPreviewSize)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Row {
                                previewApps.drop(1).forEach { app ->
                                    AppIconImage(
                                        defaultIcon = app.icon,
                                        packageName = app.packageName,
                                        contentDescription = app.label,
                                        customIconManager = customIconManager,
                                        modifier = Modifier
                                            .size(iconPreviewSize)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        Column {
                            Row {
                                previewApps.take(2).forEach { app ->
                                    AppIconImage(
                                        defaultIcon = app.icon,
                                        packageName = app.packageName,
                                        contentDescription = app.label,
                                        customIconManager = customIconManager,
                                        modifier = Modifier
                                            .size(iconPreviewSize)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                }
                            }
                            Row {
                                previewApps.drop(2).take(2).forEach { app ->
                                    AppIconImage(
                                        defaultIcon = app.icon,
                                        packageName = app.packageName,
                                        contentDescription = app.label,
                                        customIconManager = customIconManager,
                                        modifier = Modifier
                                            .size(iconPreviewSize)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            if (appVisibilityManager.showFolderNames) {
                Text(
                    text = folderName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            if (!keyboardVisible && isFocusable) {
                Spacer(Modifier.height(12.dp))

                val dividerAlpha by animateFloatAsState(
                    targetValue = if (isFocused) 1f else 0f,
                    label = "dividerAlpha"
                )

                HorizontalDivider(
                    color = ThemePrimaryColor,
                    thickness = 4.dp,
                    modifier = Modifier.alpha(dividerAlpha)
                )
            }
        }
    }
}
